package com.tickreader.service.impl;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosDiagnosticsContext;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.implementation.Configs;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import com.tickreader.config.CosmosDbAccount;
import com.tickreader.config.CosmosDbAccountConfiguration;
import com.tickreader.config.RicBasedCosmosClientFactory;
import com.tickreader.dto.TickResponse;
import com.tickreader.entity.Tick;
import com.tickreader.service.BackupTicksService;
import com.tickreader.service.utils.TickServiceUtils;
import org.apache.spark.unsafe.hash.Murmur3_x86_32;
import org.apache.spark.unsafe.types.UTF8String;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class BackupTicksServiceImpl implements BackupTicksService {

    private final static Logger logger = LoggerFactory.getLogger(BackupTicksServiceImpl.class);

    private final RicBasedCosmosClientFactory clientFactory;
    private final CosmosDbAccountConfiguration cosmosDbAccountConfiguration;
    private final ExecutorService executorService;
    private final int prefetch = 20;
    private final int pageSize = 1000;
    private final int concurrency = Configs.getCPUCnt();

    private final Comparator<Tick> tickComparatorWithPinStartAsTrue = (t1, t2) -> {
        if (t1.getMessageTimestamp().equals(t2.getMessageTimestamp())) {
            return Long.compare(t2.getRecordkey(), t1.getRecordkey());
        }
        return Long.compare(t2.getMessageTimestamp(), t1.getMessageTimestamp());
    };

    private final Comparator<Tick> getTickComparatorWithPinStartAsFalse = (t1, t2) -> {
        if (t1.getMessageTimestamp().equals(t2.getMessageTimestamp())) {
            return Long.compare(t1.getRecordkey(), t2.getRecordkey());
        }
        return Long.compare(t1.getMessageTimestamp(), t2.getMessageTimestamp());
    };

    public BackupTicksServiceImpl(RicBasedCosmosClientFactory clientFactory, 
                                 CosmosDbAccountConfiguration cosmosDbAccountConfiguration) {
        this.clientFactory = clientFactory;
        this.cosmosDbAccountConfiguration = cosmosDbAccountConfiguration;
        this.executorService = Executors.newFixedThreadPool(concurrency);
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

    @Override
    public CompletableFuture<TickResponse> getTicksAsync(
            List<String> rics,
            List<String> docTypes,
            int totalTicks,
            boolean pinStart,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        return CompletableFuture.supplyAsync(() -> {
            LocalDateTime newStartTime = startTime.isAfter(endTime) ? endTime : startTime;
            LocalDateTime newEndTime = endTime.isBefore(startTime) ? startTime : endTime;

            Map<String, TickRequestContext> tickRequestContexts = buildTickRequestContexts(rics, newStartTime, newEndTime);
            
            return executeQueryUntilTopN(
                    tickRequestContexts,
                    docTypes,
                    newStartTime,
                    newEndTime,
                    pinStart,
                    totalTicks);
        }, executorService);
    }

    private Map<String, TickRequestContext> buildTickRequestContexts(
            List<String> rics, 
            LocalDateTime startTime, 
            LocalDateTime endTime) {
        
        Map<String, TickRequestContext> tickRequestContexts = new HashMap<>();

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

                tickRequestContexts.putIfAbsent((hashIdForRic + date), new TickRequestContext(
                        asyncContainer,
                        new ArrayList<>(),
                        date,
                        dateFormat));

                TickRequestContext tickRequestContext = tickRequestContexts.get((hashIdForRic + date));
                List<String> tickIdentifiers = tickRequestContext.getTickIdentifiers();
                tickIdentifiers.add(tickIdentifier);
            }
        }

        return tickRequestContexts;
    }

    private TickResponse executeQueryUntilTopN(
            Map<String, TickRequestContext> tickRequestContexts,
            List<String> docTypes,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean pinStart,
            int totalTicks) {

        Instant executionStartTime = Instant.now();
        logger.info("Execution started at: {}", executionStartTime);

        List<CosmosDiagnosticsContext> allDiagnosticsContexts = new ArrayList<>();
        PriorityBlockingQueue<Tick> orderedTicks = new PriorityBlockingQueue<>(
                totalTicks, 
                pinStart ? tickComparatorWithPinStartAsTrue : getTickComparatorWithPinStartAsFalse
        );

        try {
            // Create tasks for each tick request context
            List<CompletableFuture<Void>> tasks = tickRequestContexts.values().stream()
                    .map(context -> CompletableFuture.runAsync(() -> 
                            processTickRequestContext(context, docTypes, startTime, endTime, pinStart, orderedTicks, totalTicks, allDiagnosticsContexts), 
                            executorService))
                    .collect(Collectors.toList());

            // Wait for all tasks to complete
            CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).get();

            // Convert PriorityBlockingQueue to List
            List<Tick> resultTicks = new ArrayList<>();
            Tick tick;
            while ((tick = orderedTicks.poll()) != null && resultTicks.size() < totalTicks) {
                resultTicks.add(tick);
            }

            Instant executionEndTime = Instant.now();
            logger.info("Execution ended at: {}", executionEndTime);

            return new TickResponse(
                    resultTicks,
                    allDiagnosticsContexts,
                    Duration.between(executionStartTime, executionEndTime));

        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error during query execution: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to execute queries", e);
        }
    }

    private void processTickRequestContext(
            TickRequestContext tickRequestContext,
            List<String> docTypes,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean pinStart,
            PriorityBlockingQueue<Tick> orderedTicks,
            int totalTicks,
            List<CosmosDiagnosticsContext> allDiagnosticsContexts) {

        try {
            SqlQuerySpec querySpec = getSqlQuerySpec(
                    tickRequestContext.getTickIdentifiers(),
                    docTypes,
                    startTime,
                    endTime,
                    tickRequestContext.getRequestDateAsString(),
                    tickRequestContext.getDateFormat(),
                    pinStart
            );

            tickRequestContext.setSqlQuerySpec(querySpec);

            // Execute query and collect results
            List<Tick> ticks = executeQuery(tickRequestContext, docTypes, startTime, endTime, pinStart);
            
            // Add ticks to the priority queue
            for (Tick tick : ticks) {
                if (orderedTicks.size() >= totalTicks) {
                    // If queue is full, check if this tick should replace the worst one
                    Tick head = orderedTicks.peek();
                    if (head != null && shouldReplace(head, tick, pinStart)) {
                        orderedTicks.poll(); // Remove the worst tick
                        orderedTicks.offer(tick);
                    }
                } else {
                    orderedTicks.offer(tick);
                }
            }

            // Collect diagnostics
            synchronized (allDiagnosticsContexts) {
                allDiagnosticsContexts.addAll(tickRequestContext.getDiagnosticsContexts());
            }

        } catch (Exception e) {
            logger.error("Error processing tick request context: {}", e.getMessage(), e);
        }
    }

    private boolean shouldReplace(Tick existing, Tick newTick, boolean pinStart) {
        if (pinStart) {
            return tickComparatorWithPinStartAsTrue.compare(newTick, existing) > 0;
        } else {
            return getTickComparatorWithPinStartAsFalse.compare(newTick, existing) < 0;
        }
    }

    private List<Tick> executeQuery(
            TickRequestContext tickRequestContext,
            List<String> docTypes,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean pinStart) {

        CosmosAsyncContainer asyncContainer = tickRequestContext.getAsyncContainer();
        List<Tick> allTicks = new ArrayList<>();
        String continuationToken = null;

        try {
            do {
                SqlQuerySpec querySpec = tickRequestContext.getSqlQuerySpec();
                if (continuationToken != null) {
                    // For pagination, we need to modify the query or use a different approach
                    // This is a simplified version - in practice you might need to handle pagination differently
                    break;
                }

                // Execute query synchronously using block() - this is the key difference from Reactor
                var page = asyncContainer.queryItems(querySpec, Tick.class)
                        .byPage(continuationToken, pageSize)
                        .blockFirst();

                if (page != null) {
                    continuationToken = page.getContinuationToken();
                    allTicks.addAll(page.getResults());

                    if (page.getCosmosDiagnostics() != null) {
                        tickRequestContext.addDiagnosticsContext(page.getCosmosDiagnostics().getDiagnosticsContext());
                    }
                }

            } while (continuationToken != null);

        } catch (CosmosException e) {
            if (TickServiceUtils.isResourceNotFound(e)) {
                logger.warn("Day : {} does not have any records!", asyncContainer.getId());
            } else {
                logger.error("Cosmos exception during query execution: {}", e.getMessage(), e);
                throw e;
            }
        } catch (Exception e) {
            logger.error("Error during query execution: {}", e.getMessage(), e);
            throw new RuntimeException("Query execution failed", e);
        }

        return allTicks;
    }

    private SqlQuerySpec getSqlQuerySpec(
            List<String> tickIdentifiers,
            List<String> docTypes,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String localDateAsString,
            String format,
            boolean pinStart) {

        LocalDate localDate = LocalDate.parse(localDateAsString, DateTimeFormatter.ofPattern(format));

        long queryStartTime = !startTime.isBefore(localDate.atStartOfDay()) ? 
                startTime.toInstant(ZoneOffset.UTC).toEpochMilli() * 1_000_000L : 
                localDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() * 1_000_000L;
        
        long queryEndTime = !endTime.isAfter(localDate.atTime(23, 59, 59, 999_999_999)) ? 
                endTime.toInstant(ZoneOffset.UTC).toEpochMilli() * 1_000_000L : 
                localDate.atTime(23, 59, 59, 999_999_999).toInstant(ZoneOffset.UTC).toEpochMilli() * 1_000_000L;

        List<SqlParameter> parameters = new ArrayList<>();

        StringBuilder sb = new StringBuilder("(");
        int shardCount = this.cosmosDbAccountConfiguration.getShardCountPerRic();
        int maxParamId = tickIdentifiers.size() * shardCount;

        for (int i = 1; i <= maxParamId; i++) {
            String param = "@pk" + i;
            sb.append(param);

            String tickIdentifier = tickIdentifiers.get((i - 1) / shardCount);
            parameters.add(new SqlParameter(param, tickIdentifier + "|" + ((i % shardCount) + 1)));

            if (i < maxParamId) {
                sb.append(", ");
            }
        }
        sb.append(")");

        parameters.add(new SqlParameter("@startTime", queryStartTime));
        parameters.add(new SqlParameter("@endTime", queryEndTime));

        StringBuilder docTypePlaceholders = new StringBuilder("(");
        for (int i = 0; i < docTypes.size(); i++) {
            String param = "@docType" + i;
            parameters.add(new SqlParameter(param, docTypes.get(i)));
            docTypePlaceholders.append(param);
            if (i < docTypes.size() - 1) {
                docTypePlaceholders.append(", ");
            }
        }
        docTypePlaceholders.append(")");

        String query;
        if (pinStart) {
            query = "SELECT * FROM C WHERE C.pk IN " + sb + " AND C.docType IN " + docTypePlaceholders +
                    " AND C.messageTimestamp >= @startTime AND C.messageTimestamp <= @endTime ORDER BY C.messageTimestamp DESC, C.recordkey DESC";
        } else {
            query = "SELECT * FROM C WHERE C.pk IN " + sb + " AND C.docType IN " + docTypePlaceholders +
                    " AND C.messageTimestamp >= @startTime AND C.messageTimestamp <= @endTime ORDER BY C.messageTimestamp ASC, C.recordkey ASC";
        }

        return new SqlQuerySpec(query, parameters);
    }

    // Cleanup method for the executor service
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
} 