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
                "path": "/recordkey",
                "order": "descending"
            }
        ],
        [
            {
                "path": "/messageTimestamp",
                "order": "ascending"
            },
            {
                "path": "/recordkey",
                "order": "ascending"
            }
        ]
    ]
}
```

### Payload Structure (POJO)

```java
package com.tickreader.entity;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Tick {

    private String id;
    private String pk;
    private String ricName;
    private Long messageTimestamp;
    private Long executionTime;
    private Integer msgSequence;
    private Long recordKey;
    private Long collectDatetime;
    private Integer rtlWrap;
    private Long rtl;
    private String subRtl;
    private String ruleSetVersion;
    private String ruleId;
    private String ruleVersionId;
    private String ruleClauseNo;
    private Long sourceDatetime;
    private Double bid;
    private Double bidSize;
    private Double ask;
    private Double askSize;
    private Double midPrice;
    private String dsplyName;
    private Double yldTomat;
    private Double bidYield;
    private Double askYield;
    private String srcRef1;
    private String dlgCode1;
    private String ctbtr1;
    private String ctbLoc1;
    private Double cnvParity;
    private Double premium;
    private Double smpMargin;
    private Double dscMargin;
    private Double impVolt;
    private Double oas;
    private Double delta;
    private Double swapSprd;
    private Double askSpread;
    private Double astSwpspd;
    private Double bidSpread;
    private Double bmkSpd;
    private Double bpv;
    private Double ismaBYld;
    private Double ismaAYld;
    private Double midSpread;
    private Double midYld1;
    private Double cdsBasis;
    private Double bevenInf;
    private Double realYlda;
    private Double realYldb;
    private Double zspread;
    private Double bmkYield;
    private Double cnvEdge;
    private Double fairPrice;
    private Double yldtobest;
    private Double yldtoworst;
    private Double lastQuote;
    private Double bondFloor;
    private Double duration;
    private Double convexity;
    private Double oasBid;
    private Double asp1m;
    private Double asp3m;
    private Double asp6m;
    private Double netchng1;
    private Double askFwdort;
    private Double bidFwdort;
    private Double bondFlr;
    private Double cnvEdge1;
    private Double yldbst;
    private Double yldwst;
    private Double openPrc;
    private Double high1;
    private Double low1;
    private Double openYld;
    private Double highYld;
    private Double lowYld;
    private Double benchPrc;
    private String bkgdRef;
    private Double nrgCrack;
    private Double nrgFrght;
    private Double nrgTop;
    private Double yield;
    private Double intBasis;
    private Double intCds;
    private Double modDurtn;
    private Double swpPoint;
    private Double cleanPrc;
    private Long sourceDatetimeExt;
    private Double cnvPrem;
    private Double cnvRatio;
    private Double currBid;
    private Double currAsk;
    private Double trtnPrice;
    private String exchDate;
    private String exchTime;
    private Double quoteVal;
    private String qteId;
    private Double quoteSize;
    private String isinCdD;
    private String qualifiers;
    private String userQualifiers;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPk() {
        return pk;
    }

    public void setPk(String pk) {
        this.pk = pk;
    }

    public String getRicName() { return ricName; }
    public void setRicName(String ricName) { this.ricName = ricName; }

    public Long getMessageTimestamp() { return messageTimestamp; }
    public void setMessageTimestamp(Long messageTimestamp) { this.messageTimestamp = messageTimestamp; }

    public Long getExecutionTime() { return executionTime; }
    public void setExecutionTime(Long executionTime) { this.executionTime = executionTime; }

    public Integer getMsgSequence() { return msgSequence; }
    public void setMsgSequence(Integer msgSequence) { this.msgSequence = msgSequence; }

    public Long getRecordKey() { return recordKey; }
    public void setRecordKey(Long recordKey) { this.recordKey = recordKey; }

    public Long getCollectDatetime() { return collectDatetime; }
    public void setCollectDatetime(Long collectDatetime) { this.collectDatetime = collectDatetime; }

    public Integer getRtlWrap() { return rtlWrap; }
    public void setRtlWrap(Integer rtlWrap) { this.rtlWrap = rtlWrap; }

    public Long getRtl() { return rtl; }
    public void setRtl(Long rtl) { this.rtl = rtl; }

    public String getSubRtl() { return subRtl; }
    public void setSubRtl(String subRtl) { this.subRtl = subRtl; }

    public String getRuleSetVersion() { return ruleSetVersion; }
    public void setRuleSetVersion(String ruleSetVersion) { this.ruleSetVersion = ruleSetVersion; }

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }

    public String getRuleVersionId() { return ruleVersionId; }
    public void setRuleVersionId(String ruleVersionId) { this.ruleVersionId = ruleVersionId; }

    public String getRuleClauseNo() { return ruleClauseNo; }
    public void setRuleClauseNo(String ruleClauseNo) { this.ruleClauseNo = ruleClauseNo; }

    public Long getSourceDatetime() { return sourceDatetime; }
    public void setSourceDatetime(Long sourceDatetime) { this.sourceDatetime = sourceDatetime; }

    public Double getBid() { return bid; }
    public void setBid(Double bid) { this.bid = bid; }

    public Double getBidSize() { return bidSize; }
    public void setBidSize(Double bidSize) { this.bidSize = bidSize; }

    public Double getAsk() { return ask; }
    public void setAsk(Double ask) { this.ask = ask; }

    public Double getAskSize() { return askSize; }
    public void setAskSize(Double askSize) { this.askSize = askSize; }

    public Double getMidPrice() { return midPrice; }
    public void setMidPrice(Double midPrice) { this.midPrice = midPrice; }

    public String getDsplyName() { return dsplyName; }
    public void setDsplyName(String dsplyName) { this.dsplyName = dsplyName; }

    public Double getYldTomat() { return yldTomat; }
    public void setYldTomat(Double yldTomat) { this.yldTomat = yldTomat; }

    public Double getBidYield() { return bidYield; }
    public void setBidYield(Double bidYield) { this.bidYield = bidYield; }

    public Double getAskYield() { return askYield; }
    public void setAskYield(Double askYield) { this.askYield = askYield; }

    public String getSrcRef1() { return srcRef1; }
    public void setSrcRef1(String srcRef1) { this.srcRef1 = srcRef1; }

    public String getDlgCode1() { return dlgCode1; }
    public void setDlgCode1(String dlgCode1) { this.dlgCode1 = dlgCode1; }

    public String getCtbtr1() { return ctbtr1; }
    public void setCtbtr1(String ctbtr1) { this.ctbtr1 = ctbtr1; }

    public String getCtbLoc1() { return ctbLoc1; }
    public void setCtbLoc1(String ctbLoc1) { this.ctbLoc1 = ctbLoc1; }

    public Double getCnvParity() { return cnvParity; }
    public void setCnvParity(Double cnvParity) { this.cnvParity = cnvParity; }

    public Double getPremium() { return premium; }
    public void setPremium(Double premium) { this.premium = premium; }

    public Double getSmpMargin() { return smpMargin; }
    public void setSmpMargin(Double smpMargin) { this.smpMargin = smpMargin; }

    public Double getDscMargin() { return dscMargin; }
    public void setDscMargin(Double dscMargin) { this.dscMargin = dscMargin; }

    public Double getImpVolt() { return impVolt; }
    public void setImpVolt(Double impVolt) { this.impVolt = impVolt; }

    public Double getOas() { return oas; }
    public void setOas(Double oas) { this.oas = oas; }

    public Double getDelta() { return delta; }
    public void setDelta(Double delta) { this.delta = delta; }

    public Double getSwapSprd() { return swapSprd; }
    public void setSwapSprd(Double swapSprd) { this.swapSprd = swapSprd; }

    public Double getAskSpread() { return askSpread; }
    public void setAskSpread(Double askSpread) { this.askSpread = askSpread; }

    public Double getAstSwpspd() { return astSwpspd; }
    public void setAstSwpspd(Double astSwpspd) { this.astSwpspd = astSwpspd; }

    public Double getBidSpread() { return bidSpread; }
    public void setBidSpread(Double bidSpread) { this.bidSpread = bidSpread; }

    public Double getBmkSpd() { return bmkSpd; }
    public void setBmkSpd(Double bmkSpd) { this.bmkSpd = bmkSpd; }

    public Double getBpv() { return bpv; }
    public void setBpv(Double bpv) { this.bpv = bpv; }

    public Double getIsmaBYld() { return ismaBYld; }
    public void setIsmaBYld(Double ismaBYld) { this.ismaBYld = ismaBYld; }

    public Double getIsmaAYld() { return ismaAYld; }
    public void setIsmaAYld(Double ismaAYld) { this.ismaAYld = ismaAYld; }

    public Double getMidSpread() { return midSpread; }
    public void setMidSpread(Double midSpread) { this.midSpread = midSpread; }

    public Double getMidYld1() { return midYld1; }
    public void setMidYld1(Double midYld1) { this.midYld1 = midYld1; }

    public Double getCdsBasis() { return cdsBasis; }
    public void setCdsBasis(Double cdsBasis) { this.cdsBasis = cdsBasis; }

    public Double getBevenInf() { return bevenInf; }
    public void setBevenInf(Double bevenInf) { this.bevenInf = bevenInf; }

    public Double getRealYlda() { return realYlda; }
    public void setRealYlda(Double realYlda) { this.realYlda = realYlda; }

    public Double getRealYldb() { return realYldb; }
    public void setRealYldb(Double realYldb) { this.realYldb = realYldb; }

    public Double getZspread() { return zspread; }
    public void setZspread(Double zspread) { this.zspread = zspread; }

    public Double getBmkYield() { return bmkYield; }
    public void setBmkYield(Double bmkYield) { this.bmkYield = bmkYield; }

    public Double getCnvEdge() { return cnvEdge; }
    public void setCnvEdge(Double cnvEdge) { this.cnvEdge = cnvEdge; }

    public Double getFairPrice() { return fairPrice; }
    public void setFairPrice(Double fairPrice) { this.fairPrice = fairPrice; }

    public Double getYldtobest() { return yldtobest; }
    public void setYldtobest(Double yldtobest) { this.yldtobest = yldtobest; }

    public Double getYldtoworst() { return yldtoworst; }
    public void setYldtoworst(Double yldtoworst) { this.yldtoworst = yldtoworst; }

    public Double getLastQuote() { return lastQuote; }
    public void setLastQuote(Double lastQuote) { this.lastQuote = lastQuote; }

    public Double getBondFloor() { return bondFloor; }
    public void setBondFloor(Double bondFloor) { this.bondFloor = bondFloor; }

    public Double getDuration() { return duration; }
    public void setDuration(Double duration) { this.duration = duration; }

    public Double getConvexity() { return convexity; }
    public void setConvexity(Double convexity) { this.convexity = convexity; }

    public Double getOasBid() { return oasBid; }
    public void setOasBid(Double oasBid) { this.oasBid = oasBid; }

    public Double getAsp1m() { return asp1m; }
    public void setAsp1m(Double asp1m) { this.asp1m = asp1m; }

    public Double getAsp3m() { return asp3m; }
    public void setAsp3m(Double asp3m) { this.asp3m = asp3m; }

    public Double getAsp6m() { return asp6m; }
    public void setAsp6m(Double asp6m) { this.asp6m = asp6m; }

    public Double getNetchng1() { return netchng1; }
    public void setNetchng1(Double netchng1) { this.netchng1 = netchng1; }

    public Double getAskFwdort() { return askFwdort; }
    public void setAskFwdort(Double askFwdort) { this.askFwdort = askFwdort; }

    public Double getBidFwdort() { return bidFwdort; }
    public void setBidFwdort(Double bidFwdort) { this.bidFwdort = bidFwdort; }

    public Double getBondFlr() { return bondFlr; }
    public void setBondFlr(Double bondFlr) { this.bondFlr = bondFlr; }

    public Double getCnvEdge1() { return cnvEdge1; }
    public void setCnvEdge1(Double cnvEdge1) { this.cnvEdge1 = cnvEdge1; }

    public Double getYldbst() { return yldbst; }
    public void setYldbst(Double yldbst) { this.yldbst = yldbst; }

    public Double getYldwst() { return yldwst; }
    public void setYldwst(Double yldwst) { this.yldwst = yldwst; }

    public Double getOpenPrc() { return openPrc; }
    public void setOpenPrc(Double openPrc) { this.openPrc = openPrc; }

    public Double getHigh1() { return high1; }
    public void setHigh1(Double high1) { this.high1 = high1; }

    public Double getLow1() { return low1; }
    public void setLow1(Double low1) { this.low1 = low1; }

    public Double getOpenYld() { return openYld; }
    public void setOpenYld(Double openYld) { this.openYld = openYld; }

    public Double getHighYld() { return highYld; }
    public void setHighYld(Double highYld) { this.highYld = highYld; }

    public Double getLowYld() { return lowYld; }
    public void setLowYld(Double lowYld) { this.lowYld = lowYld; }

    public Double getBenchPrc() { return benchPrc; }
    public void setBenchPrc(Double benchPrc) { this.benchPrc = benchPrc; }

    public String getBkgdRef() { return bkgdRef; }
    public void setBkgdRef(String bkgdRef) { this.bkgdRef = bkgdRef; }

    public Double getNrgCrack() { return nrgCrack; }
    public void setNrgCrack(Double nrgCrack) { this.nrgCrack = nrgCrack; }

    public Double getNrgFrght() { return nrgFrght; }
    public void setNrgFrght(Double nrgFrght) { this.nrgFrght = nrgFrght; }

    public Double getNrgTop() { return nrgTop; }
    public void setNrgTop(Double nrgTop) { this.nrgTop = nrgTop; }

    public Double getYield() { return yield; }
    public void setYield(Double yield) { this.yield = yield; }

    public Double getIntBasis() { return intBasis; }
    public void setIntBasis(Double intBasis) { this.intBasis = intBasis; }

    public Double getIntCds() { return intCds; }
    public void setIntCds(Double intCds) { this.intCds = intCds; }

    public Double getModDurtn() { return modDurtn; }
    public void setModDurtn(Double modDurtn) { this.modDurtn = modDurtn; }

    public Double getSwpPoint() { return swpPoint; }
    public void setSwpPoint(Double swpPoint) { this.swpPoint = swpPoint; }

    public Double getCleanPrc() { return cleanPrc; }
    public void setCleanPrc(Double cleanPrc) { this.cleanPrc = cleanPrc; }

    public Long getSourceDatetimeExt() { return sourceDatetimeExt; }
    public void setSourceDatetimeExt(Long sourceDatetimeExt) { this.sourceDatetimeExt = sourceDatetimeExt; }

    public Double getCnvPrem() { return cnvPrem; }
    public void setCnvPrem(Double cnvPrem) { this.cnvPrem = cnvPrem; }

    public Double getCnvRatio() { return cnvRatio; }
    public void setCnvRatio(Double cnvRatio) { this.cnvRatio = cnvRatio; }

    public Double getCurrBid() { return currBid; }
    public void setCurrBid(Double currBid) { this.currBid = currBid; }

    public Double getCurrAsk() { return currAsk; }
    public void setCurrAsk(Double currAsk) { this.currAsk = currAsk; }

    public Double getTrtnPrice() { return trtnPrice; }
    public void setTrtnPrice(Double trtnPrice) { this.trtnPrice = trtnPrice; }

    public String getExchDate() { return exchDate; }
    public void setExchDate(String exchDate) { this.exchDate = exchDate; }

    public String getExchTime() { return exchTime; }
    public void setExchTime(String exchTime) { this.exchTime = exchTime; }

    public Double getQuoteVal() { return quoteVal; }
    public void setQuoteVal(Double quoteVal) { this.quoteVal = quoteVal; }

    public String getQteId() { return qteId; }
    public void setQteId(String qteId) { this.qteId = qteId; }

    public Double getQuoteSize() { return quoteSize; }
    public void setQuoteSize(Double quoteSize) { this.quoteSize = quoteSize; }

    public String getIsinCdD() { return isinCdD; }
    public void setIsinCdD(String isinCdD) { this.isinCdD = isinCdD; }

    public String getQualifiers() { return qualifiers; }
    public void setQualifiers(String qualifiers) { this.qualifiers = qualifiers; }

    public String getUserQualifiers() { return userQualifiers; }
    public void setUserQualifiers(String userQualifiers) { this.userQualifiers = userQualifiers; }
}

