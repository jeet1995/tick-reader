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

    /**
     * Retrieves tick data for specified RICs within a time range with additional range filters.
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
     */
    TickResponse getTicksWithRangeFilters(
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
            Double trnovrUnsMax);

    /**
     * Retrieves tick data for specified RICs within a time range with price and volume filters.
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
     */
    TickResponse getTicksWithPriceVolumeFilters(
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
            Double trdvol1Min);

    /**
     * Retrieves tick data for specified RICs within a time range with qualifiers string filters.
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
     */
    TickResponse getTicksWithQualifiersFilters(
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
            List<String> notStartsWithFilters);
}
