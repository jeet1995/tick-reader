package com.tickreader.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "ric.cosmos")
public class RicCosmosProperties {

    private Map<String, CosmosProperties> rics;
    private int shardCount;

    public Map<String, CosmosProperties> getRics() {
        return rics;
    }

    public CosmosProperties getRicCosmosProperties(String ricGroup) {
        return rics.get(ricGroup);
    }

    public void setRics(Map<String, CosmosProperties> rics) {
        this.rics = rics;
    }

    public int getShardCount() {
        return this.shardCount;
    }

    public void setShardCount(int shardCount) {
        this.shardCount = shardCount;
    }

    public static class CosmosProperties {
        private String uri;
        private String database;

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getDatabaseId() {
            return database;
        }

        public void setDatabase(String database) {
            this.database = database;
        }
    }
}
