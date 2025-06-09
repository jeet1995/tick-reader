package com.tickreader.dto;

import com.tickreader.entity.Tick;
import com.tickreader.service.impl.TickRequestContext;

import java.util.List;

public class TickResponse {
    private final List<Tick> ticks;
    private final List<TickContinuation> tickContinuations;

    public TickResponse(List<Tick> ticks, List<TickContinuation> tickContinuations) {
        this.ticks = ticks;
        this.tickContinuations = tickContinuations;
    }

    public List<Tick> getTicks() {
        return this.ticks;
    }

    public List<TickContinuation> getTickContinuations() {
        return this.tickContinuations;
    }
}
