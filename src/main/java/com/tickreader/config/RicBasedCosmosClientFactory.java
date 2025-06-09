package com.tickreader.config;

import com.azure.core.credential.TokenCredential;
import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.implementation.apachecommons.lang.StringUtils;
import com.azure.cosmos.implementation.guava25.base.Strings;
import com.azure.identity.DefaultAzureCredentialBuilder;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RicBasedCosmosClientFactory {

    private final RicCosmosProperties ricCosmosProperties;

    private final Map<String, CosmosAsyncClient> clients = new ConcurrentHashMap<>();

    private static final String AAD_LOGIN_ENDPOINT = System.getProperty("AAD_LOGIN_ENDPOINT",
            StringUtils.defaultString(Strings.emptyToNull(
                    System.getenv().get("AAD_LOGIN_ENDPOINT")), "https://login.microsoftonline.com/"));

    private static final String AAD_MANAGED_IDENTITY_ID = System.getProperty("AAD_MANAGED_IDENTITY_ID",
            StringUtils.defaultString(Strings.emptyToNull(
                    System.getenv().get("AAD_MANAGED_IDENTITY_ID")), ""));

    private static final String AAD_TENANT_ID = System.getProperty("AAD_TENANT_ID",
            StringUtils.defaultString(Strings.emptyToNull(
                    System.getenv().get("AAD_TENANT_ID")), ""));

    private static final TokenCredential CREDENTIAL = new DefaultAzureCredentialBuilder()
            .managedIdentityClientId(AAD_MANAGED_IDENTITY_ID)
            .authorityHost(AAD_LOGIN_ENDPOINT)
            .tenantId(AAD_TENANT_ID)
            .build();

    public RicBasedCosmosClientFactory(RicCosmosProperties ricCosmosProperties) {
        this.ricCosmosProperties = ricCosmosProperties;
        this.ricCosmosProperties.getRics().forEach((key, value) -> {
            try {
                CosmosAsyncClient client = new CosmosClientBuilder()
                        .endpoint(value.getUri())
                        .credential(CREDENTIAL) // Replace with actual key or fetch from secure storage
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
