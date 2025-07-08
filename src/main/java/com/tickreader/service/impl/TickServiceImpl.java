package com.tickreader.service.impl;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.implementation.Configs;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.FeedResponse;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickreader.config.CosmosDbAccount;
import com.tickreader.config.CosmosDbAccountConfiguration;
import com.tickreader.config.RicBasedCosmosClientFactory;
import com.tickreader.dto.TickResponse;
import com.tickreader.entity.BaseTick;
import com.tickreader.entity.Tick;
import com.tickreader.entity.TickWithNoNulls;
import com.tickreader.service.TicksService;
import com.tickreader.service.utils.TickServiceUtils;
import org.apache.spark.unsafe.hash.Murmur3_x86_32;
import org.apache.spark.unsafe.types.UTF8String;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "ticks.implementation", havingValue = "completeablefuture")
public class TickServiceImpl implements TicksService {

    private final static Logger logger = LoggerFactory.getLogger(TickServiceImpl.class);

    private final RicBasedCosmosClientFactory clientFactory;
    private final CosmosDbAccountConfiguration cosmosDbAccountConfiguration;
    private final ExecutorService queryExecutorService;
    private final int concurrency = Configs.getCPUCnt() * 10;
    private final ObjectMapper nonNullObjectMapper = new ObjectMapper();

