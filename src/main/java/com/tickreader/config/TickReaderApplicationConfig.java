package com.tickreader.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({CosmosDbAccountConfiguration.class})
public class TickReaderApplicationConfig {
}
