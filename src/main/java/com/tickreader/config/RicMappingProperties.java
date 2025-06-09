package com.tickreader.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "ric")
public class RicMappingProperties {

    private Map<String, RicGroupMappingContext> mappings;

    public Map<String, RicGroupMappingContext> getMappings() {
        return this.mappings;
    }

    public void setMappings(Map<String, RicGroupMappingContext> mappings) {
        this.mappings = mappings;
    }

    public RicGroupMappingContext getMappedRicGroup(String ric) {
        return mappings.get(ric);
    }
}
