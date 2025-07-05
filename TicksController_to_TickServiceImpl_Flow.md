# TicksController to TickServiceImpl Code Flow Documentation

## Overview
This document outlines the complete code flow from the REST API endpoint in `TicksController` through the service layer implementation in `TickServiceImpl`, including data models, key components, and execution patterns.

## 1. Entry Point: TicksController

### REST Endpoint
```java
@GetMapping("/sort=messageTimestamp")
public TickResponse getTicks(
    @RequestParam List<String> rics,
    @RequestParam List<String> docTypes,
    @RequestParam int totalTicks,
    @RequestParam boolean pinStart,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
    @RequestParam(required = false, defaultValue = "false") boolean includeNullValues,
    @RequestParam(required = false, defaultValue = "100") int pageSize,
    @RequestParam(required = false, defaultValue = "false") boolean includeDiagnostics)
```

**Key Parameters:**
- `rics`: List of RIC (Reuters Instrument Code) identifiers
- `docTypes`: Document types to filter by
- `totalTicks`: Maximum number of ticks to return
- `pinStart`: Boolean flag for sorting order
- `startTime/endTime`: Time range for data retrieval
- `includeNullValues`: Whether to include null values in response
- `pageSize`: Number of items per page for pagination
- `includeDiagnostics`: Whether to include Cosmos DB diagnostics

### Controller Flow
1. **Parameter Validation**: Spring Boot automatically validates and converts request parameters
2. **Service Call**: Delegates to `TicksService.getTicks()` method
3. **Logging**: Debug logging of request parameters and response size
4. **Response**: Returns `TickResponse` object

## 2. Service Interface: TicksService

### Interface Definition
```java
public interface TicksService {
    TickResponse getTicks(
        List<String> rics,
        List<String> docTypes,
        int totalTicks,
        boolean pinStart,
        LocalDateTime startTime,
        LocalDateTime endTime,
        boolean includeNullValues,
        int pageSize,
        boolean includeDiagnostics);
}
```

**Purpose**: Defines the contract for tick data retrieval operations.

## 3. Service Implementation: TickServiceImpl

### Class Configuration
```java
@Component
@ConditionalOnProperty(name = "ticks.implementation", havingValue = "completeablefuture")
public class TickServiceImpl implements TicksService
```

**Key Dependencies:**
- `RicBasedCosmosClientFactory`: Factory for creating Cosmos DB clients
- `CosmosDbAccountConfiguration`: Configuration for Cosmos DB accounts
- `ExecutorService`: Thread pool for concurrent operations
- `ObjectMapper`: For JSON serialization/deserialization

### Main Flow: getTicks() Method

#### Step 1: Synchronous Wrapper
```java
public TickResponse getTicks(...) {
    try {
        return getTicksAsync(...).get();
    } catch (InterruptedException | ExecutionException e) {
        logger.error("Error executing getTicks: {}", e.getMessage(), e);
        throw new RuntimeException("Failed to get ticks", e);
    }
}
```

**Purpose**: Wraps the asynchronous implementation in a synchronous interface.

#### Step 2: Asynchronous Execution
```java
private CompletableFuture<TickResponse> getTicksAsync(...) {
    return CompletableFuture.supplyAsync(() -> {
        // Normalize time range
        LocalDateTime newStartTime = startTime.isAfter(endTime) ? endTime : startTime;
        LocalDateTime newEndTime = endTime.isBefore(startTime) ? startTime : endTime;
        
        // Build execution contexts
        Map<String, RicQueryExecutionState> ricToRicQueryExecutionState = 
            buildTickRequestContexts(rics, newStartTime, newEndTime, pinStart);
        
        String correlationId = UUID.randomUUID().toString();
        
        // Execute queries and return response
        return executeQueryWithTopNSorted(...);
    }, queryExecutorService);
}
```

### Step 3: Context Building: buildTickRequestContexts()

#### Purpose
Creates execution contexts for each RIC across multiple date ranges and Cosmos DB containers.

#### Key Steps:
1. **RIC Hashing**: Uses Murmur3 hash to determine Cosmos DB account
2. **Date Range Processing**: Generates list of dates between start and end times
3. **Container Mapping**: Maps each RIC+date combination to a Cosmos DB container
4. **Context Creation**: Creates `TickRequestContextPerPartitionKey` objects

