# Backup Tick Service Implementation

## Overview

This document describes the backup `TickServiceImpl` implementation that provides an alternative to the Project Reactor-based implementation. The backup implementation uses traditional Java concurrency patterns instead of reactive programming.

## Key Features

### No Project Reactor Dependencies
- Uses `CompletableFuture` for asynchronous operations
- Uses `ExecutorService` for thread management
- Uses `PriorityBlockingQueue` for ordered tick collection
- Uses synchronized collections for thread safety

### Dual Interface Support
- **Synchronous**: `getTicks()` - blocks until completion
- **Asynchronous**: `getTicksAsync()` - returns `CompletableFuture<TickResponse>`

### Simplified Error Handling
- Uses try-catch blocks and `RuntimeException`
- More straightforward exception handling compared to Reactor patterns

## Configuration

### Spring Boot Configuration

Add the following property to your `application.properties` or `application.yml`:

```properties
# Use Reactor-based implementation (default)
tick.service.implementation=reactor

# Use Backup implementation
tick.service.implementation=backup
```

### Bean Configuration

The `TickServiceConfiguration` class automatically configures the appropriate implementation based on the property:

- When `tick.service.implementation=reactor` (default): Uses `TicksServiceImpl`
- When `tick.service.implementation=backup`: Uses `BackupTicksServiceImpl`

## Usage Examples

### Synchronous Usage

```java
@Autowired
private BackupTicksService backupTicksService;

public void synchronousExample() {
    List<String> rics = Arrays.asList("AAPL.O", "MSFT.O");
    List<String> docTypes = Arrays.asList("QUOTE", "TRADE");
    int totalTicks = 1000;
    boolean pinStart = true;
    LocalDateTime startTime = LocalDateTime.now().minusHours(1);
    LocalDateTime endTime = LocalDateTime.now();

    TickResponse response = backupTicksService.getTicks(
            rics, docTypes, totalTicks, pinStart, startTime, endTime);
    
    System.out.println("Ticks retrieved: " + response.getTicks().size());
    System.out.println("Execution time: " + response.getExecutionTime());
}
```

### Asynchronous Usage

```java
@Autowired
private BackupTicksService backupTicksService;

public void asynchronousExample() {
    List<String> rics = Arrays.asList("AAPL.O", "MSFT.O");
    List<String> docTypes = Arrays.asList("QUOTE", "TRADE");
    int totalTicks = 1000;
    boolean pinStart = true;
    LocalDateTime startTime = LocalDateTime.now().minusHours(1);
    LocalDateTime endTime = LocalDateTime.now();

    CompletableFuture<TickResponse> futureResponse = backupTicksService.getTicksAsync(
            rics, docTypes, totalTicks, pinStart, startTime, endTime);

    futureResponse
            .thenAccept(response -> {
                System.out.println("Ticks retrieved: " + response.getTicks().size());
                System.out.println("Execution time: " + response.getExecutionTime());
            })
            .exceptionally(throwable -> {
                System.err.println("Error: " + throwable.getMessage());
                return null;
            });
}
```

## Architecture Differences

### Thread Management
- **Reactor**: Uses event loop threads and non-blocking I/O
- **Backup**: Uses `ExecutorService` with configurable thread pool

### Data Flow
- **Reactor**: Reactive streams with backpressure handling
- **Backup**: Blocking operations with `PriorityBlockingQueue` for ordering

### Error Handling
- **Reactor**: Error signals in reactive streams
- **Backup**: Traditional exception throwing and catching

## Performance Considerations

### Advantages of Backup Implementation
- **Easier Debugging**: Stack traces are more straightforward
- **Familiar Patterns**: Uses standard Java concurrency
- **Predictable Behavior**: Blocking operations are easier to reason about

### Disadvantages of Backup Implementation
- **Resource Usage**: May use more threads than Reactor
- **Scalability**: May not scale as well for high-throughput scenarios
- **Memory**: May use more memory due to blocking operations

## Migration Guide

### From Reactor to Backup

1. **Update Configuration**:
   ```properties
   tick.service.implementation=backup
   ```

2. **Update Dependencies** (if needed):
   - Remove Reactor-specific dependencies if not used elsewhere
   - Ensure Java 8+ for `CompletableFuture` support

3. **Update Code**:
   - Replace `TicksService` with `BackupTicksService` where needed
   - Update error handling to use try-catch instead of reactive error handling

### From Backup to Reactor

1. **Update Configuration**:
   ```properties
   tick.service.implementation=reactor
   ```

2. **Update Code**:
   - Replace `BackupTicksService` with `TicksService`
   - Update to reactive patterns if needed

## Testing

The backup implementation can be tested using the same test cases as the Reactor implementation, as both implement the same core functionality. The main differences are in the concurrency patterns and error handling.

## Troubleshooting

### Common Issues

1. **Thread Pool Exhaustion**: If you see `RejectedExecutionException`, consider increasing the thread pool size in the constructor.

2. **Memory Issues**: The `PriorityBlockingQueue` holds all ticks in memory. For very large datasets, consider implementing pagination.

3. **Timeout Issues**: The synchronous `getTicks()` method blocks indefinitely. Consider using the asynchronous version with timeouts.

### Logging

The backup implementation uses the same logging framework as the Reactor implementation. Check logs for:
- Execution start/end times
- Query execution errors
- Thread pool status

## Future Enhancements

Potential improvements for the backup implementation:
- Configurable thread pool sizes
- Timeout support for operations
- Pagination support for large datasets
- Circuit breaker pattern for resilience
- Metrics and monitoring integration 