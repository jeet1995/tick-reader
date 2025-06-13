package com.tickreader.service.impl;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosDiagnosticsContext;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.implementation.Configs;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class TicksServiceImpl implements TicksService {

    private final static Logger logger = LoggerFactory.getLogger(TicksServiceImpl.class);

    private final RicBasedCosmosClientFactory clientFactory;
    private final CosmosDbAccountConfiguration cosmosDbAccountConfiguration;
    private final ErrorHandlingStrategy errorHandlingStrategy;
    private final AtomicInteger prefetch = new AtomicInteger(20);
    private final AtomicInteger pageSize = new AtomicInteger(1000);
    private final AtomicInteger concurrency = new AtomicInteger(Configs.getCPUCnt());

    public TicksServiceImpl(RicBasedCosmosClientFactory clientFactory, CosmosDbAccountConfiguration cosmosDbAccountConfiguration) {
        this.clientFactory = clientFactory;
        this.cosmosDbAccountConfiguration = cosmosDbAccountConfiguration;
        this.errorHandlingStrategy = new RetryOnSpecificExceptionStrategy();
    }

    @Override
    public TickResponse getTicks(
            List<String> rics,
            int totalTicks,
            boolean pinStart,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        LocalDateTime newStartTime, newEndTime;

        newStartTime = startTime.isAfter(endTime) ? endTime : startTime;
        newEndTime = endTime.isBefore(startTime) ? startTime : endTime;

        Map<Integer, TickRequestContext> tickRequestContexts = new HashMap<>();

        PriorityBlockingQueue<Tick> orderedTicks = new PriorityBlockingQueue<>(100, (t1, t2) -> {

            if (t1.getMessageTimestamp().equals(t2.getMessageTimestamp())) {

                if (t1.getRecordKey().equals(t2.getRecordKey())) {
                    return 0;
                }

                if (pinStart) {
                    return t1.getRecordKey() > t2.getRecordKey() ? -1 : 1;
                } else {
                    return t1.getRecordKey() > t2.getRecordKey() ? 1 : -1;
                }
            }

            if (pinStart) {
                return t1.getMessageTimestamp() > t2.getMessageTimestamp() ? -1 : 1;
            } else {
                return t1.getMessageTimestamp() > t2.getMessageTimestamp() ? 1 : -1;
            }
        });

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

                tickRequestContexts.putIfAbsent(hashIdForRic, new TickRequestContext(
                        asyncContainer,
                        new ArrayList<>(),
                        date,
                        dateFormat));

                TickRequestContext tickRequestContext = tickRequestContexts.get(hashIdForRic);
                List<String> tickIdentifiers = tickRequestContext.getTickIdentifiers();

                tickIdentifiers.add(tickIdentifier);
            }
        }

        return executeQueryUntilTopN(
                tickRequestContexts,
                newStartTime,
                newEndTime,
                pinStart,
                orderedTicks,
                totalTicks);
    }

    private TickResponse executeQueryUntilTopN(
            Map<Integer, TickRequestContext> tickRequestContexts,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean pinStart,
            PriorityBlockingQueue<Tick> orderedTicks,
            int totalTicks) {

        Sinks.Many<PriorityBlockingQueue<Tick>> sink = Sinks.many().multicast().onBackpressureBuffer();

        AtomicReference<Instant> executionStartTime = new AtomicReference<>(Instant.MIN);
        AtomicReference<Instant> executionEndTime = new AtomicReference<>(Instant.MAX);

        Flux<Object> dataFlux = Flux.defer(() -> bufferedAndOrderedFetcher(
                tickRequestContexts, orderedTicks, startTime, endTime, pinStart, totalTicks))
                .flatMap(ticks -> {

                    if (tickRequestContexts.isEmpty()) {
                        sink.tryEmitComplete();
                        return Flux.empty();
                    }

                    if (orderedTicks.size() < totalTicks) {
                        logger.debug("Time now : {}, totalTicks: {}, orderedTicks.size(): {}", Instant.now(), totalTicks, orderedTicks.size());
                        sink.tryEmitNext(ticks);
                    } else {
                        sink.tryEmitNext(ticks);
                        sink.tryEmitComplete();
                    }

                    return Flux.empty();
                }, concurrency.get(), prefetch.get())
                .doOnTerminate(sink::tryEmitComplete)
                .doOnComplete(sink::tryEmitComplete);

        AtomicReference<Disposable> queryFluxRef = new AtomicReference<>();

        PriorityBlockingQueue<Tick> ticksPq = sink
                .asFlux()
                .publishOn(Schedulers.boundedElastic())
                .doOnSubscribe(subscription -> {
                    logger.debug("Subscription started for fetching ticks with totalTicks: {}", totalTicks);
                    queryFluxRef.set(dataFlux.subscribe());
                    executionStartTime.set(Instant.now());
                    logger.info("Execution started at: {}", executionStartTime.get());
                })
                .doOnError(throwable -> {
                    logger.error("Error occurred while fetching ticks: {}", throwable.getMessage(), throwable);
                    queryFluxRef.get().dispose();
                    executionEndTime.set(Instant.now());
                })
                .doOnComplete(() -> {
                    queryFluxRef.get().dispose();
                    executionEndTime.set(Instant.now());
                    logger.debug("Flux terminated, disposed of the subscription.");
                    logger.info("Execution ended at: {}", executionEndTime.get());
                }).subscribeOn(Schedulers.boundedElastic()).blockLast();

        List<CosmosDiagnosticsContext> diagnosticsContexts = new ArrayList<>();

        for (TickRequestContext tickRequestContext : tickRequestContexts.values()) {
            diagnosticsContexts.addAll(tickRequestContext.getDiagnosticsContexts());
        }

        List<Tick> ticks = ticksPq == null ? new ArrayList<>() : new ArrayList<>(ticksPq);

        return new TickResponse(
                ticks,
                diagnosticsContexts,
                Duration.between(executionStartTime.get(), executionEndTime.get()));
    }

    private SqlQuerySpec getSqlQuerySpec(
            List<String> tickIdentifiers,
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

        if (pinStart) {
            String query = "SELECT * FROM C WHERE C.pk IN " + sb +
                    " AND C.messageTimestamp >= @startTime AND C.messageTimestamp <= @endTime ORDER BY C.messageTimestamp DESC";

            return new SqlQuerySpec(query, parameters);
        } else {
            String query = "SELECT * FROM C WHERE C.pk IN " + sb +
                    " AND C.messageTimestamp >= @startTime AND C.messageTimestamp <= @endTime ORDER BY C.messageTimestamp ASC";

            return new SqlQuerySpec(query, parameters);
        }
    }

    private Flux<Tick> executeQuery(
            TickRequestContext tickRequestContext,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean pinStart) {

        CosmosAsyncContainer asyncContainer = tickRequestContext.getAsyncContainer();

        SqlQuerySpec querySpec = tickRequestContext.getSqlQuerySpec() != null ? tickRequestContext.getSqlQuerySpec() : getSqlQuerySpec(
                tickRequestContext.getTickIdentifiers(),
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
                        .queryItems(querySpec, Tick.class)
                        .byPage(continuationToken, pageSize.get()))
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
                }, concurrency.get(), prefetch.get());
    }

    private Flux<PriorityBlockingQueue<Tick>> bufferedAndOrderedFetcher(
            Map<Integer, TickRequestContext> tickRequestContexts,
            PriorityBlockingQueue<Tick> orderedTicks,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean pinStart,
            int totalTicks) {

        List<Flux<Tick>> tickQueryFluxes = tickRequestContexts.values().stream()
                .map(tickRequestContext -> Flux.defer(() -> executeQuery(tickRequestContext, startTime, endTime, pinStart)))
                .collect(Collectors.toList());

        @SuppressWarnings("unchecked")
        Flux<Tick>[] tickQueryFluxArray = tickQueryFluxes.toArray(new Flux[0]);

        Flux<PriorityBlockingQueue<Tick>> sourceFlux = Flux.defer(() -> Flux.mergeComparingDelayError(prefetch.get(), orderedTicks.comparator(), tickQueryFluxArray))
                .flatMap(o -> {

                    if (o != null && orderedTicks.size() < totalTicks) {
                        orderedTicks.offer(o);

                    }
                    return Mono.just(orderedTicks);
                }, concurrency.get(), prefetch.get());

        return errorHandlingStrategy.apply(sourceFlux);
    }
}
