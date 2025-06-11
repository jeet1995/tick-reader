# tick-reader

A Spring Boot application that queries tick data given list of RICs and date range.

# Configuration

## System Properties and Environment Variables

| System Property Name           | Environment Variable Name      | Default Value | Description                        |
|--------------------------------|--------------------------------|---------------|------------------------------------|
| COSMOS.CONNECTION_MODE         | COSMOS_CONNECTION_MODE         | LOW           | `GATEWAY`                          | The  connection mode which should be used when connecting to Cosmo DB. The possible values are `GATEWAY` (default) or `DIRECT`. The default value should be sufficient for this workload.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| COSMOS.AAD_MANAGED_IDENTITY_ID | COSMOS_AAD_MANAGED_IDENTITY_ID | HIGH          | Auto-Discovery                     | The client-id of the managed identity to be used - if not specified picks one based on DefaultAzureCredential logic - if specified, it will always use that identity.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| COSMOS.AAD_TENANT_ID           | COSMOS_AAD_TENANT_ID           | HIGH          | Auto-Discovery                     | The AAD tenant id for the Azure resources and identities.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| COSMOS.AAD_LOGIN_ENDPOINT      | COSMOS_AAD_LOGIN_ENDPOINT      | LOW           | https://login.microsoftonline.com/ | Only needs to be modified in non-public Azure clouds.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |

## application.yml

Configure accounts and database names for Cosmos DB in `application.yml`:

An example configuration for two Cosmos DB accounts (count can vary) with different database names and URIs:

```yaml
ticks:
  cosmosDbAccounts:
    1:
      databaseName: ${DATABASE_NAME_HASH_1}
      accountUri: ${COSMOSDB_ACCOUNT_URI_HASH_1}
      containerNamePrefix: "container_"
      containerNameFormat: "yyyyMMdd"
      containerNameSuffix: "y"
      hashId: 1
    2:
      databaseName: ${DATABASE_NAME_HASH_2}
      accountUri: ${COSMOSDB_ACCOUNT_URI_HASH_2}
      containerNamePrefix: "container_"
      containerNameFormat: "yyyyMMdd"
      containerNameSuffix: "y"
      hashId: 2
  shardCountPerRic: 8
```

## Container Setup

- Ensure that the Cosmos DB containers are set up with the name `container_<date><suffix>` where `<date>` is in the format `yyyyMMdd` and the `<suffix>` is `y`. For example, for the date `2024-10-08`, the container name would be `container_20241008y`.
- Ensure that the containers are partitioned by `pk` (partition key) and composite indexed on `messageTimestamp` and `recordKey` for efficient sorting based on both these properties.

### Composite Index Setup

```json
{
    "indexingMode": "consistent",
    "automatic": true,
    "includedPaths": [
        {
            "path": "/*"
        }
    ],
    "excludedPaths": [
        {
            "path": "/\"_etag\"/?"
        }
    ],
    "fullTextIndexes": [],
    "compositeIndexes": [
        [
            {
                "path": "/messageTimestamp",
                "order": "descending"
            },
            {
                "path": "/recordKey",
                "order": "descending"
            }
        ],
        [
            {
                "path": "/messageTimestamp",
                "order": "ascending"
            },
            {
                "path": "/recordKey",
                "order": "ascending"
            }
        ]
    ]
}
```

# Running the application

## Building the application into an executable JAR

```
mvn clean package
```

## Running the application

```
java -jar cp target/tick-reader-app.jar
```

# Executing requests

A sample request to query tick data for specific RICs and a date range can be made using the following HTTP GET request:

```http request
GET http://localhost:8080/ticks/sort=messageTimestamp&recordKey?rics=AAPL,GOOGL,MSFT&totalTicks=500&pinStart=true&startTime=2024-10-08T08:00:00.0000000Z&endTime=2024-10-08T08:19:59.9999999Z
```

## Response

A sample response will look like below:

```json
{
  "ticks": [
    {
      "id": "b1d19bdb-dc8b-47db-a2ab-e46827e66fbd",
      "pk": "AAPL|20241008|6",
      "ricName": "AAPL",
      "messageTimestamp": 1728375596311321505,
      "executionTime": 8504,
      "recordKey": 574062
    }
  ],
  "diagnosticsContexts": [ "..." ],
  "executionTime": "PT0.8935116S"
}
```