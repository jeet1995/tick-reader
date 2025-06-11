package com.tickreader.service;

import com.tickreader.dto.TickResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface TicksService {
    TickResponse getTicks(
            List<String> rics,
            int totalTicks,
            boolean pinStart,
            LocalDateTime startTime,
            LocalDateTime endTime);
}
