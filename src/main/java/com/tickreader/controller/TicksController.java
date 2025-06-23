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
            @RequestParam(required = false, defaultValue = "false") boolean includeNullValues) {

        TickResponse tickResponse = ticksService.getTicks(
                rics, docTypes, totalTicks, pinStart, startTime, endTime, includeNullValues);

        if (logger.isDebugEnabled()) {
            logger.debug("Request parameters: rics={}, totalTicks={}, pinStart={}, startTime={}, endTime={}",
                    rics, totalTicks, pinStart, startTime, endTime);
            logger.debug("Ticks fetched: {}", tickResponse.getTicks().size());
        }

        return tickResponse;
    }
}
