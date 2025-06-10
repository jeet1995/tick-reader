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

    @GetMapping("/sort=messageTimestamp&recordKey")
    public TickResponse getTicks(
            @RequestParam List<String> rics,
            @RequestParam int totalTicks,
            @RequestParam boolean pinStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam (required = false, defaultValue = "1") int prefetch,
            @RequestParam (required = false, defaultValue = "10") int pageSize,
            @RequestParam (required = false, defaultValue = "1") int concurrency) {

        TickResponse tickResponse = ticksService.getTicks(
                rics, totalTicks, pinStart, startTime, endTime, prefetch, pageSize, concurrency);

        if (logger.isDebugEnabled()) {
            logger.debug("Request parameters: rics={}, totalTicks={}, pinStart={}, startTime={}, endTime={}, prefetch={}, pageSize={}",
                    rics, totalTicks, pinStart, startTime, endTime, prefetch, pageSize);
            logger.debug("Ticks fetched: {}", tickResponse.getTicks().size());
        }

        return tickResponse;
    }
}
