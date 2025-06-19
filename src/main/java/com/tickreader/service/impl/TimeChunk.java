package com.tickreader.service.impl;

public class TimeChunk {

    private final long chunkEpochStart;
    private final long chunkEpochEnd;

    public TimeChunk(long chunkEpochStart, long chunkEpochEnd) {
        this.chunkEpochStart = chunkEpochStart;
        this.chunkEpochEnd = chunkEpochEnd;
    }

    public long getChunkEpochStart() {
        return chunkEpochStart;
    }

    public long getChunkEpochEnd() {
        return chunkEpochEnd;
    }

    @Override
    public String toString() {
        return "TimeChunk{" +
                "startTime=" + chunkEpochStart +
                ", endTime=" + chunkEpochEnd +
                '}';
    }
}
