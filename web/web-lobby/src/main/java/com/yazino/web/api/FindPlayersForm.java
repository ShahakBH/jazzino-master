package com.yazino.web.api;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * A simple object to encapsulate the parameters used when searching for players.
 */
public class FindPlayersForm {

    private String provider;
    private String providerIds;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderIds() {
        return providerIds;
    }

    /**
     * A csv of Id's for that provider
     * @param providerIds the provider ids
     */
    public void setProviderIds(String providerIds) {
        this.providerIds = providerIds;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append("provider", provider).
                append("providerIds", providerIds).
                toString();
    }
}
