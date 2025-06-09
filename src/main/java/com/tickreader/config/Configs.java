package com.tickreader.config;

import com.azure.core.credential.TokenCredential;
import com.azure.cosmos.ConnectionMode;
import com.azure.identity.DefaultAzureCredentialBuilder;
import java.util.function.Function;

public class Configs {

    public static String getConnectionMode() {
        return getOptionalConfigProperty(
                "CONNECTION_MODE",
                ConnectionMode.GATEWAY.toString(),
                (s) -> {
                    if ("direct".equalsIgnoreCase(s)) {
                        return ConnectionMode.DIRECT.toString();
                    } else {
                        return ConnectionMode.GATEWAY.toString();
                    }
                });
    }

    /**
     * Returns the given string if it is nonempty; {@code null} otherwise.
     *
     * @param string the string to test and possibly return
     * @return {@code string} itself if it is nonempty; {@code null} if it is empty or null
     */
    private static String emptyToNull(String string) {
        if (string == null || string.isEmpty()) {
            return null;
        }

        return string;
    }

    public static TokenCredential getAadTokenCredential() {
        return credential;
    }

    private static final TokenCredential credential = new DefaultAzureCredentialBuilder()
            .managedIdentityClientId(Configs.getAadManagedIdentityId())
            .authorityHost(Configs.getAadLoginUri())
            .tenantId(Configs.getAadTenantId())
            .build();

    public static String getAadLoginUri() {
        return getOptionalConfigProperty(
                "AAD_LOGIN_ENDPOINT",
                "https://login.microsoftonline.com/",
                v -> v);
    }

    public static int getMaxDiagnosticLogCount() {
        return getOptionalConfigProperty(
                "COSMOS_MAX_DIAGNOSTICS_LOG_COUNT",
                10,
                v -> Integer.parseInt(v));
    }

    public static int getMaxDiagnosticLogIntervalInMs() {
        return getOptionalConfigProperty(
                "COSMOS_MAX_DIAGNOSTICS_LOG_INTERVAL_MS",
                60_000,
                v -> Integer.parseInt(v));
    }

    public static String getAadManagedIdentityId() {
        return getOptionalConfigProperty("AAD_MANAGED_IDENTITY_ID", null, v -> v);
    }

    public static String getAadTenantId() {
        return getOptionalConfigProperty("AAD_TENANT_ID", null, v -> v);
    }

    private static <T> T getOptionalConfigProperty(String name, T defaultValue, Function<String, T> conversion) {
        String textValue = getConfigPropertyOrNull(name);

        if (textValue == null) {
            return defaultValue;
        }

        T returnValue = conversion.apply(textValue);
        return returnValue != null ? returnValue : defaultValue;
    }

    private static <T> T getRequiredConfigProperty(String name, Function<String, T> conversion) {
        String textValue = getConfigPropertyOrNull(name);
        String errorMsg = "The required configuration property '"
                + name
                + "' is not specified. You can do so via system property 'COSMOS."
                + name
                + "' or environment variable 'COSMOS_" + name + "'.";
        if (textValue == null) {
            throw new IllegalStateException(errorMsg);
        }

        T returnValue = conversion.apply(textValue);
        if (returnValue == null) {
            throw new IllegalStateException(errorMsg);
        }
        return returnValue;
    }

    private static String getConfigPropertyOrNull(String name) {
        String systemPropertyName = "COSMOS." + name;
        String environmentVariableName = "COSMOS_" + name;
        String fromSystemProperty = emptyToNull(System.getProperty(systemPropertyName));
        if (fromSystemProperty != null) {
            return fromSystemProperty;
        }

        return emptyToNull(System.getenv().get(environmentVariableName));
    }
}
