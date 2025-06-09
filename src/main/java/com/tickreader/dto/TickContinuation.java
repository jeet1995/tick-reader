package com.tickreader.dto;

public class TickContinuation {
    private final String containerId;
    private final String continuationToken;
    private final String tickIdentifier;

    public TickContinuation(String containerId, String continuationToken, String tickIdentifier) {
        this.containerId = containerId;
        this.continuationToken = continuationToken;
        this.tickIdentifier = tickIdentifier;
    }

    public String getContainerId() {
        return this.containerId;
    }

    public String getContinuationToken() {
        return this.continuationToken;
    }

    public String getTickIdentifier() {
        return this.tickIdentifier;
    }
}
