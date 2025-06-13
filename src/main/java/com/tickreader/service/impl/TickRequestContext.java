package com.tickreader.service.impl;

import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosDiagnosticsContext;
import com.azure.cosmos.models.SqlQuerySpec;
import org.apache.spark.sql.execution.command.LoadDataCommand$;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class TickRequestContext {

    private final CosmosAsyncContainer asyncContainer;
    private final List<String> tickIdentifiers;
    private final String requestDateAsString;
    private final String dateFormat;
    private final AtomicReference<String> continuationToken = new AtomicReference<>();
    private final AtomicReference<SqlQuerySpec> sqlQuerySpec = new AtomicReference<>();
    private final CopyOnWriteArrayList<CosmosDiagnosticsContext> diagnosticsContexts = new CopyOnWriteArrayList<>();

    public TickRequestContext(CosmosAsyncContainer asyncContainer, List<String> tickIdentifiers, String requestDateAsString, String dateFormat) {
        this.asyncContainer = asyncContainer;
        this.tickIdentifiers = tickIdentifiers;
        this.requestDateAsString = requestDateAsString;
        this.dateFormat = dateFormat;
    }

    public CosmosAsyncContainer getAsyncContainer() {
        return this.asyncContainer;
    }

    public List<String> getTickIdentifiers() {
        return this.tickIdentifiers;
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

    public List<CosmosDiagnosticsContext> getDiagnosticsContexts() {
        return this.diagnosticsContexts;
    }

    public void addDiagnosticsContext(CosmosDiagnosticsContext diagnosticsContext) {
        this.diagnosticsContexts.add(diagnosticsContext);
    }
}
