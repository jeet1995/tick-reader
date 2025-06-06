package com.tickreader.service.impl;

import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.models.SqlQuerySpec;

import java.util.concurrent.atomic.AtomicReference;

public class TickRequestContext {

    private final CosmosAsyncContainer asyncContainer;
    private final String tickIdentifier;
    private final AtomicReference<String> continuationToken = new AtomicReference<>();
    private final AtomicReference<SqlQuerySpec> sqlQuerySpec = new AtomicReference<>();

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
}
