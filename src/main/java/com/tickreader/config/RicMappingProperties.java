package com.tickreader.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "ric.mapping")
public class RicMappingProperties {

    private Map<String, String> mappings;

    public Map<String, String> getMappings() {
        return this.mappings;
    }

    public void setMappings(Map<String, String> mappings) {
        this.mappings = mappings;
    }

    public String getMappedRicGroup(String ric) {
        return mappings.getOrDefault(ric, ric);
    }
}
