package com.tickreader.service.impl;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import com.tickreader.config.RicBasedCosmosClientFactory;
import com.tickreader.config.RicCosmosProperties;
import com.tickreader.config.RicMappingProperties;
import com.tickreader.entity.Tick;
import com.tickreader.service.TicksService;
import com.tickreader.service.utils.TickServiceUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

@Service
public class TicksServiceImpl implements TicksService {

    private final RicBasedCosmosClientFactory clientFactory;
    private final RicMappingProperties ricMappingProperties;

    public TicksServiceImpl(RicBasedCosmosClientFactory clientFactory, RicMappingProperties ricMappingProperties) {
        this.clientFactory = clientFactory;
        this.ricMappingProperties = ricMappingProperties;
    }

    @Override
    public List<Tick> getTicks(List<String> rics, int totalTicks, boolean pinStart, LocalDateTime startTime, LocalDateTime endTime) {

        List<String> datesInBetween
                = TickServiceUtils.getLocalDatesBetweenTwoLocalDateTimes(startTime, endTime);

        RicCosmosProperties ricCosmosProperties
                = this.clientFactory.getRicCosmosProperties();

        List<TickRequestContext> tickRequestContexts = new ArrayList<>();

        PriorityQueue<Tick> orderedTicks = new PriorityQueue<>((t1, t2) -> {

            if (t1.getMessageTimestamp() == t2.getMessageTimestamp()) {
                return 0;
            }

            if (pinStart) {
                return t2.getMessageTimestamp() < t1.getMessageTimestamp() ? 1 : -1;
            } else {
                return t2.getMessageTimestamp() < t1.getMessageTimestamp() ? -1 : 1;
            }
        });

        for (String ric : rics) {
            for (String date : datesInBetween) {
                String ricGroup = this.ricMappingProperties.getMappedRicGroup(ric);
                CosmosAsyncClient asyncClient = this.clientFactory.getCosmosAsyncClient(ricGroup);

                if (asyncClient == null) {
                    continue;
                }

                String databaseId
                        = ricCosmosProperties.getRicCosmosProperties(ricGroup).getDatabaseId();

                if (databaseId == null || databaseId.isEmpty()) {
                    continue;
                }

                CosmosAsyncContainer asyncContainer = asyncClient.getDatabase(databaseId)
                        .getContainer(date);

                int shardCount = ricCosmosProperties.getShardCount();

                for (int i = 1; i <= shardCount; i++) {

                    String tickIdentifier = TickServiceUtils.constructTickIdentifier(ric, date, i);

                    TickRequestContext tickRequestContext = new TickRequestContext(
                            asyncContainer, tickIdentifier);

                    tickRequestContexts.add(tickRequestContext);
                }
            }
        }

        executeQueryUntilTopN(tickRequestContexts, startTime, endTime, pinStart, orderedTicks, totalTicks).blockFirst();

        return orderedTicks.stream().limit(totalTicks).collect(Collectors.toList());
    }

    private Flux<List<Tick>> executeQueryUntilTopN(
            List<TickRequestContext> tickRequestContexts,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean pinStart,
            PriorityQueue<Tick> orderedTicks,
            int totalTicks) {

        List<Flux<Tick>> tickQueryFluxes = tickRequestContexts.stream()
                .map(tickRequestContext -> Flux.defer(() -> executeQuery(tickRequestContext, startTime, endTime, pinStart)))
                .collect(Collectors.toList());

        @SuppressWarnings("unchecked")
        Flux<Tick>[] tickQueryFluxArray = tickQueryFluxes.toArray(new Flux[0]);

        return Flux.defer(() -> Flux.mergeComparingDelayError(1, orderedTicks.comparator(), tickQueryFluxArray))
                .repeat(() -> {

                    if (orderedTicks.size() == totalTicks) {
                        return true;
                    }

                    for (TickRequestContext tickRequestContext : tickRequestContexts) {
                        if (tickRequestContext.getContinuationToken() != null) {
                            return false;
                        }
                    }

                    return true;
                }).flatMap(o -> {
                    if (o != null) {
                        synchronized (this) {
                            orderedTicks.offer(o);
                        }
                    }

                    return Mono.just(orderedTicks.stream().toList());
                });
    }

    private SqlQuerySpec getSqlQuerySpec(
            String tickIdentifier,
            String containerId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean pinStart) {

        LocalDate localDate = LocalDate.parse(containerId);

        String queryStartTime = !startTime.isBefore(localDate.atStartOfDay()) ? startTime.toString() : localDate.atStartOfDay().toString();
        String queryEndTime = !endTime.isAfter(localDate.atTime(23, 59, 59)) ? endTime.toString() : localDate.atTime(23, 59, 59).toString();

        List<SqlParameter> parameters = new ArrayList<>();

        parameters.add(new SqlParameter("@tickIdentifier", tickIdentifier));
        parameters.add(new SqlParameter("@startTime", queryStartTime));
        parameters.add(new SqlParameter("@endTime", queryEndTime));

        if (pinStart) {
            String query = "SELECT * FROM C WHERE C.pk = @tickIdentifier " +
                    "AND C.timestamp >= @startTime AND C.timestamp <= @endTime ORDER BY C.timestamp DESC";

            return new SqlQuerySpec(query, parameters);
        } else {
            String query = "SELECT * FROM C WHERE C.pk = @tickIdentifier " +
                    "AND C.timestamp >= @startTime AND C.timestamp <= @endTime ORDER BY C.timestamp ASC";

            return new SqlQuerySpec(query, parameters);
        }
    }

    private Flux<Tick> executeQuery(TickRequestContext tickRequestContext, LocalDateTime startTime, LocalDateTime endTime, boolean pinStart) {

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
                .byPage(continuationToken, 100))
                .flatMap(page -> {
                    tickRequestContext.setContinuationToken(page.getContinuationToken());
                    return Flux.fromIterable(page.getResults());
                });
    }
}
