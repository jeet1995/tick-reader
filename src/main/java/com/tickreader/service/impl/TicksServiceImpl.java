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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class TicksServiceImpl implements TicksService {

    private final static Logger logger = LoggerFactory.getLogger(TicksServiceImpl.class);

    private final RicBasedCosmosClientFactory clientFactory;
    private final CosmosDbAccountConfiguration cosmosDbAccountConfiguration;
    private final ErrorHandlingStrategy errorHandlingStrategy;
    private final CosmosQueryRequestOptions queryRequestOptions;

    private final Comparator<Tick> tickComparatorMessageTimestampAscending = (t1, t2) -> {

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
            LocalDateTime endTime,
            int totalChunks) {

        LocalDateTime newStartTime, newEndTime;

        newStartTime = startTime.isAfter(endTime) ? endTime : startTime;
        newEndTime = endTime.isBefore(startTime) ? startTime : endTime;

        Map<String, TickRequestContext> tickRequestContexts = new HashMap<>();

        PriorityBlockingQueue<Tick> orderedTicks = new PriorityBlockingQueue<>(10_000, (t1, t2) -> {

            if (pinStart) {
                if (t1.getMessageTimestamp().equals(t2.getMessageTimestamp())) {
                    return Long.compare(t1.getRecordkey(), t2.getRecordkey());
                } else {
                    return Long.compare(t1.getMessageTimestamp(), t2.getMessageTimestamp());
                }
            } else {
                if (t1.getMessageTimestamp().equals(t2.getMessageTimestamp())) {
                    return Long.compare(t2.getRecordkey(), t1.getRecordkey());
                } else {
                    return Long.compare(t2.getMessageTimestamp(), t1.getMessageTimestamp());
                }
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
                totalTicks,
                totalChunks);
    }

    private TickResponse executeQueryUntilTopN(
            Map<String, TickRequestContext> tickRequestContexts,
            List<String> docTypes,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean pinStart,
            PriorityBlockingQueue<Tick> orderedTicks,
            int totalTicks,
            int totalChunks) {

        AtomicReference<Instant> executionStartTime = new AtomicReference<>(Instant.MIN);
        AtomicReference<Instant> executionEndTime = new AtomicReference<>(Instant.MAX);
        Object lock = new Object();

        long startEpoch = startTime.toInstant(ZoneOffset.UTC).toEpochMilli() * 1_000_000L;
        long endEpoch = endTime.toInstant(ZoneOffset.UTC).toEpochMilli() * 1_000_000L;

        List<TimeChunk> timeChunks = TickServiceUtils.generateTimeChunks(startEpoch, endEpoch, totalChunks);

        executionStartTime.set(Instant.now());

        if (pinStart) {
            for (int i = timeChunks.size() - 1; i >= 0; i--) {
                tickRequestContexts.values().forEach(TickRequestContext::resetContinuationToken);
                bufferedAndOrderedFetcher(tickRequestContexts, docTypes, orderedTicks, timeChunks.get(i), pinStart, totalTicks, lock).blockLast();

                synchronized (lock) {
                    if (orderedTicks.size() == totalTicks) {
                        break;
                    }
                }
            }
        } else {
            for (int i = 0; i < timeChunks.size(); i++) {
                tickRequestContexts.values().forEach(TickRequestContext::resetContinuationToken);
                bufferedAndOrderedFetcher(tickRequestContexts, docTypes, orderedTicks, timeChunks.get(i), pinStart, totalTicks, lock).blockLast();

                synchronized (lock) {
                    if (orderedTicks.size() == totalTicks) {
                        break;
                    }
                }
            }
        }

        List<CosmosDiagnosticsContext> diagnosticsContexts = new ArrayList<>();

        for (TickRequestContext tickRequestContext : tickRequestContexts.values()) {
            diagnosticsContexts.addAll(tickRequestContext.getDiagnosticsContexts());
        }

        List<Tick> ticks = new ArrayList<>();

        while (!orderedTicks.isEmpty()) {
            ticks.add(orderedTicks.poll());
        }

        Collections.reverse(ticks);

        executionEndTime.set(Instant.now());

        return new TickResponse(
                ticks,
                diagnosticsContexts,
                Duration.between(executionStartTime.get(), executionEndTime.get()));
    }

    private SqlQuerySpec getSqlQuerySpec(
            List<String> tickIdentifiers,
            List<String> docTypes,
            TimeChunk timeChunk,
            String localDateAsString,
            String format,
            boolean pinStart) {

        LocalDate localDate = LocalDate.parse(localDateAsString, DateTimeFormatter.ofPattern(format));

        if (!TickServiceUtils.isEpochChunkPartOfDay(timeChunk, localDate)) {
            return null;
        }

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

        parameters.add(new SqlParameter("@startTime", timeChunk.getChunkEpochStart()));
        parameters.add(new SqlParameter("@endTime", timeChunk.getChunkEpochEnd()));

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
            String query = "SELECT * FROM C WHERE C.pk IN " + sb + " AND C.docType IN " + docTypePlaceholders + " AND C.messageTimestamp >= @startTime AND C.messageTimestamp < @endTime";

            logger.info("Executing query with pinStart as true: {}", query);

            return new SqlQuerySpec(query, parameters);
        } else {
            String query = "SELECT * FROM C WHERE C.pk IN " + sb + " AND C.docType IN " + docTypePlaceholders + " AND C.messageTimestamp >= @startTime AND C.messageTimestamp < @endTime";

            return new SqlQuerySpec(query, parameters);
        }
    }

    private Flux<Tick> executeQuery(
            TickRequestContext tickRequestContext,
            List<String> docTypes,
            TimeChunk timeChunk,
            boolean pinStart) {

        CosmosAsyncContainer asyncContainer = tickRequestContext.getAsyncContainer();

        SqlQuerySpec querySpec = getSqlQuerySpec(
                tickRequestContext.getTickIdentifiers(),
                docTypes,
                timeChunk,
                tickRequestContext.getRequestDateAsString(),
                tickRequestContext.getDateFormat(),
                pinStart
        );

        String continuationToken = tickRequestContext.getContinuationToken();

        if (continuationToken != null && continuationToken.equals("drained")) {
            return Flux.empty();
        }

        if (querySpec == null) {
            logger.warn("Query spec is null for date: {} and tick identifiers: {}", tickRequestContext.getRequestDateAsString(), tickRequestContext.getTickIdentifiers());
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

                    logger.info("SQL Query Executed : {}", querySpec.getQueryText());

                    tickRequestContext.setContinuationToken(page.getContinuationToken());

                    if (page.getCosmosDiagnostics() != null) {
                        tickRequestContext.addDiagnosticsContext(page.getCosmosDiagnostics().getDiagnosticsContext());
                    }

                    if (page.getContinuationToken() == null) {
                        tickRequestContext.setContinuationToken("drained");
                    }

                    return Flux.fromIterable(page.getResults());
                })
                .doOnSubscribe(subscription -> {
                    logger.info("Executing actual query: {}", querySpec.getQueryText());
                });
    }

    private Flux<PriorityBlockingQueue<Tick>> bufferedAndOrderedFetcher(
            Map<String, TickRequestContext> tickRequestContexts,
            List<String> docTypes,
            PriorityBlockingQueue<Tick> orderedTicks,
            TimeChunk timeChunk,
            boolean pinStart,
            int totalTicks,
            Object lock) {

        List<Flux<Tick>> tickQueryFluxes = tickRequestContexts.values().stream()
                .map(tickRequestContext -> Flux.defer(() -> executeQuery(tickRequestContext, docTypes, timeChunk, pinStart)))
                .collect(Collectors.toList());

        @SuppressWarnings("unchecked")
        Flux<Tick>[] tickQueryFluxArray = tickQueryFluxes.toArray(new Flux[0]);

        Flux<PriorityBlockingQueue<Tick>> sourceFlux = Flux.defer(() -> Flux.mergeSequential(tickQueryFluxArray))
                .flatMap(o -> {

                    synchronized (lock) {
                        if (orderedTicks.size() < totalTicks) {
                            orderedTicks.offer(o);
                        } else {
                            if (pinStart) {

                                // lowest needs to be removed
                                // orderedTicks will poll / peek in ascending order
                                // if incoming tick is higher than the lowest, replace it
                                if (tickComparatorMessageTimestampAscending.compare(o, orderedTicks.peek()) > 0) {
                                    orderedTicks.poll();
                                    orderedTicks.offer(o);
                                }
                            } else {
                                // highest needs to be removed
                                // orderedTicks will poll / peek in descending order
                                // if incoming tick is lower than the highest, replace it
                                if (tickComparatorMessageTimestampAscending.compare(o, orderedTicks.peek()) < 0) {
                                    orderedTicks.poll();
                                    orderedTicks.offer(o);
                                }
                            }
                        }
                    }

                    return Mono.just(orderedTicks);
                });

        return errorHandlingStrategy.apply(sourceFlux);
    }
}
