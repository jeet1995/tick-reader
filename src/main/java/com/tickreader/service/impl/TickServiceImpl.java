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

/**
 * Implementation of TicksService that provides high-performance tick data retrieval from Cosmos DB.
 * 
 * This implementation uses:
 * - Asynchronous processing with CompletableFuture for non-blocking operations
 * - Parallel execution across multiple RICs and Cosmos DB containers
 * - Murmur3 hashing for RIC-based account distribution
 * - Date-based container partitioning for efficient data access
 * - Continuation token-based pagination for large result sets
 * - Thread pool management for controlled concurrency
 * 
 * Data Model:
 * - RIC -> CosmosDbAccount -> Database -> Container (daily granularity)
 * - Partition Key Format: <RIC>|yyyy-MM-dd|<shardId>
 * - Example: AAPL|2024-10-01|1, AAPL|2024-10-01|2, ..., AAPL|2024-10-01|8
 */
@Component
@ConditionalOnProperty(name = "ticks.implementation", havingValue = "completeablefuture")
public class TickServiceImpl implements TicksService {

    /** Logger for this class */
    private final static Logger logger = LoggerFactory.getLogger(TickServiceImpl.class);

    /** Factory for creating Cosmos DB clients based on RIC */
    private final RicBasedCosmosClientFactory clientFactory;
    
    /** Configuration for Cosmos DB accounts and containers */
    private final CosmosDbAccountConfiguration cosmosDbAccountConfiguration;
    
    /** Thread pool for executing concurrent queries */
    private final ExecutorService queryExecutorService;
    
    /** Concurrency level: CPU count * 10 for optimal performance */
    private final int concurrency = Configs.getCPUCnt() * 10;
    
    /** Object mapper configured to exclude null values */
    private final ObjectMapper nonNullObjectMapper = new ObjectMapper();

    /**
     * Constructor for TickServiceImpl.
     * 
     * @param clientFactory Factory for creating Cosmos DB clients
     * @param cosmosDbAccountConfiguration Configuration for Cosmos DB accounts
     */
    public TickServiceImpl(RicBasedCosmosClientFactory clientFactory,
                           CosmosDbAccountConfiguration cosmosDbAccountConfiguration) {
        this.clientFactory = clientFactory;
        this.cosmosDbAccountConfiguration = cosmosDbAccountConfiguration;
        
        // Initialize thread pool with concurrency level based on CPU count
        this.queryExecutorService = Executors.newFixedThreadPool(concurrency);

        // Configure object mapper to exclude null values from serialization
        nonNullObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /**
     * Retrieves tick data for specified RICs within a time range.
     * 
     * This method provides a synchronous interface that wraps the asynchronous implementation.
     * It handles parameter validation, executes the query asynchronously, and returns the results.
     * 
     * @param rics List of RIC (Reuters Instrument Code) identifiers to query
     * @param docTypes List of document types to filter by
     * @param totalTicks Maximum number of ticks to return per RIC
     * @param pinStart If true, sorts by ascending timestamp; if false, sorts by descending timestamp
     * @param startTime Start of the time range for data retrieval
     * @param endTime End of the time range for data retrieval
     * @param includeNullValues If true, includes null values in response; if false, filters them out
     * @param pageSize Number of items per page for pagination
     * @param includeDiagnostics If true, includes Cosmos DB diagnostics in response
     * @param projections List of field names to include in the SELECT clause. If null or empty, selects all fields (SELECT *)
     * @return TickResponse containing the retrieved tick data and execution metrics
     * @throws RuntimeException if the query execution fails
     * @throws IllegalArgumentException if projections validation fails
     */
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
            boolean includeDiagnostics,
            List<String> projections) {
        
        // Validate projections parameter
        validateProjections(projections);
        
        try {
            // Execute the asynchronous query and wait for completion
            return getTicksAsync(rics, docTypes, totalTicks, pinStart, startTime, endTime, includeNullValues, pageSize, includeDiagnostics, projections).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error executing getTicks: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get ticks", e);
        }
    }

