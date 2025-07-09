# Tick Service Projections Feature Implementation

## Overview

The Tick Service has been enhanced with a **projections feature** that allows you to specify which fields to include in the SELECT clause of Cosmos DB queries. This feature improves performance by reducing data transfer and allows for more efficient queries when only specific fields are needed.

## Implementation Details

### 1. Interface Changes

The `TicksService` interface has been updated to include a new `projections` parameter:

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
            boolean includeDiagnostics,
            List<String> projections);  // NEW PARAMETER
}
```

### 2. Controller Changes

The `TicksController` now accepts projections as an optional request parameter:

```java
@GetMapping("/sort=messageTimestamp")
public TickResponse getTicks(
        // ... existing parameters ...
        @RequestParam(required = false) List<String> projections) {
    // ... implementation
}
```

### 3. Query Building Logic

The `getSqlQuerySpec` method in `TickServiceImpl` has been enhanced to build dynamic SELECT clauses:

```java
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
```

## Usage Examples

### Example 1: Select All Fields (Default Behavior)
```http
GET /ticks/sort=messageTimestamp?rics=AAPL&docTypes=QUOTE&totalTicks=100&pinStart=true&startTime=2024-01-01T00:00:00&endTime=2024-01-01T23:59:59
```

**Generated Query:**
```sql
SELECT * FROM C WHERE C.pk IN (@pk1, @pk2, ..., @pk8) 
  AND C.docType IN (@docType0) 
  AND C.messageTimestamp >= @startTime AND C.messageTimestamp < @endTime 
ORDER BY C.messageTimestamp ASC OFFSET 0 LIMIT 100
```

### Example 2: Select Specific Fields
```http
GET /ticks/sort=messageTimestamp?rics=AAPL&docTypes=QUOTE&totalTicks=100&pinStart=true&startTime=2024-01-01T00:00:00&endTime=2024-01-01T23:59:59&projections=id,pk,ricName,messageTimestamp,BID,ASK,BIDSIZE,ASKSIZE
```

**Generated Query:**
```sql
SELECT C.id, C.pk, C.ricName, C.messageTimestamp, C.BID, C.ASK, C.BIDSIZE, C.ASKSIZE FROM C 
WHERE C.pk IN (@pk1, @pk2, ..., @pk8) 
  AND C.docType IN (@docType0) 
  AND C.messageTimestamp >= @startTime AND C.messageTimestamp < @endTime 
ORDER BY C.messageTimestamp ASC OFFSET 0 LIMIT 100
```

### Example 3: Select Minimal Fields for Performance
```http
GET /ticks/sort=messageTimestamp?rics=AAPL&docTypes=QUOTE&totalTicks=100&pinStart=true&startTime=2024-01-01T00:00:00&endTime=2024-01-01T23:59:59&projections=id,messageTimestamp,BID,ASK
```

**Generated Query:**
```sql
SELECT C.id, C.messageTimestamp, C.BID, C.ASK FROM C 
WHERE C.pk IN (@pk1, @pk2, ..., @pk8) 
  AND C.docType IN (@docType0) 
  AND C.messageTimestamp >= @startTime AND C.messageTimestamp < @endTime 