    public TickServiceImpl(RicBasedCosmosClientFactory clientFactory,
                           CosmosDbAccountConfiguration cosmosDbAccountConfiguration) {
        this.clientFactory = clientFactory;
        this.cosmosDbAccountConfiguration = cosmosDbAccountConfiguration;
        this.queryExecutorService = Executors.newFixedThreadPool(concurrency);

        nonNullObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public TickResponse getTicks(
            List<String> rics,
            List<String> docTypes,
            int totalTicks,
            boolean pinStart,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean includeNullValues,
            int pageSize,
            boolean includeDiagnostics) {
        
        try {
            return getTicksAsync(rics, docTypes, totalTicks, pinStart, startTime, endTime, includeNullValues, pageSize, includeDiagnostics).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error executing getTicks: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get ticks", e);
        }
    }

    // Dates: 2024-10-01T00:00:00 to 2024-10-05T23:59:59
    // RIC: AAPL
    // PkFormat: <RIC>|yyyy-MM-dd|<shardId>
    // Example: [AAPL|2024-10-01|1, .., AAPL|2024-10-01|8], [AAPL|2024-10-02|1, .., AAPL|2024-10-02|8], ... , [AAPL|2024-10-05|1, .., AAPL|2024-10-05|8]
    // Data Model
    //  - RIC <-> CosmosDbAccount <-> Database <-> Container (granularity: day)
    // Query -> across 5 containers (one for each day)
    // QueryString -> SELECT * FROM C WHERE C.docType IN " + docTypePlaceholders +
    //                    " AND C.messageTimestamp >= @startTime AND C.messageTimestamp < @endTime ORDER BY C.pk DESC, C.messageTimestamp DESC"
    // Cosmos Db perspective:
    // Task <-> TickRequestContext <-> CosmosContainer.queryItems(SELECT * FROM C WHERE C.docType IN " + docTypePlaceholders +
    //    //                    " AND C.messageTimestamp >= @startTime AND C.messageTimestamp < @endTime ORDER BY C.pk DESC, C.messageTimestamp DESC", queryRequestOptions (set the partition key), Tick.class) // AAPL|2024-10-01|1 (container for 2024-10-01)
    // CosmosContainer.queryItems(SELECT * FROM C WHERE C.docType IN " + docTypePlaceholders +
    //    //                    " AND C.messageTimestamp >= @startTime AND C.messageTimestamp < @endTime ORDER BY C.pk DESC, C.messageTimestamp DESC", queryRequestOptions (set the partition key), Tick.class) // AAPL|2024-10-01|2
    private CompletableFuture<TickResponse> getTicksAsync(
            List<String> rics,
            List<String> docTypes,
            int totalTicks,
            boolean pinStart,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean includeNullValues,
            int pageSize,
            boolean includeDiagnostics) {

        return CompletableFuture.supplyAsync(() -> {
            LocalDateTime newStartTime = startTime.isAfter(endTime) ? endTime : startTime;
            LocalDateTime newEndTime = endTime.isBefore(startTime) ? startTime : endTime;

            Map<String, RicQueryExecutionState> ricToRicQueryExecutionState = buildTickRequestContexts(rics, newStartTime, newEndTime, pinStart);
            String correlationId = UUID.randomUUID().toString();

            return executeQueryWithTopNSorted(
                    rics,
                    ricToRicQueryExecutionState,
                    docTypes,
                    newStartTime,
                    newEndTime,
                    totalTicks,
                    correlationId,
                    includeNullValues,
                    pinStart,
                    pageSize,
                    includeDiagnostics);
        }, queryExecutorService);
    }

    private Map<String, RicQueryExecutionState> buildTickRequestContexts(
            List<String> rics, 
            LocalDateTime startTime, 
            LocalDateTime endTime,
            boolean pinStart) {

        Map<String, RicQueryExecutionState> ricToRicQueryExecutionState = new HashMap<>();

        for (String ric : rics) {

            List<TickRequestContextPerPartitionKey> tickRequestContexts = new ArrayList<>();

            int seed = 42;
            UTF8String s = UTF8String.fromString(ric);
            int hash = Murmur3_x86_32.hashUnsafeBytes(s.getBaseObject(), s.getBaseOffset(), s.numBytes(), seed);
            int hashIdForRic = Math.abs(hash) % this.cosmosDbAccountConfiguration.getAccountCount() + 1;

            CosmosDbAccount cosmosDbAccount = this.cosmosDbAccountConfiguration.getCosmosDbAccount(hashIdForRic);
            String dateFormat = cosmosDbAccount.getContainerNameFormat();

            List<String> datesInBetween = TickServiceUtils.getLocalDatesBetweenTwoLocalDateTimes(startTime, endTime, dateFormat, pinStart);

            for (String date : datesInBetween) {
                CosmosAsyncClient asyncClient = this.clientFactory.getCosmosAsyncClient(hashIdForRic);

                if (asyncClient == null) {
                    logger.warn("CosmosAsyncClient instance not found for ric: {}", ric);
                    continue;
                }

                String databaseName = cosmosDbAccount.getDatabaseName();
                if (databaseName == null || databaseName.isEmpty()) {
                    logger.warn("Ric {} is not assigned to a database", ric);
                    continue;
                }

                CosmosAsyncContainer asyncContainer = asyncClient.getDatabase(databaseName)
                        .getContainer(cosmosDbAccount.getContainerNamePrefix() + date + cosmosDbAccount.getContainerNameSuffix());

                String tickIdentifier = TickServiceUtils.constructTickIdentifierPrefix(ric, date);

                TickRequestContextPerPartitionKey tickRequestContext = new TickRequestContextPerPartitionKey(
                        asyncContainer,
                        tickIdentifier,
                        date,
                        dateFormat);

                tickRequestContexts.add(tickRequestContext);
            }

            if (!tickRequestContexts.isEmpty()) {
                ricToRicQueryExecutionState.put(ric, new RicQueryExecutionState(tickRequestContexts));
            } else {
                logger.warn("No tick request contexts found for ric: {}", ric);
            }
        }

        return ricToRicQueryExecutionState;
    }

    private TickResponse executeQueryWithTopNSorted(
            List<String> rics,
            Map<String, RicQueryExecutionState> ricToRicQueryExecutionState,
            List<String> docTypes,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int totalTicks,
            String correlationId,
            boolean includeNullValues,
            boolean pinStart,
            int pageSize,
            boolean includeDiagnostics) {

        Instant executionStartTime = Instant.now();
        logger.info("Execution of query with correlationId : {} started at : {}", correlationId, executionStartTime);

        List<String> cosmosDiagnosticsContextList = Collections.synchronizedList(new ArrayList<>());

        List<Tick> resultTicks = new ArrayList<>();

        while (!ricToRicQueryExecutionState.values().stream().allMatch(RicQueryExecutionState::isCompleted)) {
            try {
                // Create tasks for each FeedRange context
                List<CompletableFuture<Void>> tasks = ricToRicQueryExecutionState.values().stream()
                        .map(ricQueryExecutionState -> CompletableFuture.runAsync(() ->
                                        fetchNextPage(ricQueryExecutionState, docTypes, startTime, endTime, pageSize, pinStart, totalTicks),
                                queryExecutorService))
                        .collect(Collectors.toList());

                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).get();
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Error during query execution: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to execute queries", e);
            }
        }

        Instant executionEndTime = Instant.now();
        logger.info("Execution of query with correlationId : {} finished in duration : {}", correlationId, Duration.between(executionStartTime, executionEndTime));

        for (String ric : rics) {
            RicQueryExecutionState ricQueryExecutionState = ricToRicQueryExecutionState.get(ric);
            if (ricQueryExecutionState == null) {
                logger.warn("No RicQueryExecutionState found for ric: {}", ric);
                continue;
            }

            List<Tick> ticks = ricQueryExecutionState.getTicks();
            if (ticks.isEmpty()) {
                logger.warn("No ticks found for ric: {}", ric);
                continue;
            }

            // Add cosmos diagnostics context

            for (TickRequestContextPerPartitionKey tickRequestContextPerPartitionKey : ricQueryExecutionState.getTickRequestContexts()) {
                if (tickRequestContextPerPartitionKey.getCosmosDiagnosticsList() != null) {
                    cosmosDiagnosticsContextList.addAll(tickRequestContextPerPartitionKey.getCosmosDiagnosticsList().stream().map(cosmosDiagnosticsContext -> cosmosDiagnosticsContext.getDiagnosticsContext().toJson()).collect(Collectors.toList()));
                }
            }

            Collections.sort(ticks, (t1, t2) -> Math.toIntExact(t2.getMessageTimestamp() - t1.getMessageTimestamp()));
            resultTicks.addAll(ticks);
        }

        List<BaseTick> finalTicks = new ArrayList<>();

        if (includeNullValues) {
            finalTicks.addAll(resultTicks);
        } else {
            List<TickWithNoNulls> newTicks = resultTicks.stream()
                    .map(tick -> nonNullObjectMapper.convertValue(tick, TickWithNoNulls.class))
                    .collect(Collectors.toList());

            finalTicks.addAll(newTicks);
        }

        return new TickResponse(
                finalTicks,
                includeDiagnostics ? cosmosDiagnosticsContextList : Collections.emptyList(),
                Duration.between(executionStartTime, executionEndTime));
    }

