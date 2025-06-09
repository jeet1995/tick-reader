package com.tickreader.service.impl;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import com.tickreader.config.RicBasedCosmosClientFactory;
import com.tickreader.config.RicCosmosProperties;
import com.tickreader.config.RicGroupMappingContext;
import com.tickreader.config.RicMappingProperties;
import com.tickreader.entity.Tick;
import com.tickreader.service.TicksService;
import com.tickreader.service.strategy.ErrorHandlingStrategy;
import com.tickreader.service.strategy.RetryOnSpecificExceptionStrategy;
import com.tickreader.service.utils.TickServiceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class TicksServiceImpl implements TicksService {

    private final static Logger logger = LoggerFactory.getLogger(TicksServiceImpl.class);

    private final RicBasedCosmosClientFactory clientFactory;
    private final RicMappingProperties ricMappingProperties;
    private final ErrorHandlingStrategy errorHandlingStrategy;

    public TicksServiceImpl(RicBasedCosmosClientFactory clientFactory, RicMappingProperties ricMappingProperties) {
        this.clientFactory = clientFactory;
        this.ricMappingProperties = ricMappingProperties;
        this.errorHandlingStrategy = new RetryOnSpecificExceptionStrategy();
    }

    @Override
    public List<Tick> getTicks(List<String> rics, int totalTicks, boolean pinStart, LocalDateTime startTime, LocalDateTime endTime) {

        LocalDateTime newStartTime, newEndTime;

        newStartTime = startTime.isAfter(endTime) ? endTime : startTime;
        newEndTime = endTime.isBefore(startTime) ? startTime : endTime;

        List<String> datesInBetween
                = TickServiceUtils.getLocalDatesBetweenTwoLocalDateTimes(newStartTime, newEndTime);

        RicCosmosProperties ricCosmosProperties
                = this.clientFactory.getRicCosmosProperties();

        List<TickRequestContext> tickRequestContexts = new ArrayList<>();

        PriorityBlockingQueue<Tick> orderedTicks = new PriorityBlockingQueue<>(100, (t1, t2) -> {

            if (t1.getExecutionTime().equals(t2.getExecutionTime())) {
                return 0;
            }

            if (pinStart) {
                return t1.getExecutionTime() > t2.getExecutionTime() ? -1 : 1;
            } else {
                return t1.getExecutionTime() > t2.getExecutionTime() ? 1 : -1;
            }
        });

        AtomicInteger totalTicksCountWithNonNullContinuation = new AtomicInteger(0);

        for (String ric : rics) {
            for (String date : datesInBetween) {
                RicGroupMappingContext ricGroup = this.ricMappingProperties.getMappedRicGroup(ric);
                CosmosAsyncClient asyncClient = this.clientFactory.getCosmosAsyncClient(ricGroup.getRicGroupId());

                if (asyncClient == null) {
                    logger.warn("CosmosAsyncClient instance not found for ric: {}", ric);
                    continue;
                }

                String databaseId
                        = ricCosmosProperties.getRicCosmosProperties(ricGroup.getRicGroupId()).getDatabaseId();

                if (databaseId == null || databaseId.isEmpty()) {
                    logger.warn("Ric {} and Ric group {} does not have a database ID", ric, ricGroup.getRicGroupId());
                    continue;
                }

                CosmosAsyncContainer asyncContainer = asyncClient.getDatabase(databaseId)
                        .getContainer(date);

                int shardCount = ricCosmosProperties.getShardCount();

                for (int i = 1; i <= shardCount; i++) {

                    String tickIdentifier = TickServiceUtils.constructTickIdentifier(ric, date, i);

                    TickRequestContext tickRequestContext = new TickRequestContext(
                            asyncContainer,
                            tickIdentifier,
                            totalTicksCountWithNonNullContinuation);

                    tickRequestContexts.add(tickRequestContext);
                }
            }
        }

        totalTicksCountWithNonNullContinuation.set(tickRequestContexts.size());

        return executeQueryUntilTopN(
                tickRequestContexts,
                newStartTime,
                newEndTime,
                pinStart,
                orderedTicks,
                totalTicks)
        .blockLast();
    }

    private Flux<List<Tick>> executeQueryUntilTopN(
            List<TickRequestContext> tickRequestContexts,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean pinStart,
            PriorityBlockingQueue<Tick> orderedTicks,
            int totalTicks) {

        Object lock = new Object();

        Flux<Flux<List<Tick>>> generator = Flux.generate(Flux::empty,
                (state, sink) -> {

                    if (tickRequestContexts.isEmpty()) {
                        sink.complete();
                        return state;
                    }

                    if (tickRequestContexts.get(0).getGlobalTickCountWithNonNullContinuationToken() <= 0) {
                        sink.complete();
                        return state;
                    }

                    if (orderedTicks.size() < totalTicks) {
                        Flux<List<Tick>> response = Flux.defer(() -> bufferedAndOrderedFetcher(tickRequestContexts, orderedTicks, startTime, endTime, pinStart, totalTicks, lock));
                        sink.next(response);
                    } else if (orderedTicks.size() == totalTicks) {
                        sink.complete();
                    }

                    return state;
                });

        return generator.flatMapSequential(flux -> flux, 1, 1);
    }

    private SqlQuerySpec getSqlQuerySpec(
            String tickIdentifier,
            String containerId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean pinStart) {

        LocalDate localDate = LocalDate.parse(containerId);

        long queryStartTime = !startTime.isBefore(localDate.atStartOfDay()) ? startTime.toEpochSecond(ZoneOffset.UTC) : localDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        long queryEndTime = !endTime.isAfter(localDate.atTime(23, 59, 59)) ? endTime.toEpochSecond(ZoneOffset.UTC) : localDate.atTime(23, 59, 59).toEpochSecond(ZoneOffset.UTC);

        List<SqlParameter> parameters = new ArrayList<>();

        parameters.add(new SqlParameter("@tickIdentifier", tickIdentifier));
        parameters.add(new SqlParameter("@startTime", queryStartTime));
        parameters.add(new SqlParameter("@endTime", queryEndTime));

        if (pinStart) {
            String query = "SELECT * FROM C WHERE C.pk = @tickIdentifier " +
                    "AND C.messageTimestamp >= @startTime AND C.messageTimestamp <= @endTime ORDER BY C.executionTime DESC";

            return new SqlQuerySpec(query, parameters);
        } else {
            String query = "SELECT * FROM C WHERE C.pk = @tickIdentifier " +
                    "AND C.messageTimestamp >= @startTime AND C.messageTimestamp <= @endTime ORDER BY C.executionTime ASC";

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
                tickRequestContext.getTickIdentifier(),
                asyncContainer.getId(),
                startTime,
                endTime,
                pinStart
        );

        tickRequestContext.setSqlQuerySpec(querySpec);

        String continuationToken = tickRequestContext.getContinuationToken();

        return Flux.defer(() -> asyncContainer
                        .queryItems(querySpec, Tick.class)
                        .byPage(continuationToken, 10))
                .onErrorResume(throwable -> {

                    if (throwable instanceof CosmosException) {
                        CosmosException cosmosException = (CosmosException) throwable;

                        if (TickServiceUtils.isResourceNotFound(cosmosException)) {
                            logger.warn("Ric with associated ID {} does not exist for day : {}", tickRequestContext.getTickIdentifier(), asyncContainer.getId());
                            tickRequestContext.setContinuationToken(null);
                            tickRequestContext.decrementGlobalTickCountWithNonNullContinuationToken();
                            return Flux.empty();
                        }
                    }

                    return Flux.error(throwable);
                })
                .flatMap(page -> {
                    tickRequestContext.setContinuationToken(page.getContinuationToken());

                    if (page.getContinuationToken() == null) {
                        tickRequestContext.decrementGlobalTickCountWithNonNullContinuationToken();
                    }

                    return Flux.fromIterable(page.getResults());
                }, 1, 1);
    }

    private Flux<List<Tick>> bufferedAndOrderedFetcher(
            List<TickRequestContext> tickRequestContexts,
            PriorityBlockingQueue<Tick> orderedTicks,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean pinStart,
            int totalTicks,
            Object lock) {

        List<Flux<Tick>> tickQueryFluxes = tickRequestContexts.stream()
                .map(tickRequestContext -> Flux.defer(() -> executeQuery(tickRequestContext, startTime, endTime, pinStart)))
                .collect(Collectors.toList());

        @SuppressWarnings("unchecked")
        Flux<Tick>[] tickQueryFluxArray = tickQueryFluxes.toArray(new Flux[0]);

        Flux<List<Tick>> sourceFlux = Flux.defer(() -> Flux.mergeComparingDelayError(1, orderedTicks.comparator(), tickQueryFluxArray))
                .buffer(100)
                .flatMap(o -> {

                    if (o != null) {
                        synchronized (lock) {

                            int idx = 0;

                            while (orderedTicks.size() < totalTicks && idx < 100) {
                                orderedTicks.offer(o.get(idx));
                                idx++;
                            }
                        }
                    }

                    return Mono.just(orderedTicks.stream().toList());
                }, 1, 1)
                .take(1);

        return errorHandlingStrategy.apply(sourceFlux);
    }
}
