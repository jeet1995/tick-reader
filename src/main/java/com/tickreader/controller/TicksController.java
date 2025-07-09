package com.tickreader.controller;

import com.tickreader.dto.TickResponse;
import com.tickreader.entity.Tick;
import com.tickreader.service.TicksService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/ticks")
public class TicksController {

    private static Logger logger = LoggerFactory.getLogger(TicksController.class);
    private final TicksService ticksService;

    public TicksController(TicksService ticksService) {
        this.ticksService = ticksService;
    }

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
            @RequestParam(required = false, defaultValue = "false") boolean includeDiagnostics,
            @RequestParam(required = false) List<String> projections) {

        try {
            TickResponse tickResponse = ticksService.getTicks(
                    rics, docTypes, totalTicks, pinStart, startTime, endTime, includeNullValues, pageSize, includeDiagnostics, projections);

            if (logger.isDebugEnabled()) {
                logger.debug("Request parameters: rics={}, totalTicks={}, pinStart={}, startTime={}, endTime={}, projections={}",
                        rics, totalTicks, pinStart, startTime, endTime, projections);
                logger.debug("Ticks fetched: {}", tickResponse.getTicks().size());
            }

            return tickResponse;
        } catch (IllegalArgumentException e) {
            logger.error("Invalid projections parameter: {}", e.getMessage());
            throw e; // Re-throw to return 400 Bad Request
        }
    }

    @GetMapping("/with-range-filters")
    public TickResponse getTicksWithRangeFilters(
            @RequestParam List<String> rics,
            @RequestParam List<String> docTypes,
            @RequestParam int totalTicks,
            @RequestParam boolean pinStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false, defaultValue = "false") boolean includeNullValues,
            @RequestParam(required = false, defaultValue = "100") int pageSize,
            @RequestParam(required = false, defaultValue = "false") boolean includeDiagnostics,
            @RequestParam(required = false) List<String> projections,
            @RequestParam Double trdprc1Min,
            @RequestParam Double trdprc1Max,
            @RequestParam Double trnovrUnsMin,
            @RequestParam Double trnovrUnsMax) {

        try {
            TickResponse tickResponse = ticksService.getTicksWithRangeFilters(
                    rics, docTypes, totalTicks, pinStart, startTime, endTime, 
                    includeNullValues, pageSize, includeDiagnostics, projections,
                    trdprc1Min, trdprc1Max, trnovrUnsMin, trnovrUnsMax);

            if (logger.isDebugEnabled()) {
                logger.debug("Request parameters: rics={}, totalTicks={}, pinStart={}, startTime={}, endTime={}, " +
                        "projections={}, trdprc1Min={}, trdprc1Max={}, trnovrUnsMin={}, trnovrUnsMax={}",
                        rics, totalTicks, pinStart, startTime, endTime, projections,
                        trdprc1Min, trdprc1Max, trnovrUnsMin, trnovrUnsMax);
                logger.debug("Ticks fetched: {}", tickResponse.getTicks().size());
            }

            return tickResponse;
        } catch (IllegalArgumentException e) {
            logger.error("Invalid parameters: {}", e.getMessage());
            throw e; // Re-throw to return 400 Bad Request
        }
    }

    @GetMapping("/with-price-volume-filters")
    public TickResponse getTicksWithPriceVolumeFilters(
            @RequestParam List<String> rics,
            @RequestParam List<String> docTypes,
            @RequestParam int totalTicks,
            @RequestParam boolean pinStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false, defaultValue = "false") boolean includeNullValues,
            @RequestParam(required = false, defaultValue = "100") int pageSize,
            @RequestParam(required = false, defaultValue = "false") boolean includeDiagnostics,
            @RequestParam(required = false) List<String> projections,
            @RequestParam Double trdprc1Min,
            @RequestParam Double trdprc1Max,
            @RequestParam Double trdvol1Min) {

        try {
            TickResponse tickResponse = ticksService.getTicksWithPriceVolumeFilters(
                    rics, docTypes, totalTicks, pinStart, startTime, endTime, 
                    includeNullValues, pageSize, includeDiagnostics, projections,
                    trdprc1Min, trdprc1Max, trdvol1Min);

            if (logger.isDebugEnabled()) {
                logger.debug("Request parameters: rics={}, totalTicks={}, pinStart={}, startTime={}, endTime={}, " +
                        "projections={}, trdprc1Min={}, trdprc1Max={}, trdvol1Min={}",
                        rics, totalTicks, pinStart, startTime, endTime, projections,
                        trdprc1Min, trdprc1Max, trdvol1Min);
                logger.debug("Ticks fetched: {}", tickResponse.getTicks().size());
            }

            return tickResponse;
        } catch (IllegalArgumentException e) {
            logger.error("Invalid parameters: {}", e.getMessage());
            throw e; // Re-throw to return 400 Bad Request
        }
    }

    @GetMapping("/with-qualifiers-filters")
    public TickResponse getTicksWithQualifiersFilters(
            @RequestParam List<String> rics,
            @RequestParam List<String> docTypes,
            @RequestParam int totalTicks,
            @RequestParam boolean pinStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false, defaultValue = "false") boolean includeNullValues,
            @RequestParam(required = false, defaultValue = "100") int pageSize,
            @RequestParam(required = false, defaultValue = "false") boolean includeDiagnostics,
            @RequestParam(required = false) List<String> projections,
            @RequestParam(required = false) List<String> containsFilters,
            @RequestParam(required = false) List<String> notContainsFilters,
            @RequestParam(required = false) List<String> startsWithFilters,
            @RequestParam(required = false) List<String> notStartsWithFilters) {

        try {
            TickResponse tickResponse = ticksService.getTicksWithQualifiersFilters(
                    rics, docTypes, totalTicks, pinStart, startTime, endTime, 
                    includeNullValues, pageSize, includeDiagnostics, projections,
                    containsFilters, notContainsFilters, startsWithFilters, notStartsWithFilters);

            if (logger.isDebugEnabled()) {
                logger.debug("Request parameters: rics={}, totalTicks={}, pinStart={}, startTime={}, endTime={}, " +
                        "projections={}, containsFilters={}, notContainsFilters={}, startsWithFilters={}, notStartsWithFilters={}",
                        rics, totalTicks, pinStart, startTime, endTime, projections,
                        containsFilters, notContainsFilters, startsWithFilters, notStartsWithFilters);
                logger.debug("Ticks fetched: {}", tickResponse.getTicks().size());
            }

            return tickResponse;
        } catch (IllegalArgumentException e) {
            logger.error("Invalid parameters: {}", e.getMessage());
            throw e; // Re-throw to return 400 Bad Request
        }
    }
}