ORDER BY C.messageTimestamp ASC OFFSET 0 LIMIT 100
```

### Example 4: Error Scenarios

#### Empty Projections List (Will Fail)
```http
GET /ticks/sort=messageTimestamp?rics=AAPL&docTypes=QUOTE&totalTicks=100&pinStart=true&startTime=2024-01-01T00:00:00&endTime=2024-01-01T23:59:59&projections=
```

**Response:**
```json
{
  "timestamp": "2024-01-01T12:00:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Projections list cannot be empty. Either provide valid field names or omit the parameter to select all fields."
}
```

#### Missing messageTimestamp (Will Fail)
```http
GET /ticks/sort=messageTimestamp?rics=AAPL&docTypes=QUOTE&totalTicks=100&pinStart=true&startTime=2024-01-01T00:00:00&endTime=2024-01-01T23:59:59&projections=id,BID,ASK
```

**Response:**
```json
{
  "timestamp": "2024-01-01T12:00:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Projections list must include 'messageTimestamp' field for sorting functionality. Provided projections: [id, BID, ASK]"
}
```

#### Invalid Field Name (Will Fail)
```http
GET /ticks/sort=messageTimestamp?rics=AAPL&docTypes=QUOTE&totalTicks=100&pinStart=true&startTime=2024-01-01T00:00:00&endTime=2024-01-01T23:59:59&projections=id,messageTimestamp,invalidField
```

**Response:**
```json
{
  "timestamp": "2024-01-01T12:00:00.000Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Cosmos DB query error: Invalid field name 'invalidField'"
}
```

## Available Fields

The following fields are available for projection (based on the `Tick` entity):

### Core Fields
- `id` - Document ID
- `pk` - Partition key
- `ricName` - RIC identifier
- `messageTimestamp` - Message timestamp
- `executionTime` - Execution time
- `msgSequence` - Message sequence number

### Price Fields
- `BID` - Bid price
- `ASK` - Ask price
- `BIDSIZE` - Bid size
- `ASKSIZE` - Ask size
- `MID_PRICE` - Mid price
- `LAST_QUOTE` - Last quote price

### Market Data Fields
- `TRDVOL_1` - Trade volume
- `VWAP` - Volume weighted average price
- `OPEN_PRC` - Open price
- `HIGH_1` - High price
- `LOW_1` - Low price
- `YIELD` - Yield

### Additional Fields
- `docType` - Document type
- `RecordKey` - Record key
- `COLLECT_DATETIME` - Collection datetime
- `SOURCE_DATETIME` - Source datetime
- `ChangeTimeStamp` - Change timestamp

*Note: This is a subset of available fields. The complete list can be found in the `Tick` entity class.*

## Benefits

1. **Performance Improvement**: Reduces data transfer between Cosmos DB and application
2. **Network Efficiency**: Smaller payload sizes for API responses
3. **Memory Optimization**: Less memory usage for processing results
4. **Flexibility**: Allows clients to request only the data they need
5. **Backward Compatibility**: Existing queries continue to work (projections parameter is optional)

## Best Practices

1. **Use Projections for High-Volume Queries**: When querying large datasets, specify only required fields
2. **Include Essential Fields**: Always include `id`, `pk`, and `messageTimestamp` for proper data handling
3. **Consider Client Needs**: Tailor projections based on what the client application actually uses
4. **Monitor Performance**: Compare query performance with and without projections
5. **Validate Field Names**: Ensure field names match exactly with the `Tick` entity properties

## Validation Rules

The projections parameter has the following validation rules:

### 1. Empty List Validation
- **Rule**: If projections list is provided (not null), it cannot be empty
- **Error**: `IllegalArgumentException` with message: "Projections list cannot be empty. Either provide valid field names or omit the parameter to select all fields."
- **HTTP Status**: 400 Bad Request

### 2. Required Field Validation
- **Rule**: If projections list is provided, it must include `messageTimestamp` field
- **Reason**: `messageTimestamp` is required for sorting functionality (ORDER BY clause)
- **Error**: `IllegalArgumentException` with message: "Projections list must include 'messageTimestamp' field for sorting functionality. Provided projections: [list]"
- **HTTP Status**: 400 Bad Request

### 3. Field Name Validation
- **Rule**: Field names must match exactly with the `Tick` entity properties
- **Error**: Cosmos DB will return an error for invalid field names
- **HTTP Status**: 500 Internal Server Error (from Cosmos DB)

## Error Handling

- Validation errors are caught and logged at both controller and service levels
- Invalid projections result in 400 Bad Request responses with descriptive error messages
- Field name validation errors are handled by Cosmos DB and returned as 500 Internal Server Error
- All validation errors are logged for debugging purposes

## Migration Guide

### For Existing Clients
- No changes required - the `projections` parameter is optional
- Existing queries will continue to work with `SELECT *` behavior
- Gradually adopt projections for performance optimization

### For New Clients
- Consider using projections from the start for better performance
- Start with essential fields and add more as needed
- Test with different projection combinations to find optimal performance 