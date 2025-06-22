package com.tickreader.service.impl;

import com.tickreader.entity.Tick;
import org.jetbrains.annotations.NotNull;

public class TickEntry implements Comparable<TickEntry> {

    private final Tick tick;
    private final int listIndex;
    private final int elementIndex;
    private final boolean pinStart;

    public TickEntry(Tick tick, int listIndex, int elementIndex, boolean pinStart) {
        this.tick = tick;
        this.listIndex = listIndex;
        this.elementIndex = elementIndex;
        this.pinStart = pinStart;
    }

    public Tick getTick() {
        return tick;
    }

    public int getListIndex() {
        return listIndex;
    }

    public int getElementIndex() {
        return elementIndex;
    }

    public boolean isPinStart() {
        return pinStart;
    }

    @Override
    public int compareTo(@NotNull TickEntry other) {
        if (this.pinStart) {
            if (other.tick.getMessageTimestamp().equals(this.tick.getMessageTimestamp())) {
                return Long.compare(other.tick.getRecordkey(), this.tick.getRecordkey());
            }
            return Long.compare(other.tick.getMessageTimestamp(), this.tick.getMessageTimestamp());
        } else {
            if (this.tick.getMessageTimestamp().equals(other.tick.getMessageTimestamp())) {
                return Long.compare(this.tick.getRecordkey(), other.tick.getRecordkey());
            }
            return Long.compare(this.tick.getMessageTimestamp(), other.tick.getMessageTimestamp());
        }
    }
}
