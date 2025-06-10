package com.tickreader.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "ticks")
public class CosmosDbAccountConfiguration {

    private final Map<Integer, CosmosDbAccount> cosmosDbAccounts;
    private final int shardCountPerRic;

    public CosmosDbAccountConfiguration(Map<Integer, CosmosDbAccount> cosmosDbAccounts, int shardCountPerRic) {
        this.cosmosDbAccounts = cosmosDbAccounts;
        this.shardCountPerRic = shardCountPerRic;
    }

    public Map<Integer, CosmosDbAccount> getCosmosDbAccounts() {
        return this.cosmosDbAccounts;
    }

    public int getShardCountPerRic() {
        return this.shardCountPerRic;
    }

    public int getAccountCount() {
        return this.cosmosDbAccounts != null ? this.cosmosDbAccounts.size() : 0;
    }

    public CosmosDbAccount getCosmosDbAccount(int key) {
        return this.cosmosDbAccounts.get(key);
    }
}
