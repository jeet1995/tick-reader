package com.tickreader.service.impl;

import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.models.SqlQuerySpec;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class TickRequestContext {

    private final CosmosAsyncContainer asyncContainer;
    private final String tickIdentifier;
    private final AtomicReference<String> continuationToken = new AtomicReference<>();
    private final AtomicReference<SqlQuerySpec> sqlQuerySpec = new AtomicReference<>();
    private final AtomicInteger globalTickCountWithNonNullContinuationToken;

    public TickRequestContext(CosmosAsyncContainer asyncContainer, String tickIdentifier, AtomicInteger globalTickCountWithNonNullContinuationToken) {
        this.asyncContainer = asyncContainer;
        this.tickIdentifier = tickIdentifier;
        this.globalTickCountWithNonNullContinuationToken = globalTickCountWithNonNullContinuationToken;
    }

    public CosmosAsyncContainer getAsyncContainer() {
        return this.asyncContainer;
    }

    public String getTickIdentifier() {
        return this.tickIdentifier;
    }

    public String getContinuationToken() {
        return this.continuationToken.get();
    }

    public void setContinuationToken(String continuationToken) {
        this.continuationToken.set(continuationToken);
    }

    public SqlQuerySpec getSqlQuerySpec() {
        return this.sqlQuerySpec.get();
    }

    public void setSqlQuerySpec(SqlQuerySpec sqlQuerySpec) {
        this.sqlQuerySpec.set(sqlQuerySpec);
    }

    public void decrementGlobalTickCountWithNonNullContinuationToken() {
        if (this.globalTickCountWithNonNullContinuationToken.get() > 0) {
            this.globalTickCountWithNonNullContinuationToken.decrementAndGet();
        }
    }

    public int getGlobalTickCountWithNonNullContinuationToken() {
        return this.globalTickCountWithNonNullContinuationToken.get();
    }
}
