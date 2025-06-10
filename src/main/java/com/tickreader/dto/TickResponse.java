package com.tickreader.dto;

import com.azure.cosmos.CosmosDiagnosticsContext;
import com.tickreader.entity.Tick;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class TickResponse {
    private final List<Tick> ticks;
    private final List<CosmosDiagnosticsContext> diagnosticsContexts;
    private final Duration executionTime;

    public TickResponse(List<Tick> ticks, List<CosmosDiagnosticsContext> diagnosticsContexts, Duration executionTime) {
        this.ticks = ticks != null ? ticks : new ArrayList<>();
        this.diagnosticsContexts = diagnosticsContexts;
        this.executionTime = executionTime;
    }

    public List<Tick> getTicks() {
        return this.ticks;
    }

    public List<CosmosDiagnosticsContext> getDiagnosticsContexts() {
        return this.diagnosticsContexts != null ? this.diagnosticsContexts : new ArrayList<>();
    }

    public Duration getExecutionTime() {
        return this.executionTime != null ? this.executionTime : Duration.ZERO;
    }
}
