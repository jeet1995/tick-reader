package com.tickreader.service.impl;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosDiagnostics;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.implementation.Configs;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.FeedResponse;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import com.tickreader.config.CosmosDbAccount;
import com.tickreader.config.CosmosDbAccountConfiguration;
import com.tickreader.config.RicBasedCosmosClientFactory;
import com.tickreader.dto.TickResponse;
import com.tickreader.entity.Tick;
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
public class FeedRangeBackupTicksServiceImpl implements TicksService {

    private final static Logger logger = LoggerFactory.getLogger(FeedRangeBackupTicksServiceImpl.class);

    private final RicBasedCosmosClientFactory clientFactory;
    private final CosmosDbAccountConfiguration cosmosDbAccountConfiguration;
    private final ExecutorService queryExecutorService;
    private final ExecutorService priorityQueueExecutorService;
    private final int pageSize = 800;
    private final int concurrency = Configs.getCPUCnt() * 10;

    public FeedRangeBackupTicksServiceImpl(RicBasedCosmosClientFactory clientFactory,
                                         CosmosDbAccountConfiguration cosmosDbAccountConfiguration) {
        this.clientFactory = clientFactory;
        this.cosmosDbAccountConfiguration = cosmosDbAccountConfiguration;
        this.queryExecutorService = Executors.newFixedThreadPool(concurrency);
        this.priorityQueueExecutorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public TickResponse getTicks(
            List<String> rics,
            List<String> docTypes,
            int totalTicks,
            boolean pinStart,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        
        try {
            return getTicksAsync(rics, docTypes, totalTicks, pinStart, startTime, endTime).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error executing getTicks: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get ticks", e);
        }
    }

    private CompletableFuture<TickResponse> getTicksAsync(
            List<String> rics,
            List<String> docTypes,
            int totalTicks,
            boolean pinStart,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        return CompletableFuture.supplyAsync(() -> {
            LocalDateTime newStartTime = startTime.isAfter(endTime) ? endTime : startTime;
            LocalDateTime newEndTime = endTime.isBefore(startTime) ? startTime : endTime;

            List<TickRequestContextPerPartitionKey> tickRequestContexts = buildTickRequestContexts(rics, newStartTime, newEndTime);
            String correlationId = UUID.randomUUID().toString();

            return executeQueryWithTopNSorted(
                    tickRequestContexts,
                    docTypes,
                    newStartTime,
                    newEndTime,
                    pinStart,
                    totalTicks,
                    correlationId);
        }, queryExecutorService);
    }

    private List<TickRequestContextPerPartitionKey> buildTickRequestContexts(
            List<String> rics, 
            LocalDateTime startTime, 
            LocalDateTime endTime) {
        
        List<TickRequestContextPerPartitionKey> tickRequestContexts = new ArrayList<>();
        int shardCount = this.cosmosDbAccountConfiguration.getShardCountPerRic();

        for (String ric : rics) {
            int seed = 42;
            UTF8String s = UTF8String.fromString(ric);
            int hash = Murmur3_x86_32.hashUnsafeBytes(s.getBaseObject(), s.getBaseOffset(), s.numBytes(), seed);
            int hashIdForRic = Math.abs(hash) % this.cosmosDbAccountConfiguration.getAccountCount() + 1;

            CosmosDbAccount cosmosDbAccount = this.cosmosDbAccountConfiguration.getCosmosDbAccount(hashIdForRic);
            String dateFormat = cosmosDbAccount.getContainerNameFormat();

            List<String> datesInBetween = TickServiceUtils.getLocalDatesBetweenTwoLocalDateTimes(startTime, endTime, dateFormat);

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

                for (int i = 1; i <= shardCount; i++) {
                    String partitionKey = tickIdentifier + "|" + i;

                    TickRequestContextPerPartitionKey tickRequestContext = new TickRequestContextPerPartitionKey(
                            asyncContainer,
                            partitionKey,
                            date,
                            dateFormat);

                    tickRequestContexts.add(tickRequestContext);
                }
            }
        }

        return tickRequestContexts;
    }

    private TickResponse executeQueryWithTopNSorted(
            List<TickRequestContextPerPartitionKey> tickRequestContexts,
            List<String> docTypes,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean pinStart,
            int totalTicks,
            String correlationId) {

        Instant executionStartTime = Instant.now();
        logger.info("Execution of query with correlationId : {} started at : {}", correlationId, executionStartTime);

        List<CosmosDiagnostics> cosmosDiagnosticsList = Collections.synchronizedList(new ArrayList<>());

        List<Tick> resultTicks = new ArrayList<>();
        ConcurrentHashMap<String, FeedResponse<Tick>> feedResponseCache = new ConcurrentHashMap<>();

        while (resultTicks.size() < totalTicks && !tickRequestContexts.stream().allMatch(tickRequestContext -> tickRequestContext.getContinuationToken() != null && tickRequestContext.getContinuationToken().equals("drained"))) {
            try {
                // Create tasks for each FeedRange context
                List<CompletableFuture<Void>> tasks = tickRequestContexts.stream()
                        .map(context -> CompletableFuture.runAsync(() ->
                                        fetchNextPage(context, docTypes, startTime, endTime, pinStart, feedResponseCache),
                                queryExecutorService))
                        .collect(Collectors.toList());

                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).get();

                resultTicks.addAll(findTopN(feedResponseCache.values().stream().collect(Collectors.toList()), totalTicks - resultTicks.size(), pinStart));

                feedResponseCache.clear();

            } catch (InterruptedException | ExecutionException e) {
                logger.error("Error during query execution: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to execute queries", e);
            }
        }

