package com.tickreader.config;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.models.CosmosClientTelemetryConfig;
import com.tickreader.logging.SamplingCosmosDiagnosticsLogger;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RicBasedCosmosClientFactory {

    private final CosmosDbAccountConfiguration cosmosDbAccountConfiguration;

    private final Map<Integer, CosmosAsyncClient> clients = new ConcurrentHashMap<>();

    private static final CosmosClientTelemetryConfig TELEMETRY_CONFIG = new CosmosClientTelemetryConfig()
            .diagnosticsHandler(new SamplingCosmosDiagnosticsLogger(Configs.getMaxDiagnosticLogCount(), Configs.getMaxDiagnosticLogIntervalInMs()));

    public RicBasedCosmosClientFactory(CosmosDbAccountConfiguration cosmosDbAccountConfiguration) {
        this.cosmosDbAccountConfiguration = cosmosDbAccountConfiguration;
        this.cosmosDbAccountConfiguration.getCosmosDbAccounts().forEach((key, val) -> {
            try {
                CosmosAsyncClient client = new CosmosClientBuilder()
                        .endpoint(val.getAccountUri())
                        .credential(Configs.getAadTokenCredential()) // Replace with actual key or fetch from secure storage
                        .contentResponseOnWriteEnabled(true)
                        .gatewayMode()
                        .clientTelemetryConfig(TELEMETRY_CONFIG)
                        .buildAsyncClient();

                clients.put(val.getHashId(), client);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CosmosAsyncClient getCosmosAsyncClient(int key) {
        CosmosAsyncClient client = clients.get(key);

        if (client == null) {
            throw new IllegalArgumentException("No Cosmos client found for key: " + key);
        }

        return client;
    }
}
