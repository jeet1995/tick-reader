package com.tickreader.service;

import com.tickreader.dto.TickResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface BackupTicksService {
    /**
     * Synchronous version of getTicks
     */
    TickResponse getTicks(
            List<String> rics,
            List<String> docTypes,
            int totalTicks,
            boolean pinStart,
            LocalDateTime startTime,
            LocalDateTime endTime);

    /**
     * Asynchronous version of getTicks using CompletableFuture
     */
    CompletableFuture<TickResponse> getTicksAsync(
            List<String> rics,
            List<String> docTypes,
            int totalTicks,
            boolean pinStart,
            LocalDateTime startTime,
            LocalDateTime endTime);
} 