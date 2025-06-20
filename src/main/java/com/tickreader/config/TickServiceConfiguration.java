package com.tickreader.config;

import com.tickreader.service.TicksService;
import com.tickreader.service.impl.FeedRangeBackupTicksServiceImpl;
import com.tickreader.service.impl.TicksServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class TickServiceConfiguration {

    @Bean
    @Primary
    @ConditionalOnProperty(name = "ticks.implementation", havingValue = "false", matchIfMissing = true)
    public TicksService ticksService(RicBasedCosmosClientFactory clientFactory, 
                                   CosmosDbAccountConfiguration cosmosDbAccountConfiguration) {
        return new FeedRangeBackupTicksServiceImpl(clientFactory, cosmosDbAccountConfiguration);
    }

    @Bean
    @ConditionalOnProperty(name = "ticks.implementation", havingValue = "true")
    public TicksService feedRangeBackupTicksService(RicBasedCosmosClientFactory clientFactory,
                                                         CosmosDbAccountConfiguration cosmosDbAccountConfiguration) {
        return new FeedRangeBackupTicksServiceImpl(clientFactory, cosmosDbAccountConfiguration);
    }
} 