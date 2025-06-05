package com.tickreader.service;

import com.tickreader.entity.Tick;

import java.time.LocalDateTime;
import java.util.List;

public interface TicksService {
    List<Tick> getTicks(
            List<String> rics,
            int totalTicks,
            boolean pinStart,
            LocalDateTime startTime,
            LocalDateTime endTime);
}