    /**
     * Asynchronous implementation of tick data retrieval.
     * 
     * This method orchestrates the entire tick retrieval process:
     * 1. Normalizes the time range (swaps start/end if needed)
     * 2. Builds execution contexts for each RIC across date ranges
     * 3. Executes parallel queries across multiple Cosmos DB containers
     * 4. Aggregates and returns the results
     * 
     * Data Model Example:
     * - Time Range: 2024-10-01T00:00:00 to 2024-10-05T23:59:59
     * - RIC: AAPL
     * - Partition Key Format: <RIC>|yyyy-MM-dd|<shardId>
     * - Containers: AAPL|2024-10-01|1-8, AAPL|2024-10-02|1-8, ..., AAPL|2024-10-05|1-8
     * 
     * @param rics List of RIC identifiers to query
     * @param docTypes List of document types to filter by
     * @param totalTicks Maximum number of ticks to return per RIC
     * @param pinStart Sorting order flag
     * @param startTime Start of time range
     * @param endTime End of time range
     * @param includeNullValues Whether to include null values
     * @param pageSize Page size for pagination
     * @param includeDiagnostics Whether to include diagnostics
     * @param projections List of field names to include in the SELECT clause. If null or empty, selects all fields (SELECT *)
     * @return CompletableFuture containing the TickResponse
     */
    private CompletableFuture<TickResponse> getTicksAsync(
            List<String> rics,
            List<String> docTypes,
            int totalTicks,
            boolean pinStart,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean includeNullValues,
            int pageSize,
            boolean includeDiagnostics,
            List<String> projections) {

        return CompletableFuture.supplyAsync(() -> {
            // Normalize time range - ensure startTime <= endTime
            LocalDateTime newStartTime = startTime.isAfter(endTime) ? endTime : startTime;
            LocalDateTime newEndTime = endTime.isBefore(startTime) ? startTime : endTime;

            // Build execution contexts for each RIC across date ranges
            Map<String, RicQueryExecutionState> ricToRicQueryExecutionState = 
                buildTickRequestContexts(rics, newStartTime, newEndTime, pinStart);
            
            // Generate correlation ID for request tracking
            String correlationId = UUID.randomUUID().toString();

            // Execute queries and return response
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
                    includeDiagnostics,
                    projections);
        }, queryExecutorService);
    }

    /**
     * Builds execution contexts for each RIC across multiple date ranges and Cosmos DB containers.
     * 
     * This method:
     * 1. Uses Murmur3 hashing to distribute RICs across Cosmos DB accounts
     * 2. Generates date ranges between start and end times
     * 3. Creates execution contexts for each RIC+date combination
     * 4. Maps contexts to appropriate Cosmos DB containers
     * 
     * @param rics List of RIC identifiers
     * @param startTime Start of time range
     * @param endTime End of time range
     * @param pinStart Sorting order flag
     * @return Map of RIC to execution state containing all contexts
     */
    private Map<String, RicQueryExecutionState> buildTickRequestContexts(
            List<String> rics, 
            LocalDateTime startTime, 
            LocalDateTime endTime,
            boolean pinStart) {

        Map<String, RicQueryExecutionState> ricToRicQueryExecutionState = new HashMap<>();

        for (String ric : rics) {
            List<TickRequestContextPerPartitionKey> tickRequestContexts = new ArrayList<>();

            // Step 1: Calculate hash for RIC to determine Cosmos DB account
            int seed = 42;
            UTF8String s = UTF8String.fromString(ric);
            int hash = Murmur3_x86_32.hashUnsafeBytes(s.getBaseObject(), s.getBaseOffset(), s.numBytes(), seed);
            int hashIdForRic = Math.abs(hash) % this.cosmosDbAccountConfiguration.getAccountCount() + 1;

            // Step 2: Get Cosmos DB account configuration
            CosmosDbAccount cosmosDbAccount = this.cosmosDbAccountConfiguration.getCosmosDbAccount(hashIdForRic);
            String dateFormat = cosmosDbAccount.getContainerNameFormat();

            // Step 3: Generate list of dates between start and end times
            List<String> datesInBetween = TickServiceUtils.getLocalDatesBetweenTwoLocalDateTimes(startTime, endTime, dateFormat, pinStart);

            // Step 4: Create execution contexts for each date
            for (String date : datesInBetween) {
                // Get Cosmos DB client for this account
                CosmosAsyncClient asyncClient = this.clientFactory.getCosmosAsyncClient(hashIdForRic);

                if (asyncClient == null) {
                    logger.warn("CosmosAsyncClient instance not found for ric: {}", ric);
                    continue;
                }

                // Validate database name
                String databaseName = cosmosDbAccount.getDatabaseName();
                if (databaseName == null || databaseName.isEmpty()) {
                    logger.warn("Ric {} is not assigned to a database", ric);
                    continue;
                }

                // Get container for this specific date
                CosmosAsyncContainer asyncContainer = asyncClient.getDatabase(databaseName)
                        .getContainer(cosmosDbAccount.getContainerNamePrefix() + date + cosmosDbAccount.getContainerNameSuffix());

                // Create tick identifier for this RIC+date combination
                String tickIdentifier = TickServiceUtils.constructTickIdentifierPrefix(ric, date);

                // Create execution context
                TickRequestContextPerPartitionKey tickRequestContext = new TickRequestContextPerPartitionKey(
                        asyncContainer,
                        tickIdentifier,
                        date,
                        dateFormat);

                tickRequestContexts.add(tickRequestContext);
            }

            // Step 5: Create execution state for this RIC
            if (!tickRequestContexts.isEmpty()) {
                ricToRicQueryExecutionState.put(ric, new RicQueryExecutionState(tickRequestContexts));
            } else {
                logger.warn("No tick request contexts found for ric: {}", ric);
            }
        }

        return ricToRicQueryExecutionState;
    }

    /**
     * Executes parallel queries across multiple Cosmos DB containers and aggregates results.
     * 
     * This method orchestrates the parallel execution of queries:
     * 1. Creates concurrent tasks for each RIC execution state
     * 2. Fetches data pages until all queries are completed
     * 3. Aggregates results from all RICs
     * 4. Applies sorting and null value filtering
     * 5. Returns final response with execution metrics
     * 
     * @param rics List of RIC identifiers
     * @param ricToRicQueryExecutionState Map of RIC to execution state
     * @param docTypes List of document types to filter by
     * @param startTime Start of time range
     * @param endTime End of time range
     * @param totalTicks Maximum number of ticks to return
     * @param correlationId Correlation ID for request tracking
     * @param includeNullValues Whether to include null values
     * @param pinStart Sorting order flag
     * @param pageSize Page size for pagination
     * @param includeDiagnostics Whether to include diagnostics
     * @param projections List of field names to include in the SELECT clause. If null or empty, selects all fields (SELECT *)
     * @return TickResponse with aggregated results and execution metrics
     */
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
            boolean includeDiagnostics,
            List<String> projections) {

        // Record execution start time for performance tracking
        Instant executionStartTime = Instant.now();
        logger.info("Execution of query with correlationId : {} started at : {}", correlationId, executionStartTime);

        // Thread-safe list for collecting Cosmos DB diagnostics
        List<String> cosmosDiagnosticsContextList = Collections.synchronizedList(new ArrayList<>());

        // List to hold all retrieved ticks
        List<Tick> resultTicks = new ArrayList<>();

        // Phase 1: Execute parallel queries until all are completed
        while (!ricToRicQueryExecutionState.values().stream().allMatch(RicQueryExecutionState::isCompleted)) {
            try {
                // Create concurrent tasks for each RIC execution state
                List<CompletableFuture<Void>> tasks = ricToRicQueryExecutionState.values().stream()
                        .map(ricQueryExecutionState -> CompletableFuture.runAsync(() ->
                                        fetchNextPage(ricQueryExecutionState, docTypes, startTime, endTime, pageSize, pinStart, totalTicks, projections),
                                queryExecutorService))
                        .collect(Collectors.toList());

                // Wait for all tasks to complete
                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).get();
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Error during query execution: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to execute queries", e);
            }
        }

        // Record execution end time and log duration
        Instant executionEndTime = Instant.now();
        logger.info("Execution of query with correlationId : {} finished in duration : {}", correlationId, Duration.between(executionStartTime, executionEndTime));

        // Phase 2: Aggregate results from all RICs
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

            // Collect Cosmos DB diagnostics from all contexts
            for (TickRequestContextPerPartitionKey tickRequestContextPerPartitionKey : ricQueryExecutionState.getTickRequestContexts()) {
                if (tickRequestContextPerPartitionKey.getCosmosDiagnosticsList() != null) {
                    cosmosDiagnosticsContextList.addAll(tickRequestContextPerPartitionKey.getCosmosDiagnosticsList().stream().map(cosmosDiagnosticsContext -> cosmosDiagnosticsContext.getDiagnosticsContext().toJson()).collect(Collectors.toList()));
                }
            }

            // Apply sorting based on pinStart flag
            if (pinStart) {
                Collections.reverse(ticks);
            }

            resultTicks.addAll(ticks);
        }

        // Phase 3: Convert to final response format
        List<BaseTick> finalTicks = new ArrayList<>();

        if (includeNullValues) {
            // Include all ticks with null values
            finalTicks.addAll(resultTicks);
        } else {
            // Convert to TickWithNoNulls to filter out null values
            List<TickWithNoNulls> newTicks = resultTicks.stream()
                    .map(tick -> nonNullObjectMapper.convertValue(tick, TickWithNoNulls.class))
                    .collect(Collectors.toList());

            finalTicks.addAll(newTicks);
        }

        // Return response with execution metrics
        return new TickResponse(
                finalTicks,
                includeDiagnostics ? cosmosDiagnosticsContextList : Collections.emptyList(),
                Duration.between(executionStartTime, executionEndTime));
    }

    /**
     * Fetches a single page of data from a specific Cosmos DB container.
     * 
     * This method:
     * 1. Selects the next available execution context
     * 2. Builds or retrieves the SQL query specification
     * 3. Executes the query with pagination support
     * 4. Processes the response and updates execution state
     * 5. Handles Cosmos DB exceptions gracefully
     * 
     * @param ricQueryExecutionState Execution state for a specific RIC
     * @param docTypes List of document types to filter by
     * @param startTime Start of time range
     * @param endTime End of time range
     * @param pageSize Number of items per page
     * @param pinStart Sorting order flag
     * @param totalTicks Maximum number of ticks to return
     * @param projections List of field names to include in the SELECT clause. If null or empty, selects all fields (SELECT *)
     */
    private void fetchNextPage(
            RicQueryExecutionState ricQueryExecutionState,
            List<String> docTypes,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int pageSize,
            boolean pinStart,
            int totalTicks,
            List<String> projections) {

        // Step 1: Get next available execution context
        TickRequestContextPerPartitionKey tickRequestContext
                = TickServiceUtils.evaluateTickRequestContextToExecute(ricQueryExecutionState);

        if (tickRequestContext == null) {
            // No more contexts available, mark execution as completed
            ricQueryExecutionState.setCompleted(true);
            return;
        }

        // Step 2: Prepare query execution
        CosmosAsyncContainer asyncContainer = tickRequestContext.getAsyncContainer();
        CosmosQueryRequestOptions queryRequestOptions = new CosmosQueryRequestOptions();

        // Step 3: Build or retrieve SQL query specification
        SqlQuerySpec querySpec = tickRequestContext.getSqlQuerySpec() != null ? 
            tickRequestContext.getSqlQuerySpec() : 
            getSqlQuerySpec(
                tickRequestContext.getTickIdentifier(),
                docTypes,
                startTime,
                endTime,
                tickRequestContext.getRequestDateAsString(),
                tickRequestContext.getDateFormat(),
                pinStart,
                totalTicks,
                projections);

        tickRequestContext.setSqlQuerySpec(querySpec);

        // Step 4: Check continuation token
        String continuationToken = tickRequestContext.getContinuationToken();
        if (continuationToken != null && continuationToken.equals("drained")) {
            // This context has been fully processed
            return;
        }

        // Step 5: Execute query with error handling
        try {
            FeedResponse<Tick> response = asyncContainer.queryItems(querySpec, queryRequestOptions, Tick.class)
                    .byPage(continuationToken, pageSize)
                    .next()
                    .onErrorResume(throwable -> {
                        // Handle Cosmos DB exceptions
                        if (throwable instanceof CosmosException) {
                            CosmosException cosmosException = (CosmosException) throwable;
                            logger.error("Cosmos exception during page fetch: {}", cosmosException.getMessage(), cosmosException);

                            // Handle resource not found scenario
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

            // Step 6: Process response
            if (response != null) {
                // Update continuation token for next page
                tickRequestContext.setContinuationToken(response.getContinuationToken() != null ? response.getContinuationToken() : "drained");
                
                // Collect diagnostics information
                tickRequestContext.addCosmosDiagnostics(response.getCosmosDiagnostics());

                // Add results to execution state
                ricQueryExecutionState.addTicks(response.getResults(), totalTicks);
            }
        } catch (CosmosException e) {
            logger.error("Cosmos exception during page fetch: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Error during page fetch: {}", e.getMessage(), e);
        }
    }

    /**
     * Builds parameterized SQL query specification for Cosmos DB.
     * 
     * This method constructs a parameterized SQL query with:
     * - Partition key filtering across multiple shards
     * - Document type filtering
     * - Time range filtering with nanosecond precision
     * - Sorting based on message timestamp
     * - Result limiting
     * - Field projection (optional)
     * 
     * Query Structure:
     * SELECT [projections] FROM C 
     * WHERE C.pk IN (@pk1, @pk2, ..., @pk8) 
     *   AND C.docType IN (@docType0, @docType1, ...) 
     *   AND C.messageTimestamp >= @startTime 
     *   AND C.messageTimestamp < @endTime 
     * ORDER BY C.messageTimestamp [ASC|DESC] 
     * OFFSET 0 LIMIT @totalTicks
     * 
     * @param tickIdentifier Base identifier for the tick (RIC|date)
     * @param docTypes List of document types to filter by
     * @param startTime Start of time range
     * @param endTime End of time range
     * @param localDateAsString Date string for container-specific time bounds
     * @param format Date format pattern
     * @param pinStart If true, sort ascending; if false, sort descending
     * @param totalTicks Maximum number of ticks to return
     * @param projections List of field names to include in the SELECT clause. If null or empty, selects all fields (SELECT *)
     * @return SqlQuerySpec with parameterized query and parameters
     */
    private SqlQuerySpec getSqlQuerySpec(
            String tickIdentifier,
            List<String> docTypes,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String localDateAsString,
            String format,
            boolean pinStart,
            int totalTicks,
            List<String> projections) {

        // Parse the date string to get container-specific date bounds
        LocalDate localDate = LocalDate.parse(localDateAsString, DateTimeFormatter.ofPattern(format));

        // Calculate query time bounds with nanosecond precision
        // Use container date bounds if query time range extends beyond them
        long queryStartTime = !startTime.isBefore(localDate.atStartOfDay()) ? 
            startTime.toInstant(ZoneOffset.UTC).toEpochMilli() * 1_000_000L : 
            localDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() * 1_000_000L;
        
        long queryEndTime = !endTime.isAfter(localDate.atTime(23, 59, 59, 999_999_999)) ? 
            endTime.toInstant(ZoneOffset.UTC).toEpochMilli() * 1_000_000L : 
            localDate.atTime(23, 59, 59, 999_999_999).toInstant(ZoneOffset.UTC).toEpochMilli() * 1_000_000L;

        // Initialize parameter list
        List<SqlParameter> parameters = new ArrayList<>();

        // Add time range parameters
        parameters.add(new SqlParameter("@startTime", queryStartTime));
        parameters.add(new SqlParameter("@endTime", queryEndTime));

        // Build document type filter parameters
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

        // Build partition key filter parameters
        StringBuilder partitionKeyPlaceholders = new StringBuilder();
        partitionKeyPlaceholders.append("(");

        // Create parameters for each shard (partition key)
        for (int i = 1; i <= this.cosmosDbAccountConfiguration.getShardCountPerRic(); i++) {
            String param = "@pk" + i;
            parameters.add(new SqlParameter(param, tickIdentifier + "|" + i));
            partitionKeyPlaceholders.append(param);
            if (i <= this.cosmosDbAccountConfiguration.getShardCountPerRic() - 1) {
                partitionKeyPlaceholders.append(", ");
            }
        }
        partitionKeyPlaceholders.append(")");

        // Build SELECT clause with projections
        String selectClause;
        if (projections != null && !projections.isEmpty()) {
            // Use specified projections
            selectClause = "SELECT " + String.join(", ", projections.stream()
                    .map(field -> "C." + field)
                    .collect(Collectors.toList())) + " FROM C";
        } else {
            // Select all fields
            selectClause = "SELECT * FROM C";
        }

        // Build final query with appropriate sorting
        if (pinStart) {
            // Ascending order for pinStart=true
            String query = selectClause + " WHERE C.pk IN " + partitionKeyPlaceholders + 
                          " AND C.docType IN " + docTypePlaceholders +
                          " AND C.messageTimestamp >= @startTime AND C.messageTimestamp < @endTime " +
                          "ORDER BY C.messageTimestamp ASC OFFSET 0 LIMIT " + totalTicks;

            return new SqlQuerySpec(query, parameters);
        } else {
            // Descending order for pinStart=false
            String query = selectClause + " WHERE C.pk IN " + partitionKeyPlaceholders + 
                          " AND C.docType IN " + docTypePlaceholders +
                          " AND C.messageTimestamp >= @startTime AND C.messageTimestamp < @endTime " +
                          "ORDER BY C.messageTimestamp DESC OFFSET 0 LIMIT " + totalTicks;

            return new SqlQuerySpec(query, parameters);
        }
    }

    /**
     * Validates the projections parameter to ensure it meets the required criteria.
     * 
     * Validation Rules:
     * 1. If projections list is provided (not null), it cannot be empty
     * 2. If projections list is provided, it must include 'messageTimestamp' field
     *    (required for sorting functionality)
     * 
     * @param projections List of field names to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateProjections(List<String> projections) {
        // If projections is null, it's valid (will use SELECT *)
        if (projections == null) {
            return;
        }
        
        // Rule 1: If projections list is provided, it cannot be empty
        if (projections.isEmpty()) {
            logger.error("Projections list cannot be empty. Either provide valid field names or omit the parameter to select all fields.");
            throw new IllegalArgumentException("Projections list cannot be empty. Either provide valid field names or omit the parameter to select all fields.");
        }
        
        // Rule 2: If projections list is provided, it must include 'messageTimestamp'
        if (!projections.contains("messageTimestamp")) {
            logger.error("Projections list must include 'messageTimestamp' field for sorting functionality. Provided projections: {}", projections);
            throw new IllegalArgumentException("Projections list must include 'messageTimestamp' field for sorting functionality. Provided projections: " + projections);
        }
        
        logger.debug("Projections validation passed. Valid projections: {}", projections);
    }
}