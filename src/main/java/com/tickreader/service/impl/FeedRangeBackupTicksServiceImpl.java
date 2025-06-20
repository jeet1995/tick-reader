package com.tickreader.service.impl;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosDiagnosticsContext;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.implementation.Configs;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.FeedRange;
import com.azure.cosmos.models.PartitionKey;
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
import java.util.stream.Collectors;

@Service
public class FeedRangeBackupTicksServiceImpl implements BackupTicksService {

    private final static Logger logger = LoggerFactory.getLogger(FeedRangeBackupTicksServiceImpl.class);

    private final RicBasedCosmosClientFactory clientFactory;
    private final CosmosDbAccountConfiguration cosmosDbAccountConfiguration;
    private final ExecutorService queryExecutorService;
    private final ExecutorService priorityQueueExecutorService;
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

            Map<String, FeedRangeTickRequestContext> feedRangeContexts = buildFeedRangeContexts(rics, newStartTime, newEndTime);
            
            return executeQueryWithFeedRanges(
                    feedRangeContexts,
                    docTypes,
                    newStartTime,
                    newEndTime,
                    pinStart,
                    totalTicks);
        }, queryExecutorService);
    }

    private Map<String, FeedRangeTickRequestContext> buildFeedRangeContexts(
            List<String> rics, 
            LocalDateTime startTime, 
            LocalDateTime endTime) {
        
        Map<String, FeedRangeTickRequestContext> feedRangeContexts = new HashMap<>();

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

                // Create FeedRange contexts for each shard
                int shardCount = this.cosmosDbAccountConfiguration.getShardCountPerRic();
                for (int shardId = 1; shardId <= shardCount; shardId++) {
                    String contextKey = String.format("%s_%s_%d", hashIdForRic, date, shardId);
                    
                    FeedRangeTickRequestContext context = new FeedRangeTickRequestContext(
                            asyncContainer,
                            tickIdentifier,
                            shardId,
                            date,
                            dateFormat
                    );
                    
                    feedRangeContexts.put(contextKey, context);
                }
            }
        }

        return feedRangeContexts;
    }

    private TickResponse executeQueryWithFeedRanges(
            Map<String, FeedRangeTickRequestContext> feedRangeContexts,
            List<String> docTypes,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean pinStart,
            int totalTicks) {

        Instant executionStartTime = Instant.now();
        logger.info("Execution started at: {}", executionStartTime);

        List<CosmosDiagnosticsContext> allDiagnosticsContexts = Collections.synchronizedList(new ArrayList<>());
        
        // Single-threaded priority queue for thread safety
        PriorityQueue<Tick> priorityQueue = new PriorityQueue<>(
                totalTicks, 
                pinStart ? tickComparatorWithPinStartAsTrue : getTickComparatorWithPinStartAsFalse
        );

        try {
            // Create tasks for each FeedRange context
            List<CompletableFuture<Void>> tasks = feedRangeContexts.values().stream()
                    .map(context -> CompletableFuture.runAsync(() -> 
                            processFeedRangeContext(context, docTypes, startTime, endTime, pinStart, 
                                    priorityQueue, totalTicks, allDiagnosticsContexts), 
                            queryExecutorService))
                    .collect(Collectors.toList());

            // Wait for all tasks to complete
            CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).get();

            // Convert PriorityQueue to List (single-threaded operation)
            List<Tick> resultTicks = new ArrayList<>();
            Tick tick;
            while ((tick = priorityQueue.poll()) != null && resultTicks.size() < totalTicks) {
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

    private void processFeedRangeContext(
            FeedRangeTickRequestContext feedRangeContext,
            List<String> docTypes,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean pinStart,
            PriorityQueue<Tick> priorityQueue,
            int totalTicks,
            List<CosmosDiagnosticsContext> allDiagnosticsContexts) {

        try {
            SqlQuerySpec querySpec = getSqlQuerySpecForFeedRange(
                    feedRangeContext.getTickIdentifier(),
                    feedRangeContext.getShardId(),
                    docTypes,
                    startTime,
                    endTime,
                    feedRangeContext.getRequestDateAsString(),
                    feedRangeContext.getDateFormat(),
                    pinStart
            );

            // Create FeedRange for the specific shard
            String partitionKeyValue = feedRangeContext.getTickIdentifier() + "|" + feedRangeContext.getShardId();
            FeedRange feedRange = FeedRange.forLogicalPartition(new PartitionKey(partitionKeyValue));
            
            // Create query options with FeedRange
            CosmosQueryRequestOptions queryOptions = new CosmosQueryRequestOptions();
            queryOptions.setFeedRange(feedRange);

            // Execute fetchNextPage in parallel for pagination
            List<Tick> allTicks = executeFetchNextPageInParallel(
                    feedRangeContext.getAsyncContainer(),
                    querySpec,
                    queryOptions,
                    feedRangeContext,
                    allDiagnosticsContexts
            );
            
            // Add ticks to priority queue using single-threaded executor
            if (!allTicks.isEmpty()) {
                CompletableFuture.runAsync(() -> {
                    synchronized (priorityQueue) {
                        for (Tick tick : allTicks) {
                            if (priorityQueue.size() >= totalTicks) {
                                // If queue is full, check if this tick should replace the worst one
                                Tick head = priorityQueue.peek();
                                if (head != null && shouldReplace(head, tick, pinStart)) {
                                    priorityQueue.poll(); // Remove the worst tick
                                    priorityQueue.offer(tick);
                                }
                            } else {
                                priorityQueue.offer(tick);
                            }
                        }
                    }
                }, priorityQueueExecutorService).join(); // Wait for completion
            }

        } catch (Exception e) {
            logger.error("Error processing FeedRange context: {}", e.getMessage(), e);
        }
    }

    /**
     * Executes fetchNextPage calls in parallel for efficient pagination
     * This method coordinates parallel execution of fetchNextPage across multiple pages
     */
    private List<Tick> executeFetchNextPageInParallel(
            CosmosAsyncContainer asyncContainer,
            SqlQuerySpec querySpec,
            CosmosQueryRequestOptions queryOptions,
            FeedRangeTickRequestContext feedRangeContext,
            List<CosmosDiagnosticsContext> allDiagnosticsContexts) {

        List<Tick> allTicks = Collections.synchronizedList(new ArrayList<>());
        String continuationToken = null;
        int maxParallelPages = 5; // Limit parallel page fetches to avoid overwhelming the system

        try {
            do {
                // Create a batch of parallel fetchNextPage calls
                List<CompletableFuture<PageResult>> pageFutures = new ArrayList<>();
                
                // Start with the current continuation token
                String currentToken = continuationToken;
                
                // Create multiple parallel page fetch tasks
                for (int i = 0; i < maxParallelPages; i++) {
                    final String token = currentToken;
                    CompletableFuture<PageResult> pageFuture = CompletableFuture.supplyAsync(() -> 
                            fetchNextPage(asyncContainer, querySpec, queryOptions, token, pageSize), 
                            queryExecutorService);
                    
                    pageFutures.add(pageFuture);
                    
                    // If we get a continuation token, prepare for the next batch
                    if (currentToken != null) {
                        // We'll update currentToken after getting the first result
                        break;
                    }
                }

                // Wait for all page fetches to complete
                CompletableFuture<Void> allPagesFuture = CompletableFuture.allOf(
                        pageFutures.toArray(new CompletableFuture[0])
                );

                // Process results
                allPagesFuture.thenRun(() -> {
                    for (CompletableFuture<PageResult> future : pageFutures) {
                        try {
                            PageResult result = future.get();
                            if (result != null && result.getTicks() != null) {
                                allTicks.addAll(result.getTicks());
                                
                                if (result.getDiagnosticsContext() != null) {
                                    synchronized (allDiagnosticsContexts) {
                                        allDiagnosticsContexts.add(result.getDiagnosticsContext());
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.error("Error processing page result: {}", e.getMessage(), e);
                        }
                    }
                }).join();

                // Update continuation token for next iteration
                // Get the continuation token from the first successful result
                for (CompletableFuture<PageResult> future : pageFutures) {
                    try {
                        PageResult result = future.get();
                        if (result != null && result.getContinuationToken() != null) {
                            continuationToken = result.getContinuationToken();
                            break;
                        }
                    } catch (Exception e) {
                        // Continue to next future
                    }
                }

            } while (continuationToken != null);

        } catch (CosmosException e) {
            if (TickServiceUtils.isResourceNotFound(e)) {
                logger.warn("FeedRange {} for container {} does not have any records!", 
                        feedRangeContext.getShardId(), asyncContainer.getId());
            } else {
                logger.error("Cosmos exception during parallel page fetch: {}", e.getMessage(), e);
                throw e;
            }
        } catch (Exception e) {
            logger.error("Error during parallel page fetch: {}", e.getMessage(), e);
            throw new RuntimeException("Parallel page fetch failed", e);
        }

        return new ArrayList<>(allTicks);
    }

    /**
     * Fetches the next page of results from Cosmos DB
     * This method is designed to be called in parallel across multiple TickRequestContext instances
     * 
     * @param asyncContainer The Cosmos container to query
     * @param querySpec The SQL query specification
     * @param queryOptions The query options including FeedRange
     * @param continuationToken The continuation token for pagination (null for first page)
     * @param pageSize The size of the page to fetch
     * @return PageResult containing ticks, continuation token, and diagnostics, or null if no more results
     */
    private PageResult fetchNextPage(
            CosmosAsyncContainer asyncContainer,
            SqlQuerySpec querySpec,
            CosmosQueryRequestOptions queryOptions,
            String continuationToken,
            int pageSize) {

        try {
            // Execute query synchronously using block() with FeedRange
            var page = asyncContainer.queryItems(querySpec, queryOptions, Tick.class)
                    .byPage(continuationToken, pageSize)
                    .blockFirst();

            if (page != null && !page.getResults().isEmpty()) {
                return new PageResult(
                        page.getResults(),
                        page.getContinuationToken(),
                        page.getCosmosDiagnostics() != null ? page.getCosmosDiagnostics().getDiagnosticsContext() : null
                );
            } else if (page != null) {
                // Page exists but is empty, return with continuation token if available
                return new PageResult(
                        new ArrayList<>(),
                        page.getContinuationToken(),
                        page.getCosmosDiagnostics() != null ? page.getCosmosDiagnostics().getDiagnosticsContext() : null
                );
            }
            
            return null; // No more results

        } catch (CosmosException e) {
            logger.error("Cosmos exception during page fetch: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Error during page fetch: {}", e.getMessage(), e);
    private SqlQuerySpec getSqlQuerySpecForFeedRange(
            String tickIdentifier,
            int shardId,
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

        // Single partition key for FeedRange query
        String partitionKey = tickIdentifier + "|" + shardId;
        parameters.add(new SqlParameter("@pk", partitionKey));
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
            query = "SELECT * FROM C WHERE C.pk = @pk AND C.docType IN " + docTypePlaceholders +
                    " AND C.messageTimestamp >= @startTime AND C.messageTimestamp <= @endTime ORDER BY C.messageTimestamp DESC, C.recordkey DESC";
        } else {
            query = "SELECT * FROM C WHERE C.pk = @pk AND C.docType IN " + docTypePlaceholders +
                    " AND C.messageTimestamp >= @startTime AND C.messageTimestamp <= @endTime ORDER BY C.messageTimestamp ASC, C.recordkey ASC";
        }

        return new SqlQuerySpec(query, parameters);
    }

    // Inner class for FeedRange tick request context
    private static class FeedRangeTickRequestContext {
        private final CosmosAsyncContainer asyncContainer;
        private final String tickIdentifier;
        private final int shardId;
        private final String requestDateAsString;
        private final String dateFormat;
        private final List<CosmosDiagnosticsContext> diagnosticsContexts = new ArrayList<>();

        public FeedRangeTickRequestContext(CosmosAsyncContainer asyncContainer, String tickIdentifier, 
                                         int shardId, String requestDateAsString, String dateFormat) {
            this.asyncContainer = asyncContainer;
            this.tickIdentifier = tickIdentifier;
            this.shardId = shardId;
            this.requestDateAsString = requestDateAsString;
            this.dateFormat = dateFormat;
        }

        public CosmosAsyncContainer getAsyncContainer() { return asyncContainer; }
        public String getTickIdentifier() { return tickIdentifier; }
        public int getShardId() { return shardId; }
        public String getRequestDateAsString() { return requestDateAsString; }
        public String getDateFormat() { return dateFormat; }
        public List<CosmosDiagnosticsContext> getDiagnosticsContexts() { return diagnosticsContexts; }
        public void addDiagnosticsContext(CosmosDiagnosticsContext context) { 
            diagnosticsContexts.add(context); 
        }
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