package com.tickreader.config;

public class CosmosDbAccount {

    private final String databaseName;
    private final String accountUri;
    private final String containerNamePrefix;
    private final String containerNameFormat;
    private final String containerNameSuffix;
    private final int hashId;

    public CosmosDbAccount(String databaseName, String accountUri, String containerNamePrefix, String containerNameFormat, String containerNameSuffix, int hashId) {
        this.databaseName = databaseName;
        this.accountUri = accountUri;
        this.containerNamePrefix = containerNamePrefix;
        this.containerNameFormat = containerNameFormat;
        this.containerNameSuffix = containerNameSuffix;
        this.hashId = hashId;
    }

    public String getDatabaseName() {
        return this.databaseName;
    }

    public String getAccountUri() {
        return this.accountUri;
    }

    public String getContainerNamePrefix() {
        return this.containerNamePrefix;
    }

    public String getContainerNameFormat() {
        return this.containerNameFormat;
    }

    public String getContainerNameSuffix() {
        return this.containerNameSuffix;
    }

    public int getHashId() {
        return this.hashId;
    }
}