        Instant executionEndTime = Instant.now();
        logger.info("Execution of query with correlationId : {} finished in duration : {}", correlationId, Duration.between(executionStartTime, executionEndTime));

        for (TickRequestContextPerPartitionKey tickRequestContext : tickRequestContexts) {
            cosmosDiagnosticsList.addAll(tickRequestContext.getCosmosDiagnosticsList());
        }

        return new TickResponse(
                resultTicks,
                cosmosDiagnosticsList,
                Duration.between(executionStartTime, executionEndTime));
    }

    private void fetchNextPage(
            TickRequestContextPerPartitionKey tickRequestContext,
            List<String> docTypes,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean pinStart,
            ConcurrentHashMap<String, FeedResponse<Tick>> feedResponseCache) {

        CosmosAsyncContainer asyncContainer = tickRequestContext.getAsyncContainer();
        CosmosQueryRequestOptions queryRequestOptions = new CosmosQueryRequestOptions();

        queryRequestOptions.setPartitionKey(new PartitionKey(tickRequestContext.getTickIdentifier()));

        SqlQuerySpec querySpec = tickRequestContext.getSqlQuerySpec() != null ? tickRequestContext.getSqlQuerySpec() : getSqlQuerySpec(
                docTypes,
                startTime,
                endTime,
                tickRequestContext.getRequestDateAsString(),
                tickRequestContext.getDateFormat(),
                pinStart
        );

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
                feedResponseCache.put(tickRequestContext.getId(), response);
            }
        } catch (CosmosException e) {
            logger.error("Cosmos exception during page fetch: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Error during page fetch: {}", e.getMessage(), e);
        }
    }

    private SqlQuerySpec getSqlQuerySpec(
            List<String> docTypes,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String localDateAsString,
            String format,
            boolean pinStart) {

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

        if (pinStart) {
            String query = "SELECT * FROM C WHERE C.docType IN " + docTypePlaceholders +
                    " AND C.messageTimestamp >= @startTime AND C.messageTimestamp < @endTime ORDER BY C.messageTimestamp DESC, C.recordkey DESC";

            return new SqlQuerySpec(query, parameters);
        } else {
            String query = "SELECT * FROM C WHERE C.docType IN " + docTypePlaceholders +
                    " AND C.messageTimestamp >= @startTime AND C.messageTimestamp < @endTime ORDER BY C.messageTimestamp ASC, C.recordkey ASC";

            return new SqlQuerySpec(query, parameters);
        }
    }

    public List<Tick> findTopN(List<FeedResponse<Tick>> responses, int topN, boolean pinStart) {
        PriorityQueue<TickEntry> globalOrderedTicks = new PriorityQueue<>();

        for (int i = 0; i < responses.size(); i++) {
            List<Tick> ticks = responses.get(i).getResults();

            if (ticks != null && !ticks.isEmpty()) {
                int firstIndex = 0;
                globalOrderedTicks.offer(new TickEntry(ticks.get(firstIndex), i, firstIndex, pinStart));
            }
        }

        List<Tick> topNTicks = new ArrayList<>();

        while (!globalOrderedTicks.isEmpty() && topNTicks.size() < topN) {

            TickEntry tickEntry = globalOrderedTicks.poll();

            topNTicks.add(tickEntry.getTick());

            if (tickEntry.getElementIndex() < responses.get(tickEntry.getListIndex()).getResults().size() - 1) {
                int nextElementIndex = tickEntry.getElementIndex() + 1;
                int nextListIndex = tickEntry.getListIndex();

                Tick nextTick = responses.get(nextListIndex).getResults().get(nextElementIndex);

                globalOrderedTicks.offer(new TickEntry(nextTick, nextListIndex, nextElementIndex, pinStart));
            }
        }

        return topNTicks;
    }

    // Cleanup method for the executor services
    public void shutdown() {
        if (queryExecutorService != null && !queryExecutorService.isShutdown()) {
            queryExecutorService.shutdown();
            try {
                if (!queryExecutorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    queryExecutorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                queryExecutorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        if (priorityQueueExecutorService != null && !priorityQueueExecutorService.isShutdown()) {
            priorityQueueExecutorService.shutdown();
            try {
                if (!priorityQueueExecutorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    priorityQueueExecutorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                priorityQueueExecutorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
} 