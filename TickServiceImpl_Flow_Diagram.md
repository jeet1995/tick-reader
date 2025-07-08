# TickServiceImpl Code Flow Diagram

## Overview
This diagram illustrates the complete code flow in `TickServiceImpl`, showing the asynchronous processing, parallel execution, and data retrieval patterns.

## Main Flow Diagram

```mermaid
flowchart TD
    A[TicksController.getTicks] --> B[TickServiceImpl.getTicks]
    B --> C{Synchronous Wrapper}
    C --> D[getTicksAsync]
    
    D --> E[CompletableFuture.supplyAsync]
    E --> F[Normalize Time Range]
    F --> G[buildTickRequestContexts]
    
    G --> H[For each RIC]
    H --> I[Calculate Murmur3 Hash]
    I --> J[Determine Cosmos DB Account]
    J --> K[Generate Date Range]
    K --> L[For each Date]
    L --> M[Get CosmosAsyncClient]
    M --> N[Get CosmosAsyncContainer]
    N --> O[Create TickRequestContextPerPartitionKey]
    O --> P[Add to tickRequestContexts]
    P --> Q{More Dates?}
    Q -->|Yes| L
    Q -->|No| R[Create RicQueryExecutionState]
    R --> S{More RICs?}
    S -->|Yes| H
    S -->|No| T[executeQueryWithTopNSorted]
    
    T --> U[Record Start Time]
    U --> V[Create Diagnostics List]
    V --> W[While Not All Completed]
    W --> X[Create Parallel Tasks]
    X --> Y[For each RicQueryExecutionState]
    Y --> Z[fetchNextPage]
    Z --> AA[Wait for All Tasks]
    AA --> BB{All Completed?}
    BB -->|No| W
    BB -->|Yes| CC[Record End Time]
    
    CC --> DD[Aggregate Results]
    DD --> EE[For each RIC]
    EE --> FF[Get RicQueryExecutionState]
    FF --> GG[Get Ticks]
    GG --> HH[Collect Diagnostics]
    HH --> II[Apply Sorting if pinStart]
    II --> JJ[Add to resultTicks]
    JJ --> KK{More RICs?}
    KK -->|Yes| EE
    KK -->|No| LL[Convert Response Format]
    
    LL --> MM{includeNullValues?}
    MM -->|Yes| NN[Use Original Ticks]
    MM -->|No| OO[Convert to TickWithNoNulls]
    OO --> PP[Create TickResponse]
    NN --> PP
    PP --> QQ[Return Response]
    
    style A fill:#e1f5fe
    style B fill:#e1f5fe
    style T fill:#fff3e0
    style Z fill:#fff3e0
    style PP fill:#e8f5e8
    style QQ fill:#e8f5e8
```

## Page Fetching Flow

```mermaid
flowchart TD
    A[fetchNextPage] --> B[Get Next Context]
    B --> C{Context Available?}
    C -->|No| D[Mark Completed]
    C -->|Yes| E[Get AsyncContainer]
    
    E --> F[Get/Create SqlQuerySpec]
    F --> G[Check Continuation Token]
    G --> H{Token - drained?}
    H -->|Yes| I[Return]
    H -->|No| J[Execute Query]
    
    J --> K[asyncContainer.queryItems]
    K --> L[byPage with continuationToken]
    L --> M[onErrorResume]
    M --> N{Exception Type?}
    N -->|CosmosException| O[Handle Cosmos Exception]
    N -->|Other| P[Return Mono.error]
    
    O --> Q{Resource Not Found?}
    Q -->|Yes| R[Set Token to drained]
    Q -->|No| S[Log Error]
    R --> T[Return Mono.empty]
    S --> P
    
    K --> U[Process Response]
    U --> V{Response Null?}
    V -->|Yes| W[Log Error]
    V -->|No| X[Update Continuation Token]
    
    X --> Y[Add Cosmos Diagnostics]
    Y --> Z[Add Ticks to Execution State]
    Z --> AA[Return]
    
    style A fill:#fff3e0
    style J fill:#fff3e0
    style Z fill:#e8f5e8
```

## Context Building Flow

