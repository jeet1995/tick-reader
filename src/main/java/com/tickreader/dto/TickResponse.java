package com.tickreader.dto;

import com.azure.cosmos.CosmosDiagnostics;
import com.tickreader.entity.BaseTick;
import com.tickreader.entity.Tick;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class TickResponse {
    private final List<BaseTick> ticks;
    private final List<CosmosDiagnostics> diagnosticsList;
    private final Duration executionTime;

    public TickResponse(List<BaseTick> ticks, List<CosmosDiagnostics> diagnosticsList, Duration executionTime) {
        this.ticks = ticks != null ? ticks : new ArrayList<>();
        this.diagnosticsList = diagnosticsList;
        this.executionTime = executionTime;
    }

    public List<BaseTick> getTicks() {
        return this.ticks;
    }

    public List<CosmosDiagnostics> getDiagnosticsList() {
        return this.diagnosticsList != null ? this.diagnosticsList : new ArrayList<>();
    }

    public Duration getExecutionTime() {
        return this.executionTime != null ? this.executionTime : Duration.ZERO;
    }
}