#### Code Flow:
```java
private Map<String, RicQueryExecutionState> buildTickRequestContexts(...) {
    Map<String, RicQueryExecutionState> ricToRicQueryExecutionState = new HashMap<>();
    
    for (String ric : rics) {
        // 1. Calculate hash for RIC to determine Cosmos DB account
        int hash = Murmur3_x86_32.hashUnsafeBytes(...);
        int hashIdForRic = Math.abs(hash) % this.cosmosDbAccountConfiguration.getAccountCount() + 1;
        
        // 2. Get Cosmos DB account configuration
        CosmosDbAccount cosmosDbAccount = this.cosmosDbAccountConfiguration.getCosmosDbAccount(hashIdForRic);
        
        // 3. Generate date range
        List<String> datesInBetween = TickServiceUtils.getLocalDatesBetweenTwoLocalDateTimes(...);
        
        // 4. Create contexts for each date
        for (String date : datesInBetween) {
            CosmosAsyncContainer asyncContainer = // ... get container
            TickRequestContextPerPartitionKey tickRequestContext = new TickRequestContextPerPartitionKey(...);
            tickRequestContexts.add(tickRequestContext);
        }
        
        // 5. Create execution state for RIC
        ricToRicQueryExecutionState.put(ric, new RicQueryExecutionState(tickRequestContexts));
    }
    
    return ricToRicQueryExecutionState;
}
```

### Step 4: Query Execution: executeQueryWithTopNSorted()

#### Purpose
Executes parallel queries across multiple Cosmos DB containers and aggregates results.

#### Key Steps:
1. **Parallel Execution**: Creates concurrent tasks for each RIC
2. **Page Fetching**: Fetches data pages until completion
3. **Result Aggregation**: Combines results from all RICs
4. **Response Building**: Creates final `TickResponse` object

#### Code Flow:
```java
private TickResponse executeQueryWithTopNSorted(...) {
    Instant executionStartTime = Instant.now();
    List<Tick> resultTicks = new ArrayList<>();
    
    // 1. Execute queries in parallel until all are completed
    while (!ricToRicQueryExecutionState.values().stream().allMatch(RicQueryExecutionState::isCompleted)) {
        List<CompletableFuture<Void>> tasks = ricToRicQueryExecutionState.values().stream()
            .map(ricQueryExecutionState -> CompletableFuture.runAsync(() ->
                fetchNextPage(ricQueryExecutionState, docTypes, startTime, endTime, pageSize, pinStart, totalTicks),
                queryExecutorService))
            .collect(Collectors.toList());
        
        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).get();
    }
    
    // 2. Aggregate results from all RICs
    for (String ric : rics) {
        RicQueryExecutionState ricQueryExecutionState = ricToRicQueryExecutionState.get(ric);
        List<Tick> ticks = ricQueryExecutionState.getTicks();
        
        // Handle sorting based on pinStart flag
        if (pinStart) {
            Collections.reverse(ticks);
        }
        
        resultTicks.addAll(ticks);
    }
    
    // 3. Convert to final response format
    List<BaseTick> finalTicks = new ArrayList<>();
    if (includeNullValues) {
        finalTicks.addAll(resultTicks);
    } else {
        List<TickWithNoNulls> newTicks = resultTicks.stream()
            .map(tick -> nonNullObjectMapper.convertValue(tick, TickWithNoNulls.class))
            .collect(Collectors.toList());
        finalTicks.addAll(newTicks);
    }
    
    // 4. Return response with execution metrics
    return new TickResponse(finalTicks, diagnosticsList, Duration.between(executionStartTime, executionEndTime));
}
```

### Step 5: Page Fetching: fetchNextPage()

#### Purpose
Fetches a single page of data from a specific Cosmos DB container.

#### Key Steps:
1. **Context Selection**: Selects next available context for execution
2. **Query Building**: Constructs SQL query with parameters
3. **Database Query**: Executes query against Cosmos DB
4. **Result Processing**: Processes response and updates execution state

