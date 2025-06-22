package com.tickreader.service.impl;

import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosDiagnostics;
import com.azure.cosmos.models.SqlQuerySpec;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class TickRequestContextPerPartitionKey {
    private final String id;
    private final CosmosAsyncContainer asyncContainer;
    private final String tickIdentifier;
    private final String requestDateAsString;
    private final String dateFormat;
    private final AtomicReference<String> continuationToken = new AtomicReference<>();
    private final AtomicReference<SqlQuerySpec> sqlQuerySpec = new AtomicReference<>();
    private final CopyOnWriteArrayList<CosmosDiagnostics> cosmosDiagnosticsList = new CopyOnWriteArrayList<>();

    public TickRequestContextPerPartitionKey(CosmosAsyncContainer asyncContainer, String tickIdentifier, String requestDateAsString, String dateFormat) {
        this.id = UUID.randomUUID().toString();
        this.asyncContainer = asyncContainer;
        this.tickIdentifier = tickIdentifier;
        this.requestDateAsString = requestDateAsString;
        this.dateFormat = dateFormat;
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

    public String getRequestDateAsString() {
        return this.requestDateAsString;
    }

    public String getDateFormat() {
        return this.dateFormat;
    }

    public List<CosmosDiagnostics> getCosmosDiagnosticsList() {
        return this.cosmosDiagnosticsList;
    }

    public void addCosmosDiagnostics(CosmosDiagnostics cosmosDiagnostics) {
        this.cosmosDiagnosticsList.add(cosmosDiagnostics);
    }

    public String getId() {
        return id;
    }
}
