package com.tickreader.service.impl;

import com.azure.cosmos.CosmosAsyncContainer;

public class TickRequestContext {

    private final CosmosAsyncContainer asyncContainer;
    private final String tickIdentifier;

    public TickRequestContext(CosmosAsyncContainer asyncContainer, String tickIdentifier) {
        this.asyncContainer = asyncContainer;
        this.tickIdentifier = tickIdentifier;
    }

    public CosmosAsyncContainer getAsyncContainer() {
        return this.asyncContainer;
    }

    public String getTickIdentifier() {
        return this.tickIdentifier;
    }
}
