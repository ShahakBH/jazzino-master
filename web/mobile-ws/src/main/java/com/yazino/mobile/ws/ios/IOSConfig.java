package com.yazino.mobile.ws.ios;

import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

public class IOSConfig {
    private Map<String, String> minimumVersions = new HashMap<>();
    private Map<String, String> latestVersions = new HashMap<>();
    private Map<String, String> identifiers = new HashMap<>();

    public Map<String, String> getMinimumVersions() {
        return minimumVersions;
    }

    public void setMinimumVersions(final Map<String, String> minimumVersions) {
        notNull(minimumVersions);

        this.minimumVersions = minimumVersions;
    }

    public Map<String, String> getLatestVersions() {
        return latestVersions;
    }

    public void setLatestVersions(Map<String, String> latestVersions) {
        notNull(latestVersions);

        this.latestVersions = latestVersions;
    }

    public Map<String, String> getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(Map<String, String> identifiers) {
        notNull(identifiers);

        this.identifiers = identifiers;
    }
}
