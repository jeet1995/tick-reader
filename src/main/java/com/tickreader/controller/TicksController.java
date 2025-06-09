package com.tickreader.controller;

import com.tickreader.entity.Tick;
import com.tickreader.service.TicksService;
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

    private final TicksService ticksService;

    public TicksController(TicksService ticksService) {
        this.ticksService = ticksService;
    }

    @GetMapping("")
    public List<Tick> getTicks(
            @RequestParam List<String> rics,
            @RequestParam int totalTicks,
            @RequestParam boolean pinStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        return ticksService.getTicks(rics, totalTicks, pinStart, startTime, endTime);
    }
}
