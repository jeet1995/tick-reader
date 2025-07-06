package com.tickreader.service;

import com.tickreader.dto.TickResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface TicksService {
    /**
     * Retrieves tick data for specified RICs within a time range.
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
     */
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
            List<String> projections);
}
