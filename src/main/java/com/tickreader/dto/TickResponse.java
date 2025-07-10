package com.tickreader.dto;

import com.azure.cosmos.CosmosDiagnostics;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tickreader.entity.BaseTick;
import com.tickreader.entity.Tick;
import com.tickreader.entity.TickInResponse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class TickResponse {
    private final List<ObjectNode> ticks;
    private final List<String> diagnosticsList;
    private final Duration executionTime;

    public TickResponse(List<ObjectNode> ticks, List<String> diagnosticsList, Duration executionTime) {
        this.ticks = ticks != null ? ticks : new ArrayList<>();
        this.diagnosticsList = diagnosticsList;
        this.executionTime = executionTime;
    }

    public List<ObjectNode> getTicks() {
        return this.ticks;
    }

    public List<String> getDiagnosticsList() {
        return this.diagnosticsList != null ? this.diagnosticsList : new ArrayList<>();
    }

    public Duration getExecutionTime() {
        return this.executionTime != null ? this.executionTime : Duration.ZERO;
    }
}