#### Code Flow:
```java
private void fetchNextPage(...) {
    // 1. Get next available context
    TickRequestContextPerPartitionKey tickRequestContext = 
        TickServiceUtils.evaluateTickRequestContextToExecute(ricQueryExecutionState);
    
    if (tickRequestContext == null) {
        ricQueryExecutionState.setCompleted(true);
        return;
    }
    
    // 2. Build or retrieve SQL query
    SqlQuerySpec querySpec = tickRequestContext.getSqlQuerySpec() != null ? 
        tickRequestContext.getSqlQuerySpec() : 
        getSqlQuerySpec(...);
    
    // 3. Execute query with pagination
    FeedResponse<Tick> response = asyncContainer.queryItems(querySpec, queryRequestOptions, Tick.class)
        .byPage(continuationToken, pageSize)
        .next()
        .onErrorResume(throwable -> {
            // Handle Cosmos DB exceptions
            if (throwable instanceof CosmosException) {
                // Handle resource not found, etc.
            }
            return Mono.error(throwable);
        })
        .block();
    
    // 4. Process response
    if (response != null) {
        tickRequestContext.setContinuationToken(response.getContinuationToken());
        tickRequestContext.addCosmosDiagnostics(response.getCosmosDiagnostics());
        ricQueryExecutionState.addTicks(response.getResults(), totalTicks);
    }
}
```

### Step 6: Query Specification: getSqlQuerySpec()

#### Purpose
Builds parameterized SQL queries for Cosmos DB with proper filtering and sorting.

#### Query Structure:
```sql
SELECT * FROM C 
WHERE C.pk IN (@pk1, @pk2, ..., @pk8) 
  AND C.docType IN (@docType0, @docType1, ...) 
  AND C.messageTimestamp >= @startTime 
  AND C.messageTimestamp < @endTime 
ORDER BY C.messageTimestamp [ASC|DESC] 
OFFSET 0 LIMIT @totalTicks
```

#### Key Features:
- **Partition Key Filtering**: Queries across multiple shards per RIC
- **Document Type Filtering**: Filters by specified document types
- **Time Range Filtering**: Filters by message timestamp
- **Sorting**: ASC for pinStart=true, DESC for pinStart=false
- **Limiting**: Limits results to requested number of ticks

## 4. Data Models

### TickResponse
```java
public class TickResponse {
    private final List<BaseTick> ticks;
    private final List<String> diagnosticsList;
    private final Duration executionTime;
}
```

### RicQueryExecutionState
```java
public class RicQueryExecutionState {
    private List<Tick> ticks;
    private List<TickRequestContextPerPartitionKey> tickRequestContexts;
    private boolean isCompleted;
}
```

### TickRequestContextPerPartitionKey
```java
public class TickRequestContextPerPartitionKey {
    private final CosmosAsyncContainer asyncContainer;
    private final String tickIdentifier;
    private final AtomicReference<String> continuationToken;
    private final AtomicReference<SqlQuerySpec> sqlQuerySpec;
    private final CopyOnWriteArrayList<CosmosDiagnostics> cosmosDiagnosticsList;
}
```

## 5. Key Design Patterns

### 1. Asynchronous Processing
- Uses `CompletableFuture` for non-blocking operations
- Parallel execution across multiple RICs and containers
- Thread pool management for controlled concurrency

### 2. Pagination Strategy
- Continuation token-based pagination
- Automatic page fetching until completion
- State management across multiple requests

### 3. Error Handling
- Graceful handling of Cosmos DB exceptions
- Resource not found scenarios
- Comprehensive logging and diagnostics

### 4. Data Partitioning
- RIC-based partitioning across Cosmos DB accounts
- Date-based container partitioning
- Shard-based partition key distribution

### 5. Response Optimization
- Optional null value filtering
- Configurable diagnostics inclusion
- Execution time tracking

## 6. Performance Considerations

### Concurrency
- Thread pool size: `Configs.getCPUCnt() * 10`
- Parallel execution across RICs and containers
- Non-blocking I/O operations

### Query Optimization
- Partition key-based queries
- Indexed timestamp filtering
- Result limiting at database level

### Memory Management
- Streaming pagination
- Controlled result aggregation
- Efficient data structure usage

## 7. Configuration

### Conditional Implementation
```java
@ConditionalOnProperty(name = "ticks.implementation", havingValue = "completeablefuture")
```

### Cosmos DB Configuration
- Multiple account support
- Container naming conventions
- Partition key strategies

### Thread Pool Configuration
- CPU-based sizing
- Fixed thread pool for query execution
- Controlled resource utilization

## 8. Monitoring and Diagnostics

### Logging
- Request correlation IDs
- Execution time tracking
- Error condition logging

### Cosmos DB Diagnostics
- Optional diagnostics collection
- Performance metrics
- Query execution details

### Response Metrics
- Execution duration
- Result count
- Diagnostics information

This flow demonstrates a sophisticated, production-ready implementation for high-performance tick data retrieval from Cosmos DB with proper concurrency, error handling, and monitoring capabilities. 