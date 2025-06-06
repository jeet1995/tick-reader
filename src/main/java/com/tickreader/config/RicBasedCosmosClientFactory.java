package com.tickreader.config;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClientBuilder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RicBasedCosmosClientFactory {

    private final RicCosmosProperties ricCosmosProperties;

    private final Map<String, CosmosAsyncClient> clients = new ConcurrentHashMap<>();

    public RicBasedCosmosClientFactory(RicCosmosProperties ricCosmosProperties) {
        this.ricCosmosProperties = ricCosmosProperties;
    }

    @PostConstruct
    public void initialize() {
        this.ricCosmosProperties.getRics().forEach((key, value) -> {
            try {
                CosmosAsyncClient client = new CosmosClientBuilder()
                        .endpoint(value.getUri())
                        .key("your-cosmos-key") // Replace with actual key or fetch from secure storage
                        .contentResponseOnWriteEnabled(true)
                        .gatewayMode()
                        .buildAsyncClient();

                clients.put(key, client);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CosmosAsyncClient getCosmosAsyncClient(String key) {
        CosmosAsyncClient client = clients.get(key);

        if (client == null) {
            throw new IllegalArgumentException("No Cosmos client found for key: " + key);
        }

        return client;
    }

    public RicCosmosProperties getRicCosmosProperties() {
        return ricCosmosProperties;
    }
}