    private void fetchNextPage(
            RicQueryExecutionState ricQueryExecutionState,
            List<String> docTypes,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int pageSize,
            boolean pinStart,
            int totalTicks) {

        TickRequestContextPerPartitionKey tickRequestContext
                = TickServiceUtils.evaluateTickRequestContextToExecute(ricQueryExecutionState);

        if (tickRequestContext == null) {
            ricQueryExecutionState.setCompleted(true);
            return;
        }

        CosmosAsyncContainer asyncContainer = tickRequestContext.getAsyncContainer();
        CosmosQueryRequestOptions queryRequestOptions = new CosmosQueryRequestOptions();

        SqlQuerySpec querySpec = tickRequestContext.getSqlQuerySpec() != null ? tickRequestContext.getSqlQuerySpec() : getSqlQuerySpec(
                tickRequestContext.getTickIdentifier(),
                docTypes,
                startTime,
                endTime,
                tickRequestContext.getRequestDateAsString(),
                tickRequestContext.getDateFormat(),
                pinStart,
                totalTicks);

        tickRequestContext.setSqlQuerySpec(querySpec);

        String continuationToken = tickRequestContext.getContinuationToken();

        if (continuationToken != null && continuationToken.equals("drained")) {
            return;
        }

        try {
            FeedResponse<Tick> response = asyncContainer.queryItems(querySpec, queryRequestOptions, Tick.class)
                    .byPage(continuationToken, pageSize)
                    .next()
                    .onErrorResume(throwable -> {

                        if (throwable instanceof CosmosException) {
                            CosmosException cosmosException = (CosmosException) throwable;
                            logger.error("Cosmos exception during page fetch: {}", cosmosException.getMessage(), cosmosException);

                            if (TickServiceUtils.isResourceNotFound(cosmosException)) {
                                logger.warn("Cosmos exception during page fetch: {}", cosmosException.getMessage());
                                tickRequestContext.setContinuationToken("drained");
                                logger.warn("No tick data found for date: {}", tickRequestContext.getRequestDateAsString());

                                return Mono.empty();
                            }
                        }

                        logger.error("Error during page fetch: {}", throwable.getMessage(), throwable);
                        return Mono.error(throwable);
                    })
                    .block();

            if (response != null) {
                tickRequestContext.setContinuationToken(response.getContinuationToken() != null ? response.getContinuationToken() : "drained");
                tickRequestContext.addCosmosDiagnostics(response.getCosmosDiagnostics());

                ricQueryExecutionState.addTicks(response.getResults(), totalTicks);
            }
        } catch (CosmosException e) {
            logger.error("Cosmos exception during page fetch: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Error during page fetch: {}", e.getMessage(), e);
        }
    }

