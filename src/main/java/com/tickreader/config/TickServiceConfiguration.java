package com.tickreader.config;

import com.tickreader.service.BackupTicksService;
import com.tickreader.service.TicksService;
import com.tickreader.service.impl.BackupTicksServiceImpl;
import com.tickreader.service.impl.TicksServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class TickServiceConfiguration {

    @Bean
    @Primary
    @ConditionalOnProperty(name = "tick.service.implementation", havingValue = "reactor", matchIfMissing = true)
    public TicksService ticksService(RicBasedCosmosClientFactory clientFactory, 
                                   CosmosDbAccountConfiguration cosmosDbAccountConfiguration) {
        return new TicksServiceImpl(clientFactory, cosmosDbAccountConfiguration);
    }

    @Bean
    @ConditionalOnProperty(name = "tick.service.implementation", havingValue = "backup")
    public BackupTicksService backupTicksService(RicBasedCosmosClientFactory clientFactory, 
                                               CosmosDbAccountConfiguration cosmosDbAccountConfiguration) {
        return new BackupTicksServiceImpl(clientFactory, cosmosDbAccountConfiguration);
    }
} 