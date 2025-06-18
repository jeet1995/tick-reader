package com.tickreader.service.impl;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosDiagnosticsContext;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import com.tickreader.config.CosmosDbAccount;
import com.tickreader.config.CosmosDbAccountConfiguration;
import com.tickreader.config.RicBasedCosmosClientFactory;
import com.tickreader.dto.TickResponse;
import com.tickreader.entity.Tick;
import com.tickreader.service.TicksService;
import com.tickreader.service.strategy.ErrorHandlingStrategy;
import com.tickreader.service.strategy.RetryOnSpecificExceptionStrategy;
import com.tickreader.service.utils.TickServiceUtils;
import org.apache.spark.unsafe.hash.Murmur3_x86_32;
import org.apache.spark.unsafe.types.UTF8String;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class TicksServiceImpl implements TicksService {

    private final static Logger logger = LoggerFactory.getLogger(TicksServiceImpl.class);

    private final RicBasedCosmosClientFactory clientFactory;
    private final CosmosDbAccountConfiguration cosmosDbAccountConfiguration;
    private final ErrorHandlingStrategy errorHandlingStrategy;
    private final CosmosQueryRequestOptions queryRequestOptions;

    private final Comparator<Tick> tickComparatorWithPinStartAsTrue = (t1, t2) -> {

        if (t1.getMessageTimestamp().equals(t2.getMessageTimestamp())) {
            return Long.compare(t2.getRecordkey(), t1.getRecordkey());
        }
        return Long.compare(t2.getMessageTimestamp(), t1.getMessageTimestamp());
    };

    private final Comparator<Tick> getTickComparatorWithPinStartAsFalse = (t1, t2) -> {

        if (t1.getMessageTimestamp().equals(t2.getMessageTimestamp())) {
            return Long.compare(t1.getRecordkey(), t2.getRecordkey());
        }
        return Long.compare(t1.getMessageTimestamp(), t2.getMessageTimestamp());
    };


    public TicksServiceImpl(RicBasedCosmosClientFactory clientFactory, CosmosDbAccountConfiguration cosmosDbAccountConfiguration) {
        this.clientFactory = clientFactory;
        this.cosmosDbAccountConfiguration = cosmosDbAccountConfiguration;
        this.errorHandlingStrategy = new RetryOnSpecificExceptionStrategy();
        this.queryRequestOptions = new CosmosQueryRequestOptions().setMaxDegreeOfParallelism(3000);
    }

    @Override
    public TickResponse getTicks(
            List<String> rics,
            List<String> docTypes,
            int totalTicks,
            boolean pinStart,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        LocalDateTime newStartTime, newEndTime;

        newStartTime = startTime.isAfter(endTime) ? endTime : startTime;
        newEndTime = endTime.isBefore(startTime) ? startTime : endTime;

        Map<String, TickRequestContext> tickRequestContexts = new HashMap<>();

        ConcurrentLinkedQueue<Tick> orderedTicks = new ConcurrentLinkedQueue<>();

        for (String ric : rics) {

            int seed = 42;
            UTF8String s = UTF8String.fromString(ric);
            int hash = Murmur3_x86_32.hashUnsafeBytes(s.getBaseObject(), s.getBaseOffset(), s.numBytes(), seed);
            int hashIdForRic = Math.abs(hash) % this.cosmosDbAccountConfiguration.getAccountCount() + 1;

            CosmosDbAccount cosmosDbAccount
                    = this.cosmosDbAccountConfiguration.getCosmosDbAccount(hashIdForRic);

            String dateFormat = cosmosDbAccount.getContainerNameFormat();

            List<String> datesInBetween
                    = TickServiceUtils.getLocalDatesBetweenTwoLocalDateTimes(newStartTime, newEndTime, dateFormat);

            for (String date : datesInBetween) {
                CosmosAsyncClient asyncClient = this.clientFactory.getCosmosAsyncClient(hashIdForRic);

                if (asyncClient == null) {
                    logger.warn("CosmosAsyncClient instance not found for ric: {}", ric);
                    continue;
                }

                String databaseName = cosmosDbAccount.getDatabaseName();

                if (databaseName == null || databaseName.isEmpty()) {
                    logger.warn("Ric {} is not assigned to a database", ric);
                    continue;
                }

                CosmosAsyncContainer asyncContainer = asyncClient.getDatabase(databaseName)
                        .getContainer(cosmosDbAccount.getContainerNamePrefix() + date + cosmosDbAccount.getContainerNameSuffix());

                String tickIdentifier = TickServiceUtils.constructTickIdentifierPrefix(ric, date);

                tickRequestContexts.putIfAbsent((hashIdForRic + date), new TickRequestContext(
                        asyncContainer,
                        new ArrayList<>(),
                        date,
                        dateFormat));

                TickRequestContext tickRequestContext = tickRequestContexts.get((hashIdForRic + date));
                List<String> tickIdentifiers = tickRequestContext.getTickIdentifiers();

                tickIdentifiers.add(tickIdentifier);
            }
        }

        return executeQueryUntilTopN(
                tickRequestContexts,
                docTypes,
                newStartTime,
                newEndTime,
                pinStart,
                orderedTicks,
                totalTicks);
    }

    private TickResponse executeQueryUntilTopN(
            Map<String, TickRequestContext> tickRequestContexts,
            List<String> docTypes,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean pinStart,
            ConcurrentLinkedQueue<Tick> orderedTicks,
            int totalTicks) {

        AtomicReference<Instant> executionStartTime = new AtomicReference<>(Instant.MIN);
        AtomicReference<Instant> executionEndTime = new AtomicReference<>(Instant.MAX);

        ConcurrentLinkedQueue<Tick> ticksOrdered = bufferedAndOrderedFetcher(
                tickRequestContexts, docTypes, orderedTicks, startTime, endTime, pinStart, totalTicks)
                .doOnSubscribe(subscription -> {
                    executionStartTime.set(Instant.now());
                })
                .doOnComplete(() -> {
                    executionEndTime.set(Instant.now());
                })
                .blockLast();

        List<CosmosDiagnosticsContext> diagnosticsContexts = new ArrayList<>();

        for (TickRequestContext tickRequestContext : tickRequestContexts.values()) {
            diagnosticsContexts.addAll(tickRequestContext.getDiagnosticsContexts());
        }

        List<Tick> ticks = ticksOrdered == null ? new ArrayList<>() : new ArrayList<>(ticksOrdered);

        return new TickResponse(
                ticks,
                diagnosticsContexts,
                Duration.between(executionStartTime.get(), executionEndTime.get()));
    }

    private SqlQuerySpec getSqlQuerySpec(
            List<String> tickIdentifiers,
            List<String> docTypes,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String localDateAsString,
            String format,
            boolean pinStart) {

        LocalDate localDate = LocalDate.parse(localDateAsString, DateTimeFormatter.ofPattern(format));

        long queryStartTime = !startTime.isBefore(localDate.atStartOfDay()) ? startTime.toInstant(ZoneOffset.UTC).toEpochMilli() * 1_000_000L : localDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() * 1_000_000L;
        long queryEndTime = !endTime.isAfter(localDate.atTime(23, 59, 59, 999_999_999)) ? endTime.toInstant(ZoneOffset.UTC).toEpochMilli() * 1_000_000L : localDate.atTime(23, 59, 59, 999_999_999).toInstant(ZoneOffset.UTC).toEpochMilli() * 1_000_000L;

        List<SqlParameter> parameters = new ArrayList<>();

        StringBuilder sb = new StringBuilder("(");

        int shardCount = this.cosmosDbAccountConfiguration.getShardCountPerRic();

        int maxParamId = tickIdentifiers.size() * shardCount;

        for (int i = 1; i <= maxParamId; i++) {

            String param = "@pk" + i;
            sb.append(param);

            String tickIdentifier = tickIdentifiers.get((i - 1) / shardCount);

            parameters.add(new SqlParameter(param, tickIdentifier + "|" + ((i % shardCount) + 1)));

            if (i < maxParamId) {
                sb.append(", ");
            }
        }

        sb.append(")");

        parameters.add(new SqlParameter("@startTime", queryStartTime));
        parameters.add(new SqlParameter("@endTime", queryEndTime));

        StringBuilder docTypePlaceholders = new StringBuilder();

        docTypePlaceholders.append("(");

        for (int i = 0; i < docTypes.size(); i++) {

            String param = "@docType" + i;

            parameters.add(new SqlParameter(param, docTypes.get(i)));

            docTypePlaceholders.append(param);

            if (i < docTypes.size() - 1) {
                docTypePlaceholders.append(", ");
            }
        }

        docTypePlaceholders.append(")");

        if (pinStart) {
            String query = "SELECT * FROM C WHERE C.pk IN " + sb + " AND C.docType IN " + docTypePlaceholders +
                    " AND C.messageTimestamp >= @startTime AND C.messageTimestamp <= @endTime ORDER BY C.messageTimestamp DESC, C.recordkey DESC OFFSET 0 LIMIT 10000";

            return new SqlQuerySpec(query, parameters);
        } else {
            String query = "SELECT * FROM C WHERE C.pk IN " + sb + " AND C.docType IN " + docTypePlaceholders +
                    " AND C.messageTimestamp >= @startTime AND C.messageTimestamp <= @endTime ORDER BY C.messageTimestamp ASC, C.recordkey ASC OFFSET 0 LIMIT 10000";

            return new SqlQuerySpec(query, parameters);
        }
    }

    private Flux<Tick> executeQuery(
            TickRequestContext tickRequestContext,
            List<String> docTypes,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean pinStart) {

        CosmosAsyncContainer asyncContainer = tickRequestContext.getAsyncContainer();

        SqlQuerySpec querySpec = tickRequestContext.getSqlQuerySpec() != null ? tickRequestContext.getSqlQuerySpec() : getSqlQuerySpec(
                tickRequestContext.getTickIdentifiers(),
                docTypes,
                startTime,
                endTime,
                tickRequestContext.getRequestDateAsString(),
                tickRequestContext.getDateFormat(),
                pinStart
        );

        tickRequestContext.setSqlQuerySpec(querySpec);

        String continuationToken = tickRequestContext.getContinuationToken();

        if (continuationToken != null && continuationToken.equals("drained")) {
            return Flux.empty();
        }

        return Flux.defer(() -> asyncContainer
                        .queryItems(querySpec, this.queryRequestOptions, Tick.class)
                        .byPage(continuationToken, 10000))
                .onErrorResume(throwable -> {

                    if (throwable instanceof CosmosException) {
                        CosmosException cosmosException = (CosmosException) throwable;

                        if (TickServiceUtils.isResourceNotFound(cosmosException)) {
                            logger.warn("Day : {} does not have any records!", asyncContainer.getId());
                            tickRequestContext.setContinuationToken("drained");
                            return Flux.empty();
                        }
                    }

                    return Flux.error(throwable);
                })
                .flatMap(page -> {
                    tickRequestContext.setContinuationToken(page.getContinuationToken());

                    if (page.getCosmosDiagnostics() != null) {
                        tickRequestContext.addDiagnosticsContext(page.getCosmosDiagnostics().getDiagnosticsContext());
                    }

                    if (page.getContinuationToken() == null) {
                        tickRequestContext.setContinuationToken("drained");
                    }

                    return Flux.fromIterable(page.getResults());
                });
    }

    private Flux<ConcurrentLinkedQueue<Tick>> bufferedAndOrderedFetcher(
            Map<String, TickRequestContext> tickRequestContexts,
            List<String> docTypes,
            ConcurrentLinkedQueue<Tick> orderedTicks,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean pinStart,
            int totalTicks) {

        List<Flux<Tick>> tickQueryFluxes = tickRequestContexts.values().stream()
                .map(tickRequestContext -> Flux.defer(() -> executeQuery(tickRequestContext, docTypes, startTime, endTime, pinStart)))
                .collect(Collectors.toList());

        Comparator<Tick> comparator = pinStart ? tickComparatorWithPinStartAsTrue : getTickComparatorWithPinStartAsFalse;

        @SuppressWarnings("unchecked")
        Flux<Tick>[] tickQueryFluxArray = tickQueryFluxes.toArray(new Flux[0]);

        Flux<ConcurrentLinkedQueue<Tick>> sourceFlux = Flux.defer(() -> Flux.mergeComparingDelayError(10000, comparator, tickQueryFluxArray))
                .flatMap(o -> {

                    if (o != null && orderedTicks.size() < totalTicks) {
                        orderedTicks.offer(o);

                    }
                    return Mono.just(orderedTicks);
                });

        return errorHandlingStrategy.apply(sourceFlux);
    }
}