    private SqlQuerySpec getSqlQuerySpec(
            String tickIdentifier,
            List<String> docTypes,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String localDateAsString,
            String format,
            boolean pinStart,
            int totalTicks) {

        LocalDate localDate = LocalDate.parse(localDateAsString, DateTimeFormatter.ofPattern(format));

        long queryStartTime = !startTime.isBefore(localDate.atStartOfDay()) ? startTime.toInstant(ZoneOffset.UTC).toEpochMilli() * 1_000_000L : localDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() * 1_000_000L;
        long queryEndTime = !endTime.isAfter(localDate.atTime(23, 59, 59, 999_999_999)) ? endTime.toInstant(ZoneOffset.UTC).toEpochMilli() * 1_000_000L : localDate.atTime(23, 59, 59, 999_999_999).toInstant(ZoneOffset.UTC).toEpochMilli() * 1_000_000L;

        List<SqlParameter> parameters = new ArrayList<>();

        parameters.add(new SqlParameter("@startTime", queryStartTime));
        parameters.add(new SqlParameter("@endTime", queryEndTime));

        StringBuilder docTypePlaceholders = new StringBuilder();

        docTypePlaceholders.append("(");

        for (int i = 0; i < docTypes.size(); i++) {

            String param = "@docType" + i;

            parameters.add(new SqlParameter(param, docTypes.get(i)));

            docTypePlaceholders.append(param);

            if (i < docTypes.size() - 1) {
                docTypePlaceholders.append(", ");
            }
        }

        docTypePlaceholders.append(")");

        StringBuilder partitionKeyPlaceholders = new StringBuilder();
        partitionKeyPlaceholders.append("(");

        for (int i = 1; i <= this.cosmosDbAccountConfiguration.getShardCountPerRic(); i++) {
            String param = "@pk" + i;
            parameters.add(new SqlParameter(param, tickIdentifier + "|" + i));
            partitionKeyPlaceholders.append(param);
            if (i <= this.cosmosDbAccountConfiguration.getShardCountPerRic() - 1) {
                partitionKeyPlaceholders.append(", ");
            }
        }

        partitionKeyPlaceholders.append(")");

        if (pinStart) {
            String query = "SELECT * FROM C WHERE C.pk IN " + partitionKeyPlaceholders + " AND C.docType IN " + docTypePlaceholders +
                    " AND C.messageTimestamp >= @startTime AND C.messageTimestamp < @endTime ORDER BY C.pk ASC, C.messageTimestamp ASC OFFSET 0 LIMIT " + totalTicks;

            return new SqlQuerySpec(query, parameters);
        } else {
            String query = "SELECT * FROM C WHERE C.pk IN " + partitionKeyPlaceholders + " AND C.docType IN " + docTypePlaceholders +
                    " AND C.messageTimestamp >= @startTime AND C.messageTimestamp < @endTime ORDER BY C.pk ASC, C.messageTimestamp DESC OFFSET 0 LIMIT " + totalTicks;

            return new SqlQuerySpec(query, parameters);
        }
    }
}