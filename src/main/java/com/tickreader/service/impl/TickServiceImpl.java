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
     * @return TickResponse containing the retrieved tick data and execution metrics
     * @throws RuntimeException if the query execution fails
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
        
        try {
            // Execute the asynchronous query and wait for completion
            return getTicksAsync(rics, docTypes, totalTicks, pinStart, startTime, endTime, includeNullValues, pageSize, includeDiagnostics, projections).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error executing getTicks: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get ticks", e);
        }
    }

    /**
     * Retrieves tick data for specified RICs within a time range with additional range filters.
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
     * @param trdprc1Min Minimum value for TRDPRC_1 filter (inclusive)
     * @param trdprc1Max Maximum value for TRDPRC_1 filter (inclusive)
     * @param trnovrUnsMin Minimum value for TRNOVR_UNS filter (inclusive)
     * @param trnovrUnsMax Maximum value for TRNOVR_UNS filter (inclusive)
     * @return TickResponse containing the retrieved tick data and execution metrics
     * @throws RuntimeException if the query execution fails
     */
    @Override
    public TickResponse getTicksWithRangeFilters(
            List<String> rics,
            List<String> docTypes,
            int totalTicks,
            boolean pinStart,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean includeNullValues,
            int pageSize,
            boolean includeDiagnostics,
            List<String> projections,
            Double trdprc1Min,
            Double trdprc1Max,
            Double trnovrUnsMin,
            Double trnovrUnsMax) {
        
        try {
            // Execute the asynchronous query and wait for completion
            return getTicksWithRangeFiltersAsync(rics, docTypes, totalTicks, pinStart, startTime, endTime, 
                    includeNullValues, pageSize, includeDiagnostics, projections,
                    trdprc1Min, trdprc1Max, trnovrUnsMin, trnovrUnsMax).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error executing getTicksWithRangeFilters: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get ticks with range filters", e);
        }
    }

    /**
     * Retrieves tick data for specified RICs within a time range with price and volume filters.
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
     * @param trdprc1Min Minimum value for TRDPRC_1 filter (inclusive)
     * @param trdprc1Max Maximum value for TRDPRC_1 filter (inclusive)
     * @param trdvol1Min Minimum value for TRDVOL_1 filter (inclusive)
     * @return TickResponse containing the retrieved tick data and execution metrics
     * @throws RuntimeException if the query execution fails
     */
    @Override
    public TickResponse getTicksWithPriceVolumeFilters(
            List<String> rics,
            List<String> docTypes,
            int totalTicks,
            boolean pinStart,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean includeNullValues,
            int pageSize,
            boolean includeDiagnostics,
            List<String> projections,
            Double trdprc1Min,
            Double trdprc1Max,
            Double trdvol1Min) {
        
        try {
            // Execute the asynchronous query and wait for completion
            return getTicksWithPriceVolumeFiltersAsync(rics, docTypes, totalTicks, pinStart, startTime, endTime, 
                    includeNullValues, pageSize, includeDiagnostics, projections,
                    trdprc1Min, trdprc1Max, trdvol1Min).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error executing getTicksWithPriceVolumeFilters: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get ticks with price volume filters", e);
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
     * Asynchronous implementation of tick data retrieval with range filters.
     *
     * This method orchestrates the entire tick retrieval process with additional range filters:
     * 1. Normalizes the time range (swaps start/end if needed)
     * 2. Builds execution contexts for each RIC across date ranges
     * 3. Executes parallel queries across multiple Cosmos DB containers
     * 4. Aggregates and returns the results
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
     * @param projections List of field names to include in the SELECT clause
     * @param trdprc1Min Minimum value for TRDPRC_1 filter (inclusive)
     * @param trdprc1Max Maximum value for TRDPRC_1 filter (inclusive)
     * @param trnovrUnsMin Minimum value for TRNOVR_UNS filter (inclusive)
     * @param trnovrUnsMax Maximum value for TRNOVR_UNS filter (inclusive)
     * @return CompletableFuture containing the TickResponse
     */
    private CompletableFuture<TickResponse> getTicksWithRangeFiltersAsync(
            List<String> rics,
            List<String> docTypes,
            int totalTicks,
            boolean pinStart,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean includeNullValues,
            int pageSize,
            boolean includeDiagnostics,
            List<String> projections,
            Double trdprc1Min,
            Double trdprc1Max,
            Double trnovrUnsMin,
            Double trnovrUnsMax) {

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
            return executeQueryWithTopNSortedAndRangeFilters(
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
                    projections,
                    trdprc1Min,
                    trdprc1Max,
                    trnovrUnsMin,
                    trnovrUnsMax);
        }, queryExecutorService);
    }

    /**
     * Asynchronous implementation of tick data retrieval with price and volume filters.
     *
     * This method orchestrates the entire tick retrieval process with price and volume filters:
     * 1. Normalizes the time range (swaps start/end if needed)
     * 2. Builds execution contexts for each RIC across date ranges
     * 3. Executes parallel queries across multiple Cosmos DB containers
     * 4. Aggregates and returns the results
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
     * @param projections List of field names to include in the SELECT clause
     * @param trdprc1Min Minimum value for TRDPRC_1 filter (inclusive)
     * @param trdprc1Max Maximum value for TRDPRC_1 filter (inclusive)
     * @param trdvol1Min Minimum value for TRDVOL_1 filter (inclusive)
     * @return CompletableFuture containing the TickResponse
     */
    private CompletableFuture<TickResponse> getTicksWithPriceVolumeFiltersAsync(
            List<String> rics,
            List<String> docTypes,
            int totalTicks,
            boolean pinStart,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean includeNullValues,
            int pageSize,
            boolean includeDiagnostics,
            List<String> projections,
            Double trdprc1Min,
            Double trdprc1Max,
            Double trdvol1Min) {

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
            return executeQueryWithTopNSortedAndPriceVolumeFilters(
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
                    projections,
                    trdprc1Min,
                    trdprc1Max,
                    trdvol1Min);
        }, queryExecutorService);
    }

    /**
     * Asynchronous implementation of tick data retrieval with qualifiers string filters.
     *
     * This method orchestrates the entire tick retrieval process with qualifiers string filters:
     * 1. Normalizes the time range (swaps start/end if needed)
     * 2. Builds execution contexts for each RIC across date ranges
     * 3. Executes parallel queries across multiple Cosmos DB containers
     * 4. Aggregates and returns the results
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
     * @param projections List of field names to include in the SELECT clause
     * @param containsFilters List of strings that must be contained in the qualifiers field
     * @param notContainsFilters List of strings that must NOT be contained in the qualifiers field
     * @param startsWithFilters List of strings that the qualifiers field must start with
     * @param notStartsWithFilters List of strings that the qualifiers field must NOT start with
     * @return CompletableFuture containing the TickResponse
     */
    private CompletableFuture<TickResponse> getTicksWithQualifiersFiltersAsync(
            List<String> rics,
            List<String> docTypes,
            int totalTicks,
            boolean pinStart,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean includeNullValues,
            int pageSize,
            boolean includeDiagnostics,
            List<String> projections,
            List<String> containsFilters,
            List<String> notContainsFilters,
            List<String> startsWithFilters,
            List<String> notStartsWithFilters) {

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
            return executeQueryWithTopNSortedAndQualifiersFilters(
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
                    projections,
                    containsFilters,
                    notContainsFilters,
                    startsWithFilters,
                    notStartsWithFilters);
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
                List<TickRequestContextPerPartitionKey> tickRequestContexts = new ArrayList<>();

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

                for (int i = 1; i <= this.cosmosDbAccountConfiguration.getShardCountPerRic(); i++) {
                    // Create tick identifier for this RIC+date combination
                    String tickIdentifier = TickServiceUtils.constructTickIdentifierPrefix(ric, date) + "|" + i;

                    // Create execution context
                    TickRequestContextPerPartitionKey tickRequestContext = new TickRequestContextPerPartitionKey(
                            asyncContainer,
                            tickIdentifier,
                            date,
                            dateFormat);

                    tickRequestContexts.add(tickRequestContext);
                }

                ricToRicQueryExecutionState.putIfAbsent(ric, new RicQueryExecutionState(new ArrayList<>()));
                List<RicQueryExecutionStateByDate> ricQueryExecutionStateByDates = ricToRicQueryExecutionState.get(ric).getRicQueryExecutionStatesByDate();

                ricQueryExecutionStateByDates.add(new RicQueryExecutionStateByDate(tickRequestContexts, date));
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

            Collections.sort(ticks, (t1, t2) -> t2.getMessageTimestamp().compareTo(t1.getMessageTimestamp()));

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
     * Executes parallel queries across multiple Cosmos DB containers with range filters and aggregates results.
     *
     * This method orchestrates the parallel execution of queries with additional range filters:
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
     * @param projections List of field names to include in the SELECT clause
     * @param trdprc1Min Minimum value for TRDPRC_1 filter (inclusive)
     * @param trdprc1Max Maximum value for TRDPRC_1 filter (inclusive)
     * @param trnovrUnsMin Minimum value for TRNOVR_UNS filter (inclusive)
     * @param trnovrUnsMax Maximum value for TRNOVR_UNS filter (inclusive)
     * @return TickResponse with aggregated results and execution metrics
     */
    private TickResponse executeQueryWithTopNSortedAndRangeFilters(
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
            List<String> projections,
            Double trdprc1Min,
            Double trdprc1Max,
            Double trnovrUnsMin,
            Double trnovrUnsMax) {

        // Record execution start time for performance tracking
        Instant executionStartTime = Instant.now();
        logger.info("Execution of query with range filters and correlationId : {} started at : {}", correlationId, executionStartTime);

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
                                        fetchNextPageWithRangeFilters(ricQueryExecutionState, docTypes, startTime, endTime, 
                                                pageSize, pinStart, totalTicks, projections, trdprc1Min, trdprc1Max, trnovrUnsMin, trnovrUnsMax),
                                queryExecutorService))
                        .collect(Collectors.toList());

                // Wait for all tasks to complete
                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).get();
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Error during query execution with range filters: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to execute queries with range filters", e);
            }
        }

        // Record execution end time and log duration
        Instant executionEndTime = Instant.now();
        logger.info("Execution of query with range filters and correlationId : {} finished in duration : {}", correlationId, Duration.between(executionStartTime, executionEndTime));

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

            Collections.sort(ticks, (t1, t2) -> t2.getMessageTimestamp().compareTo(t1.getMessageTimestamp()));

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
     * Executes parallel queries across multiple Cosmos DB containers with price and volume filters and aggregates results.
     *
     * This method orchestrates the parallel execution of queries with price and volume filters:
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
     * @param projections List of field names to include in the SELECT clause
     * @param trdprc1Min Minimum value for TRDPRC_1 filter (inclusive)
     * @param trdprc1Max Maximum value for TRDPRC_1 filter (inclusive)
     * @param trdvol1Min Minimum value for TRDVOL_1 filter (inclusive)
     * @return TickResponse with aggregated results and execution metrics
     */
    private TickResponse executeQueryWithTopNSortedAndPriceVolumeFilters(
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
            List<String> projections,
            Double trdprc1Min,
            Double trdprc1Max,
            Double trdvol1Min) {

        // Record execution start time for performance tracking
        Instant executionStartTime = Instant.now();
        logger.info("Execution of query with price volume filters and correlationId : {} started at : {}", correlationId, executionStartTime);

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
                                        fetchNextPageWithPriceVolumeFilters(ricQueryExecutionState, docTypes, startTime, endTime, 
                                                pageSize, pinStart, totalTicks, projections, trdprc1Min, trdprc1Max, trdvol1Min),
                                queryExecutorService))
                        .collect(Collectors.toList());

                // Wait for all tasks to complete
                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).get();
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Error during query execution with price volume filters: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to execute queries with price volume filters", e);
            }
        }

        // Record execution end time and log duration
        Instant executionEndTime = Instant.now();
        logger.info("Execution of query with price volume filters and correlationId : {} finished in duration : {}", correlationId, Duration.between(executionStartTime, executionEndTime));

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

            Collections.sort(ticks, (t1, t2) -> t2.getMessageTimestamp().compareTo(t1.getMessageTimestamp()));

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
     * Executes parallel queries across multiple Cosmos DB containers with qualifiers string filters and aggregates results.
     *
     * This method orchestrates the parallel execution of queries with qualifiers string filters:
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
     * @param projections List of field names to include in the SELECT clause
     * @param containsFilters List of strings that must be contained in the qualifiers field
     * @param notContainsFilters List of strings that must NOT be contained in the qualifiers field
     * @param startsWithFilters List of strings that the qualifiers field must start with
     * @param notStartsWithFilters List of strings that the qualifiers field must NOT start with
     * @return TickResponse with aggregated results and execution metrics
     */
    private TickResponse executeQueryWithTopNSortedAndQualifiersFilters(
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
            List<String> projections,
            List<String> containsFilters,
            List<String> notContainsFilters,
            List<String> startsWithFilters,
            List<String> notStartsWithFilters) {

        // Record execution start time for performance tracking
        Instant executionStartTime = Instant.now();
        logger.info("Execution of query with qualifiers filters and correlationId : {} started at : {}", correlationId, executionStartTime);

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
                                        fetchNextPageWithQualifiersFilters(ricQueryExecutionState, docTypes, startTime, endTime, 
                                                pageSize, pinStart, totalTicks, projections, containsFilters, notContainsFilters, 
                                                startsWithFilters, notStartsWithFilters),
                                queryExecutorService))
                        .collect(Collectors.toList());

                // Wait for all tasks to complete
                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).get();
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Error during query execution with qualifiers filters: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to execute queries with qualifiers filters", e);
            }
        }

        // Record execution end time and log duration
        Instant executionEndTime = Instant.now();
        logger.info("Execution of query with qualifiers filters and correlationId : {} finished in duration : {}", correlationId, Duration.between(executionStartTime, executionEndTime));

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

            Collections.sort(ticks, (t1, t2) -> t2.getMessageTimestamp().compareTo(t1.getMessageTimestamp()));

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
     * Fetches a single page of data from a specific Cosmos DB container with range filters.
     *
     * This method:
     * 1. Selects the next available execution context
     * 2. Builds or retrieves the SQL query specification with range filters
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
     * @param projections List of field names to include in the SELECT clause
     * @param trdprc1Min Minimum value for TRDPRC_1 filter (inclusive)
     * @param trdprc1Max Maximum value for TRDPRC_1 filter (inclusive)
     * @param trnovrUnsMin Minimum value for TRNOVR_UNS filter (inclusive)
     * @param trnovrUnsMax Maximum value for TRNOVR_UNS filter (inclusive)
     */
    private void fetchNextPageWithRangeFilters(
            RicQueryExecutionState ricQueryExecutionState,
            List<String> docTypes,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int pageSize,
            boolean pinStart,
            int totalTicks,
            List<String> projections,
            Double trdprc1Min,
            Double trdprc1Max,
            Double trnovrUnsMin,
            Double trnovrUnsMax) {

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

        // Step 3: Build or retrieve SQL query specification with range filters
        SqlQuerySpec querySpec = tickRequestContext.getSqlQuerySpec() != null ?
            tickRequestContext.getSqlQuerySpec() :
            getSqlQuerySpecWithRangeFilters(
                tickRequestContext.getTickIdentifier(),
                docTypes,
                startTime,
                endTime,
                tickRequestContext.getRequestDateAsString(),
                tickRequestContext.getDateFormat(),
                pinStart,
                totalTicks,
                projections,
                trdprc1Min,
                trdprc1Max,
                trnovrUnsMin,
                trnovrUnsMax);

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
                            logger.error("Cosmos exception during page fetch with range filters: {}", cosmosException.getMessage(), cosmosException);

                            // Handle resource not found scenario
                            if (TickServiceUtils.isResourceNotFound(cosmosException)) {
                                logger.warn("Cosmos exception during page fetch with range filters: {}", cosmosException.getMessage());
                                tickRequestContext.setContinuationToken("drained");
                                logger.warn("No tick data found for date: {}", tickRequestContext.getRequestDateAsString());

                                return Mono.empty();
                            }
                        }

                        logger.error("Error during page fetch with range filters: {}", throwable.getMessage(), throwable);
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
            logger.error("Cosmos exception during page fetch with range filters: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Error during page fetch with range filters: {}", e.getMessage(), e);
        }
    }

    /**
     * Fetches a single page of data from a specific Cosmos DB container with price and volume filters.
     *
     * This method:
     * 1. Selects the next available execution context
     * 2. Builds or retrieves the SQL query specification with price and volume filters
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
     * @param projections List of field names to include in the SELECT clause
     * @param trdprc1Min Minimum value for TRDPRC_1 filter (inclusive)
     * @param trdprc1Max Maximum value for TRDPRC_1 filter (inclusive)
     * @param trdvol1Min Minimum value for TRDVOL_1 filter (inclusive)
     */
    private void fetchNextPageWithPriceVolumeFilters(
            RicQueryExecutionState ricQueryExecutionState,
            List<String> docTypes,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int pageSize,
            boolean pinStart,
            int totalTicks,
            List<String> projections,
            Double trdprc1Min,
            Double trdprc1Max,
            Double trdvol1Min) {

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

        // Step 3: Build or retrieve SQL query specification with price and volume filters
        SqlQuerySpec querySpec = tickRequestContext.getSqlQuerySpec() != null ?
            tickRequestContext.getSqlQuerySpec() :
            getSqlQuerySpecWithPriceVolumeFilters(
                tickRequestContext.getTickIdentifier(),
                docTypes,
                startTime,
                endTime,
                tickRequestContext.getRequestDateAsString(),
                tickRequestContext.getDateFormat(),
                pinStart,
                totalTicks,
                projections,
                trdprc1Min,
                trdprc1Max,
                trdvol1Min);

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
                            logger.error("Cosmos exception during page fetch with price volume filters: {}", cosmosException.getMessage(), cosmosException);

                            // Handle resource not found scenario
                            if (TickServiceUtils.isResourceNotFound(cosmosException)) {
                                logger.warn("Cosmos exception during page fetch with price volume filters: {}", cosmosException.getMessage());
                                tickRequestContext.setContinuationToken("drained");
                                logger.warn("No tick data found for date: {}", tickRequestContext.getRequestDateAsString());

                                return Mono.empty();
                            }
                        }

                        logger.error("Error during page fetch with price volume filters: {}", throwable.getMessage(), throwable);
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
            logger.error("Cosmos exception during page fetch with price volume filters: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Error during page fetch with price volume filters: {}", e.getMessage(), e);
        }
    }

    /**
     * Fetches a single page of data from a specific Cosmos DB container with qualifiers string filters.
     *
     * This method:
     * 1. Selects the next available execution context
     * 2. Builds or retrieves the SQL query specification with qualifiers string filters
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
     * @param projections List of field names to include in the SELECT clause
     * @param containsFilters List of strings that must be contained in the qualifiers field
     * @param notContainsFilters List of strings that must NOT be contained in the qualifiers field
     * @param startsWithFilters List of strings that the qualifiers field must start with
     * @param notStartsWithFilters List of strings that the qualifiers field must NOT start with
     */
    private void fetchNextPageWithQualifiersFilters(
            RicQueryExecutionState ricQueryExecutionState,
            List<String> docTypes,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int pageSize,
            boolean pinStart,
            int totalTicks,
            List<String> projections,
            List<String> containsFilters,
            List<String> notContainsFilters,
            List<String> startsWithFilters,
            List<String> notStartsWithFilters) {

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

        // Step 3: Build or retrieve SQL query specification with qualifiers string filters
        SqlQuerySpec querySpec = tickRequestContext.getSqlQuerySpec() != null ?
            tickRequestContext.getSqlQuerySpec() :
            getSqlQuerySpecWithQualifiersFilters(
                tickRequestContext.getTickIdentifier(),
                docTypes,
                startTime,
                endTime,
                tickRequestContext.getRequestDateAsString(),
                tickRequestContext.getDateFormat(),
                pinStart,
                totalTicks,
                projections,
                containsFilters,
                notContainsFilters,
                startsWithFilters,
                notStartsWithFilters);

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
                            logger.error("Cosmos exception during page fetch with qualifiers filters: {}", cosmosException.getMessage(), cosmosException);

                            // Handle resource not found scenario
                            if (TickServiceUtils.isResourceNotFound(cosmosException)) {
                                logger.warn("Cosmos exception during page fetch with qualifiers filters: {}", cosmosException.getMessage());
                                tickRequestContext.setContinuationToken("drained");
                                logger.warn("No tick data found for date: {}", tickRequestContext.getRequestDateAsString());

                                return Mono.empty();
                            }
                        }

                        logger.error("Error during page fetch with qualifiers filters: {}", throwable.getMessage(), throwable);
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
            logger.error("Cosmos exception during page fetch with qualifiers filters: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Error during page fetch with qualifiers filters: {}", e.getMessage(), e);
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
     *
     * Query Structure:
     * SELECT * FROM C
     * WHERE C.pk IN (@pk1, @pk2, ..., @pk8)
     *   AND C.docType IN (@docType0, @docType1, ...)
     *   AND C.messageTimestamp >= @startTime
     *   AND C.messageTimestamp < @endTime
     * ORDER BY C.messageTimestamp [ASC|DESC]
     *
     * @param tickIdentifier Base identifier for the tick (RIC|date)
     * @param docTypes List of document types to filter by
     * @param startTime Start of time range
     * @param endTime End of time range
     * @param localDateAsString Date string for container-specific time bounds
     * @param format Date format pattern
     * @param pinStart If true, sort ascending; if false, sort descending
     * @param totalTicks Maximum number of ticks to return
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
//        StringBuilder partitionKeyPlaceholders = new StringBuilder();
//        partitionKeyPlaceholders.append("(");
//
//        // Create parameters for each shard (partition key)
//        for (int i = 1; i <= this.cosmosDbAccountConfiguration.getShardCountPerRic(); i++) {
//            String param = "@pk" + i;
//            parameters.add(new SqlParameter(param, tickIdentifier + "|" + i));
//            partitionKeyPlaceholders.append(param);
//            if (i <= this.cosmosDbAccountConfiguration.getShardCountPerRic() - 1) {
//                partitionKeyPlaceholders.append(", ");
//            }
//        }
//        partitionKeyPlaceholders.append(")");

        // Build SELECT clause based on projections
        String selectClause = buildSelectClause("C", projections);

        // Build final query with appropriate sorting
        if (pinStart) {
            // Ascending order for pinStart=true
            String query = "SELECT " + selectClause + " FROM C WHERE C.pk = " + tickIdentifier + " AND C.docType IN " + docTypePlaceholders +
                    " AND C.messageTimestamp >= @startTime AND C.messageTimestamp < @endTime ORDER BY C.pk ASC, C.messageTimestamp ASC";

            return new SqlQuerySpec(query, parameters);
        } else {
            String query = "SELECT " + selectClause + " FROM C WHERE C.pk = " + tickIdentifier + " AND C.docType IN " + docTypePlaceholders +
                    " AND C.messageTimestamp >= @startTime AND C.messageTimestamp < @endTime ORDER BY C.pk ASC, C.messageTimestamp DESC";
            return new SqlQuerySpec(query, parameters);
        }
    }

    /**
     * Builds parameterized SQL query specification for Cosmos DB with range filters.
     *
     * This method constructs a parameterized SQL query with:
     * - Partition key filtering across multiple shards
     * - Document type filtering
     * - Time range filtering with nanosecond precision
     * - Range filtering for TRDPRC_1 and TRNOVR_UNS fields
     * - Sorting based on message timestamp
     * - Result limiting
     *
     * Query Structure:
     * SELECT * FROM C
     * WHERE C.pk IN (@pk1, @pk2, ..., @pk8)
     *   AND C.docType IN (@docType0, @docType1, ...)
     *   AND C.messageTimestamp >= @startTime
     *   AND C.messageTimestamp < @endTime
     *   AND C.TRDPRC_1 >= @trdprc1Min AND C.TRDPRC_1 <= @trdprc1Max
     *   AND C.TRNOVR_UNS >= @trnovrUnsMin AND C.TRNOVR_UNS <= @trnovrUnsMax
     * ORDER BY C.messageTimestamp [ASC|DESC]
     *
     * @param tickIdentifier Base identifier for the tick (RIC|date)
     * @param docTypes List of document types to filter by
     * @param startTime Start of time range
     * @param endTime End of time range
     * @param localDateAsString Date string for container-specific time bounds
     * @param format Date format pattern
     * @param pinStart If true, sort ascending; if false, sort descending
     * @param totalTicks Maximum number of ticks to return
     * @param projections List of field names to include in the SELECT clause
     * @param trdprc1Min Minimum value for TRDPRC_1 filter (inclusive)
     * @param trdprc1Max Maximum value for TRDPRC_1 filter (inclusive)
     * @param trnovrUnsMin Minimum value for TRNOVR_UNS filter (inclusive)
     * @param trnovrUnsMax Maximum value for TRNOVR_UNS filter (inclusive)
     * @return SqlQuerySpec with parameterized query and parameters
     */
    private SqlQuerySpec getSqlQuerySpecWithRangeFilters(
            String tickIdentifier,
            List<String> docTypes,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String localDateAsString,
            String format,
            boolean pinStart,
            int totalTicks,
            List<String> projections,
            Double trdprc1Min,
            Double trdprc1Max,
            Double trnovrUnsMin,
            Double trnovrUnsMax) {

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

        // Build range filter conditions
        StringBuilder rangeFilters = new StringBuilder();
        
        // Add TRDPRC_1 range filters if specified
        if (trdprc1Min != null) {
            parameters.add(new SqlParameter("@trdprc1Min", trdprc1Min));
            rangeFilters.append(" AND C.TRDPRC_1 >= @trdprc1Min");
        }
        if (trdprc1Max != null) {
            parameters.add(new SqlParameter("@trdprc1Max", trdprc1Max));
            rangeFilters.append(" AND C.TRDPRC_1 <= @trdprc1Max");
        }
        
        // Add TRNOVR_UNS range filters if specified
        if (trnovrUnsMin != null) {
            parameters.add(new SqlParameter("@trnovrUnsMin", trnovrUnsMin));
            rangeFilters.append(" AND C.TRNOVR_UNS >= @trnovrUnsMin");
        }
        if (trnovrUnsMax != null) {
            parameters.add(new SqlParameter("@trnovrUnsMax", trnovrUnsMax));
            rangeFilters.append(" AND C.TRNOVR_UNS <= @trnovrUnsMax");
        }

        // Build SELECT clause based on projections
        String selectClause = buildSelectClause("C", projections);

        // Build final query with appropriate sorting
        if (pinStart) {
            // Ascending order for pinStart=true
            String query = "SELECT " + selectClause + " FROM C WHERE C.pk IN " + partitionKeyPlaceholders + 
                    " AND C.docType IN " + docTypePlaceholders +
                    " AND C.messageTimestamp >= @startTime AND C.messageTimestamp < @endTime" +
                    rangeFilters +
                    " ORDER BY C.pk ASC, C.messageTimestamp ASC";

            return new SqlQuerySpec(query, parameters);
        } else {
            String query = "SELECT " + selectClause + " FROM C WHERE C.pk IN " + partitionKeyPlaceholders + 
                    " AND C.docType IN " + docTypePlaceholders +
                    " AND C.messageTimestamp >= @startTime AND C.messageTimestamp < @endTime" +
                    rangeFilters +
                    " ORDER BY C.pk ASC, C.messageTimestamp DESC";
            return new SqlQuerySpec(query, parameters);
        }
    }

    /**
     * Builds parameterized SQL query specification for Cosmos DB with price and volume filters.
     *
     * This method constructs a parameterized SQL query with:
     * - Partition key filtering across multiple shards
     * - Document type filtering
     * - Time range filtering with nanosecond precision
     * - Range filtering for TRDPRC_1 (min/max) and TRDVOL_1 (min only) fields
     * - Sorting based on message timestamp
     * - Result limiting
     *
     * Query Structure:
     * SELECT * FROM C
     * WHERE C.pk IN (@pk1, @pk2, ..., @pk8)
     *   AND C.docType IN (@docType0, @docType1, ...)
     *   AND C.messageTimestamp >= @startTime
     *   AND C.messageTimestamp < @endTime
     *   AND C.TRDPRC_1 >= @trdprc1Min AND C.TRDPRC_1 <= @trdprc1Max
     *   AND C.TRDVOL_1 >= @trdvol1Min
     * ORDER BY C.messageTimestamp [ASC|DESC]
     *
     * @param tickIdentifier Base identifier for the tick (RIC|date)
     * @param docTypes List of document types to filter by
     * @param startTime Start of time range
     * @param endTime End of time range
     * @param localDateAsString Date string for container-specific time bounds
     * @param format Date format pattern
     * @param pinStart If true, sort ascending; if false, sort descending
     * @param totalTicks Maximum number of ticks to return
     * @param projections List of field names to include in the SELECT clause
     * @param trdprc1Min Minimum value for TRDPRC_1 filter (inclusive)
     * @param trdprc1Max Maximum value for TRDPRC_1 filter (inclusive)
     * @param trdvol1Min Minimum value for TRDVOL_1 filter (inclusive)
     * @return SqlQuerySpec with parameterized query and parameters
     */
    private SqlQuerySpec getSqlQuerySpecWithPriceVolumeFilters(
            String tickIdentifier,
            List<String> docTypes,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String localDateAsString,
            String format,
            boolean pinStart,
            int totalTicks,
            List<String> projections,
            Double trdprc1Min,
            Double trdprc1Max,
            Double trdvol1Min) {

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

        // Build range filter conditions
        StringBuilder rangeFilters = new StringBuilder();
        
        // Add TRDPRC_1 range filters if specified
        if (trdprc1Min != null) {
            parameters.add(new SqlParameter("@trdprc1Min", trdprc1Min));
            rangeFilters.append(" AND C.TRDPRC_1 >= @trdprc1Min");
        }
        if (trdprc1Max != null) {
            parameters.add(new SqlParameter("@trdprc1Max", trdprc1Max));
            rangeFilters.append(" AND C.TRDPRC_1 <= @trdprc1Max");
        }
        
        // Add TRDVOL_1 range filter if specified
        if (trdvol1Min != null) {
            parameters.add(new SqlParameter("@trdvol1Min", trdvol1Min));
            rangeFilters.append(" AND C.TRDVOL_1 >= @trdvol1Min");
        }

        // Build SELECT clause based on projections
        String selectClause = buildSelectClause("C", projections);

        // Build final query with appropriate sorting
        if (pinStart) {
            // Ascending order for pinStart=true
            String query = "SELECT " + selectClause + " FROM C WHERE C.pk IN " + partitionKeyPlaceholders + 
                    " AND C.docType IN " + docTypePlaceholders +
                    " AND C.messageTimestamp >= @startTime AND C.messageTimestamp < @endTime" +
                    rangeFilters +
                    " ORDER BY C.pk ASC, C.messageTimestamp ASC";

            return new SqlQuerySpec(query, parameters);
        } else {
            String query = "SELECT " + selectClause + " FROM C WHERE C.pk IN " + partitionKeyPlaceholders + 
                    " AND C.docType IN " + docTypePlaceholders +
                    " AND C.messageTimestamp >= @startTime AND C.messageTimestamp < @endTime" +
                    rangeFilters +
                    " ORDER BY C.pk ASC, C.messageTimestamp DESC";
            return new SqlQuerySpec(query, parameters);
        }
    }

    /**
     * Builds parameterized SQL query specification for Cosmos DB with qualifiers string filters.
     *
     * This method constructs a parameterized SQL query with:
     * - Partition key filtering across multiple shards
     * - Document type filtering
     * - Time range filtering with nanosecond precision
     * - String filters using CONTAINS, NOT CONTAINS, STARTSWITH, and NOT STARTSWITH on qualifiers field
     * - Sorting based on message timestamp
     * - Result limiting
     *
     * Query Structure:
     * SELECT * FROM C
     * WHERE C.pk IN (@pk1, @pk2, ..., @pk8)
     *   AND C.docType IN (@docType0, @docType1, ...)
     *   AND C.messageTimestamp >= @startTime
     *   AND C.messageTimestamp < @endTime
     *   AND CONTAINS(C.qualifiers, @contains0) AND CONTAINS(C.qualifiers, @contains1) ...
     *   AND NOT CONTAINS(C.qualifiers, @notContains0) AND NOT CONTAINS(C.qualifiers, @notContains1) ...
     *   AND STARTSWITH(C.qualifiers, @startsWith0) AND STARTSWITH(C.qualifiers, @startsWith1) ...
     *   AND NOT STARTSWITH(C.qualifiers, @notStartsWith0) AND NOT STARTSWITH(C.qualifiers, @notStartsWith1) ...
     * ORDER BY C.messageTimestamp [ASC|DESC]
     *
     * @param tickIdentifier Base identifier for the tick (RIC|date)
     * @param docTypes List of document types to filter by
     * @param startTime Start of time range
     * @param endTime End of time range
     * @param localDateAsString Date string for container-specific time bounds
     * @param format Date format pattern
     * @param pinStart If true, sort ascending; if false, sort descending
     * @param totalTicks Maximum number of ticks to return
     * @param projections List of field names to include in the SELECT clause
     * @param containsFilters List of strings that must be contained in the qualifiers field
     * @param notContainsFilters List of strings that must NOT be contained in the qualifiers field
     * @param startsWithFilters List of strings that the qualifiers field must start with
     * @param notStartsWithFilters List of strings that the qualifiers field must NOT start with
     * @return SqlQuerySpec with parameterized query and parameters
     */
    private SqlQuerySpec getSqlQuerySpecWithQualifiersFilters(
            String tickIdentifier,
            List<String> docTypes,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String localDateAsString,
            String format,
            boolean pinStart,
            int totalTicks,
            List<String> projections,
            List<String> containsFilters,
            List<String> notContainsFilters,
            List<String> startsWithFilters,
            List<String> notStartsWithFilters) {

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

        // Build string filter conditions
        StringBuilder stringFilters = new StringBuilder();
        
        // Add CONTAINS filters
        if (containsFilters != null && !containsFilters.isEmpty()) {
            for (int i = 0; i < containsFilters.size(); i++) {
                String param = "@contains" + i;
                parameters.add(new SqlParameter(param, containsFilters.get(i)));
                stringFilters.append(" AND CONTAINS(C.qualifiers, ").append(param).append(")");
            }
        }
        
        // Add NOT CONTAINS filters
        if (notContainsFilters != null && !notContainsFilters.isEmpty()) {
            for (int i = 0; i < notContainsFilters.size(); i++) {
                String param = "@notContains" + i;
                parameters.add(new SqlParameter(param, notContainsFilters.get(i)));
                stringFilters.append(" AND NOT CONTAINS(C.qualifiers, ").append(param).append(")");
            }
        }
        
        // Add STARTSWITH filters
        if (startsWithFilters != null && !startsWithFilters.isEmpty()) {
            for (int i = 0; i < startsWithFilters.size(); i++) {
                String param = "@startsWith" + i;
                parameters.add(new SqlParameter(param, startsWithFilters.get(i)));
                stringFilters.append(" AND STARTSWITH(C.qualifiers, ").append(param).append(")");
            }
        }
        
        // Add NOT STARTSWITH filters
        if (notStartsWithFilters != null && !notStartsWithFilters.isEmpty()) {
            for (int i = 0; i < notStartsWithFilters.size(); i++) {
                String param = "@notStartsWith" + i;
                parameters.add(new SqlParameter(param, notStartsWithFilters.get(i)));
                stringFilters.append(" AND NOT STARTSWITH(C.qualifiers, ").append(param).append(")");
            }
        }

        // Build SELECT clause based on projections
        String selectClause = buildSelectClause("C", projections);

        // Build final query with appropriate sorting
        if (pinStart) {
            // Ascending order for pinStart=true
            String query = "SELECT " + selectClause + " FROM C WHERE C.pk IN " + partitionKeyPlaceholders + 
                    " AND C.docType IN " + docTypePlaceholders +
                    " AND C.messageTimestamp >= @startTime AND C.messageTimestamp < @endTime" +
                    stringFilters +
                    " ORDER BY C.pk ASC, C.messageTimestamp ASC";

            return new SqlQuerySpec(query, parameters);
        } else {
            String query = "SELECT " + selectClause + " FROM C WHERE C.pk IN " + partitionKeyPlaceholders + 
                    " AND C.docType IN " + docTypePlaceholders +
                    " AND C.messageTimestamp >= @startTime AND C.messageTimestamp < @endTime" +
                    stringFilters +
                    " ORDER BY C.pk ASC, C.messageTimestamp DESC";
            return new SqlQuerySpec(query, parameters);
        }
    }

    /**
     * Builds the SELECT clause for the SQL query based on projections.
     *
     * If projections is null or empty, returns "*" for SELECT *.
     * Otherwise, builds a comma-separated list of field names, always including
     * the required core fields: id, ricName, messageTimestamp, pk.
     *
     * @param projections List of field names to include in the SELECT clause
     * @return String representing the SELECT clause
     */
    private String buildSelectClause(String containerAlias, List<String> projections) {
        if (projections == null || projections.isEmpty()) {
            return "*";
        }

        // Validate projections to prevent SQL injection
        List<String> validatedProjections = projections.stream()
                .filter(projection -> projection != null && !projection.trim().isEmpty())
                .map(projection -> projection.trim())
                .filter(projection -> isValidFieldName(projection))
                .map(projection -> containerAlias + "." + projection)
                .collect(Collectors.toList());

        if (validatedProjections.isEmpty()) {
            return "*";
        }

        // Always include required core fields
        Set<String> requiredFields = new HashSet<>();

        requiredFields.add(containerAlias + "." + "id");
        requiredFields.add(containerAlias + "." + "ricName");
        requiredFields.add(containerAlias + "." + "messageTimestamp");
        requiredFields.add(containerAlias + "." + "pk");

        Set<String> allFields = new LinkedHashSet<>(requiredFields);
        allFields.addAll(validatedProjections);

        return String.join(", ", allFields);
    }

    /**
     * Validates if a field name is safe for use in SQL queries.
     *
     * This method performs basic validation to prevent SQL injection:
     * - Checks for null or empty strings
     * - Ensures the field name contains only alphanumeric characters, underscores, and dots
     * - Prevents common SQL injection patterns
     *
     * @param fieldName The field name to validate
     * @return true if the field name is valid, false otherwise
     */
    private boolean isValidFieldName(String fieldName) {
        if (fieldName == null || fieldName.trim().isEmpty()) {
            return false;
        }

        // Check for SQL injection patterns
        String lowerFieldName = fieldName.toLowerCase();
        if (lowerFieldName.contains("select") || 
            lowerFieldName.contains("from") || 
            lowerFieldName.contains("where") || 
            lowerFieldName.contains("order") || 
            lowerFieldName.contains("group") || 
            lowerFieldName.contains("union") || 
            lowerFieldName.contains("insert") || 
            lowerFieldName.contains("update") || 
            lowerFieldName.contains("delete") || 
            lowerFieldName.contains("drop") || 
            lowerFieldName.contains("create") || 
            lowerFieldName.contains("alter") ||
            lowerFieldName.contains(";") ||
            lowerFieldName.contains("--") ||
            lowerFieldName.contains("/*") ||
            lowerFieldName.contains("*/")) {
            return false;
        }

        // Allow alphanumeric characters, underscores, and dots (for nested properties)
        return fieldName.matches("^[a-zA-Z0-9_.]+$");
    }

    /**
     * Retrieves tick data for specified RICs within a time range with qualifiers string filters.
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
     * @param containsFilters List of strings that must be contained in the qualifiers field
     * @param notContainsFilters List of strings that must NOT be contained in the qualifiers field
     * @param startsWithFilters List of strings that the qualifiers field must start with
     * @param notStartsWithFilters List of strings that the qualifiers field must NOT start with
     * @return TickResponse containing the retrieved tick data and execution metrics
     * @throws RuntimeException if the query execution fails
     */
    @Override
    public TickResponse getTicksWithQualifiersFilters(
            List<String> rics,
            List<String> docTypes,
            int totalTicks,
            boolean pinStart,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean includeNullValues,
            int pageSize,
            boolean includeDiagnostics,
            List<String> projections,
            List<String> containsFilters,
            List<String> notContainsFilters,
            List<String> startsWithFilters,
            List<String> notStartsWithFilters) {
        
        try {
            // Execute the asynchronous query and wait for completion
            return getTicksWithQualifiersFiltersAsync(rics, docTypes, totalTicks, pinStart, startTime, endTime, 
                    includeNullValues, pageSize, includeDiagnostics, projections,
                    containsFilters, notContainsFilters, startsWithFilters, notStartsWithFilters).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error executing getTicksWithQualifiersFilters: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get ticks with qualifiers filters", e);
        }
    }
}