```mermaid
flowchart TD
    A[buildTickRequestContexts] --> B[Create Result Map]
    B --> C[For each RIC]
    C --> D[Create Context List]
    D --> E[Calculate Murmur3 Hash]
    E --> F[Get Hash ID for RIC]
    F --> G[Get Cosmos DB Account]
    G --> H[Get Date Format]
    H --> I[Generate Date Range]
    
    I --> J[For each Date]
    J --> K[Get CosmosAsyncClient]
    K --> L{Client Found?}
    L -->|No| M[Log Warning]
    L -->|Yes| N[Get Database Name]
    
    M --> O{More Dates?}
    N --> P{Database Valid?}
    P -->|No| Q[Log Warning]
    P -->|Yes| R[Get CosmosAsyncContainer]
    
    Q --> O
    R --> S[Create Tick Identifier]
    S --> T[Create TickRequestContextPerPartitionKey]
    T --> U[Add to Context List]
    U --> O
    
    O -->|Yes| J
    O -->|No| V{Contexts Created?}
    V -->|Yes| W[Create RicQueryExecutionState]
    V -->|No| X[Log Warning]
    
    W --> Y[Add to Result Map]
    X --> Z{More RICs?}
    Y --> Z
    Z -->|Yes| C
    Z -->|No| AA[Return Result Map]
    
    style A fill:#fff3e0
    style E fill:#fff3e0
    style R fill:#fff3e0
    style AA fill:#e8f5e8
```

## SQL Query Building Flow

```mermaid
flowchart TD
    A[getSqlQuerySpec] --> B[Parse Date String]
    B --> C[Calculate Query Time Bounds]
    C --> D[Initialize Parameters List]
    D --> E[Add Time Parameters]
    E --> F[Build Document Type Parameters]
    
    F --> G[For each DocType]
    G --> H[Create Parameter]
    H --> I[Add to Parameters]
    I --> J[Add to Placeholders]
    J --> K{More DocTypes?}
    K -->|Yes| G
    K -->|No| L[Build Partition Key Parameters]
    
    L --> M[For each Shard]
    M --> N[Create Partition Key Parameter]
    N --> O[Add to Parameters]
    O --> P[Add to Placeholders]
    P --> Q{More Shards?}
    Q -->|Yes| M
    Q -->|No| R{pinStart?}
    
    R -->|True| S[Build ASC Query]
    R -->|False| T[Build DESC Query]
    S --> U[Return SqlQuerySpec]
    T --> U
    
    style A fill:#fff3e0
    style C fill:#fff3e0
    style U fill:#e8f5e8
```

## Data Model Architecture

```mermaid
graph TB
    subgraph "Data Model"
        A[RIC: AAPL] --> B[Cosmos DB Account 1]
        C[RIC: MSFT] --> D[Cosmos DB Account 2]
        E[RIC: GOOGL] --> F[Cosmos DB Account 3]
        
        B --> G[Database: TicksDB]
        D --> H[Database: TicksDB]
        F --> I[Database: TicksDB]
        
        G --> J[Container: AAPL_2024-10-01]
        G --> K[Container: AAPL_2024-10-02]
        G --> L[Container: AAPL_2024-10-03]
        
        H --> M[Container: MSFT_2024-10-01]
        H --> N[Container: MSFT_2024-10-02]
        H --> O[Container: MSFT_2024-10-03]
        
        I --> P[Container: GOOGL_2024-10-01]
        I --> Q[Container: GOOGL_2024-10-02]
        I --> R[Container: GOOGL_2024-10-03]
        
        J --> S[Partition Key: AAPL_2024-10-01_1]
        J --> T[Partition Key: AAPL_2024-10-01_2]
        J --> U[Partition Key: AAPL_2024-10-01_8]
    end
    
    subgraph "Execution Contexts"
        V[RicQueryExecutionState for AAPL]
        W[RicQueryExecutionState for MSFT]
        X[RicQueryExecutionState for GOOGL]
        
        V --> Y[TickRequestContextPerPartitionKey 1]
        V --> Z[TickRequestContextPerPartitionKey 2]
        V --> AA[TickRequestContextPerPartitionKey 3]
        
        W --> BB[TickRequestContextPerPartitionKey 4]
        W --> CC[TickRequestContextPerPartitionKey 5]
        W --> DD[TickRequestContextPerPartitionKey 6]
        
        X --> EE[TickRequestContextPerPartitionKey 7]
        X --> FF[TickRequestContextPerPartitionKey 8]
        X --> GG[TickRequestContextPerPartitionKey 9]
    end
    
    style A fill:#e1f5fe
    style C fill:#e1f5fe
    style E fill:#e1f5fe
    style V fill:#fff3e0
    style W fill:#fff3e0
    style X fill:#fff3e0
```

