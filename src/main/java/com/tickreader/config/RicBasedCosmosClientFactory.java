package com.tickreader.config;

import com.azure.core.credential.TokenCredential;
import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.implementation.apachecommons.lang.StringUtils;
import com.azure.cosmos.implementation.guava25.base.Strings;
import com.azure.cosmos.models.CosmosClientTelemetryConfig;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.tickreader.logging.SamplingCosmosDiagnosticsLogger;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RicBasedCosmosClientFactory {

    private final RicCosmosProperties ricCosmosProperties;

    private final Map<String, CosmosAsyncClient> clients = new ConcurrentHashMap<>();

    private static final CosmosClientTelemetryConfig TELEMETRY_CONFIG = new CosmosClientTelemetryConfig()
            .diagnosticsHandler(new SamplingCosmosDiagnosticsLogger(Configs.getMaxDiagnosticLogCount(), Configs.getMaxDiagnosticLogIntervalInMs()));

    public RicBasedCosmosClientFactory(RicCosmosProperties ricCosmosProperties) {
        this.ricCosmosProperties = ricCosmosProperties;
        this.ricCosmosProperties.getRics().forEach((key, value) -> {
            try {
                CosmosAsyncClient client = new CosmosClientBuilder()
                        .endpoint(value.getUri())
                        .credential(Configs.getAadTokenCredential()) // Replace with actual key or fetch from secure storage
                        .contentResponseOnWriteEnabled(true)
                        .gatewayMode()
                        .clientTelemetryConfig(TELEMETRY_CONFIG)
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