```

# Running the application

## Setting up the environment

- Ensure you have Java 17 or higher installed.
  - https://learn.microsoft.com/en-us/java/openjdk/download
  - https://learn.microsoft.com/en-us/java/openjdk/install
- Ensure you have Maven installed.
  - https://maven.apache.org/install.html

## AAD Setup

- To assign the data-plane access to the identity of the application, follow the steps in the Azure documentation:
  - https://learn.microsoft.com/en-us/azure/cosmos-db/nosql/how-to-grant-data-plane-access?tabs=built-in-definition%2Ccsharp&pivots=azure-interface-cli
  - The `tick-reader` application uses `DefaultAzureCredential` to authenticate with Azure Cosmos DB, which supports managed identities, environment variables, and other authentication methods. This will do an auto-discovery of the managed identity if not specified.

- Below is a sample command to assign the `Cosmos DB Built-in Data Contributor` role to the managed identity of the application:

```bash
az cosmosdb sql role assignment create --resource-group "<resource-group>" --account-name "<cosmos-db-account-name>" --role-definition-id "00000000-0000-0000-0000-000000000002" --principal-id "<service-principal-id>" --scope "" --subscription "<subscription>"        
```

## Building the application into an executable JAR

```
mvn clean package
```

## Running the application

```
java -jar target/tick-reader-app.jar
```

# Executing requests

A sample request to query tick data for specific RICs and a date range can be made using the following HTTP GET request:

```http request
GET http://localhost:8080/ticks/sort=messageTimestamp&recordKey?rics=AAPL,GOOGL,MSFT&docTypes=TAS,TAQ&totalTicks=10000&pinStart=true&startTime=2024-10-07T00:00:00.0000000Z&endTime=2024-10-07T23:59:59.9999999Z&totalChunks=6
```

## Query Parameters

| Parameter Name | Type    | Required | Example                      | Description                                                                                                             |
|----------------|---------|----------|------------------------------|-------------------------------------------------------------------------------------------------------------------------|
| rics           | String  | Yes      | `MSFT,GOOGL,AAPL`            | Comma-separated list of RICs to query.                                                                                  |
| docTypes       | String  | Yes      | `TAS,TAQ`                    | Comma-separated list of document types to filter by.                                                                    |
| totalTicks     | Integer | Yes      | 10000                        | Total number of ticks to return.                                                                                        |
| pinStart       | Boolean | Yes      | `true`                       | Whether to pin the start time to the first tick.                                                                        |
| startTime      | String  | Yes      | 2024-10-07T00:00:00.0000000Z | Start time in ISO 8601 format.                                                                                          |
| endTime        | String  | Yes      | 2024-10-07T23:59:59.9999999Z | End time in ISO 8601 format.                                                                                            |
| totalChunks    | Integer | Yes      | 6                            | Total number of chunks to return. This is used to determine the number of time windows to run internal queries against. |

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