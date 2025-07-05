package com.tickreader.service.impl;

import com.tickreader.entity.Tick;

import java.util.ArrayList;
import java.util.List;

public class RicQueryExecutionState {

    private List<Tick> ticks;
    private List<TickRequestContextPerPartitionKey> tickRequestContexts;
    private boolean isCompleted;

    public RicQueryExecutionState(List<TickRequestContextPerPartitionKey> tickRequestContexts) {
        this.ticks = new ArrayList<>();
        this.tickRequestContexts = tickRequestContexts;
        this.isCompleted = false;
    }

    public synchronized void addTicks(List<Tick> ticksToAdd, int tickCount) {
        int size = this.ticks.size();
        int remainingTicks = tickCount - size;

        if (ticksToAdd.size() > remainingTicks) {
            ticksToAdd = ticksToAdd.subList(0, remainingTicks);
            this.ticks.addAll(ticksToAdd);
        } else {
            this.ticks.addAll(ticksToAdd);
        }

        if (this.ticks.size() == tickCount) {
            this.setCompleted(true);
        }
    }

    public synchronized List<Tick> getTicks() {
        return this.ticks;
    }

    public synchronized boolean isCompleted() {
        return this.isCompleted;
    }

    public synchronized void setCompleted(boolean completed) {
        this.isCompleted = completed;
    }

    public List<TickRequestContextPerPartitionKey> getTickRequestContexts() {
        return this.tickRequestContexts;
    }
}
