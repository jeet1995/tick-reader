package com.tickreader.dto;

import com.tickreader.entity.Tick;

import java.util.ArrayList;
import java.util.List;

public class TickResponse {
    private final List<Tick> ticks;

    public TickResponse(List<Tick> ticks) {
        this.ticks = ticks != null ? ticks : new ArrayList<>();
    }

    public List<Tick> getTicks() {
        return this.ticks;
    }
}
