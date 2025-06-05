package com.tickreader.service.impl;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.tickreader.config.RicBasedCosmosClientFactory;
import com.tickreader.config.RicCosmosProperties;
import com.tickreader.config.RicMappingProperties;
import com.tickreader.entity.Tick;
import com.tickreader.service.TicksService;
import com.tickreader.service.utils.TickServiceUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TicksServiceImpl implements TicksService {

    private final RicBasedCosmosClientFactory clientFactory;
    private final RicMappingProperties ricMappingProperties;
    private static final String QUERY_TEMPLATE
            = "SELECT * FROM C where C.pk IN (%s) AND C.timestamp >= '%s' AND C.timestamp <= '%s'";

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

        return Flux.fromIterable(tickRequestContexts)
                .flatMap(tickRequestContext -> {
                    String query = String.format(QUERY_TEMPLATE, tickRequestContext.getTickIdentifier(),
                            startTime.toString(), endTime.toString());
                    return tickRequestContext.getAsyncContainer().queryItems(query, Tick.class);
                })
                .take(totalTicks)
                .collectList()
                .block();
    }
}