## Concurrency and Threading Model

```mermaid
graph TB
    subgraph "Thread Pool Management"
        A[queryExecutorService] --> B[Fixed Thread Pool]
        B --> C[Concurrency = CPU Count * 10]
        
        C --> D[Thread 1]
        C --> E[Thread 2]
        C --> F[Thread 3]
        C --> G[Thread N]
    end
    
    subgraph "Parallel Execution"
        H[RIC 1 Task] --> D
        I[RIC 2 Task] --> E
        J[RIC 3 Task] --> F
        K[RIC N Task] --> G
        
        D --> L[Cosmos DB Container 1]
        E --> M[Cosmos DB Container 2]
        F --> N[Cosmos DB Container 3]
        G --> O[Cosmos DB Container N]
    end
    
    subgraph "Result Aggregation"
        L --> P[Result Queue]
        M --> P
        N --> P
        O --> P
        
        P --> Q[TickResponse]
    end
    
    style A fill:#fff3e0
    style C fill:#fff3e0
    style Q fill:#e8f5e8
```

## Error Handling Flow

```mermaid
flowchart TD
    A[Query Execution] --> B{Exception Occurs?}
    B -->|No| C[Continue Processing]
    B -->|Yes| D{Exception Type?}
    
    D -->|CosmosException| E[Handle Cosmos Exception]
    D -->|InterruptedException| F[Log Error]
    D -->|ExecutionException| G[Log Error]
    D -->|Other| H[Log Error]
    
    E --> I{Resource Not Found?}
    I -->|Yes| J[Set Token to drained]
    I -->|No| K[Log Error Details]
    
    J --> L[Continue with Next Context]
    K --> M[Throw Exception]
    F --> M
    G --> M
    H --> M
    
    M --> N[RuntimeException]
    N --> O[Return Error Response]
    
    style A fill:#fff3e0
    style E fill:#fff3e0
    style O fill:#ffebee
```

## Performance Metrics Flow

```mermaid
flowchart LR
    A[Request Start] --> B[Generate Correlation ID]
    B --> C[Record Start Time]
    C --> D[Execute Queries]
    D --> E[Record End Time]
    E --> F[Calculate Duration]
    F --> G[Collect Diagnostics]
    G --> H[Build Response]
    H --> I[Return with Metrics]
    
    subgraph "Metrics Collected"
        J[Execution Duration]
        K[Correlation ID]
        L[Cosmos DB Diagnostics]
        M[Result Count]
        N[Error Count]
    end
    
    I --> J
    I --> K
    I --> L
    I --> M
    I --> N
    
    style A fill:#e1f5fe
    style I fill:#e8f5e8
    style J fill:#fff3e0
    style K fill:#fff3e0
    style L fill:#fff3e0
    style M fill:#fff3e0
    style N fill:#fff3e0
```

## Key Design Patterns Illustrated

1. **Asynchronous Processing**: CompletableFuture for non-blocking operations
2. **Parallel Execution**: Concurrent tasks across multiple RICs and containers
3. **Pagination Strategy**: Continuation token-based page fetching
4. **Error Handling**: Graceful exception handling with fallback strategies
5. **Resource Management**: Thread pool management and connection handling
6. **Performance Monitoring**: Execution time tracking and diagnostics collection
7. **Data Partitioning**: RIC-based account distribution and date-based containers
8. **State Management**: Execution state tracking across multiple contexts

These diagrams provide a comprehensive view of the complex asynchronous, parallel processing architecture implemented in `TickServiceImpl`. 