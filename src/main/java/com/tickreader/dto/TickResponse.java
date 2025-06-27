package com.tickreader.dto;

import com.azure.cosmos.CosmosDiagnostics;
import com.tickreader.entity.BaseTick;
import com.tickreader.entity.Tick;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class TickResponse {
    private final List<BaseTick> ticks;
    private final List<String> diagnosticsList;
    private final Duration executionTime;

    public TickResponse(List<BaseTick> ticks, List<String> diagnosticsList, Duration executionTime) {
        this.ticks = ticks != null ? ticks : new ArrayList<>();
        this.diagnosticsList = diagnosticsList;
        this.executionTime = executionTime;
    }

    public List<BaseTick> getTicks() {
        return this.ticks;
    }

    public List<String> getDiagnosticsList() {
        return this.diagnosticsList != null ? this.diagnosticsList : new ArrayList<>();
    }

    public Duration getExecutionTime() {
        return this.executionTime != null ? this.executionTime : Duration.ZERO;
    }
}
