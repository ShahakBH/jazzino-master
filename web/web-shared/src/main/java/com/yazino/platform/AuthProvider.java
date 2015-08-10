package com.yazino.platform;

public enum AuthProvider {
    FACEBOOK("FACEBOOK"),
    YAZINO("YAZINO"),
    TANGO("TANGO"),

    /**
     * Following values were used by RPX *
     */
    @Deprecated
    GOOGLE("Google"),
    @Deprecated
    TWITTER("Twitter"),
    @Deprecated
    YAHOO("Yahoo!");

    private final String providerName;

    private AuthProvider(String providerName) {
        this.providerName = providerName;
    }

    /**
     * Only required to keep backwards compatibility with old RPX providers.
     */
    @Deprecated
    public static AuthProvider parseProviderName(String providerName) {
        if (providerName == null) {
            throw new IllegalArgumentException("Invalid provider name: " + providerName);
        }
        for (AuthProvider authProvider : values()) {
            if (authProvider.providerName.toLowerCase().equals(providerName.toLowerCase())) {
                return authProvider;
            }
        }
        throw new IllegalArgumentException("Invalid provider name: " + providerName);
    }

    @Override
    public String toString() {
        return providerName;
    }
}
