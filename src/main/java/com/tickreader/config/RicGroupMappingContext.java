package com.tickreader.config;

public class RicGroupMappingContext {

    private final String ricName;
    private final String ricGroupId;

    public RicGroupMappingContext(String ricName, String ricGroupId) {
        this.ricName = ricName;
        this.ricGroupId = ricGroupId;
    }

    public String getRicName() {
        return ricName;
    }

    public String getRicGroupId() {
        return ricGroupId;
    }
}
