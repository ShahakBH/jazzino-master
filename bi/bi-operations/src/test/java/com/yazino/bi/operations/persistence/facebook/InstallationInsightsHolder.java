package com.yazino.bi.operations.persistence.facebook;

import java.util.List;

import com.restfb.Facebook;

public class InstallationInsightsHolder {
    @Facebook
    private String id;

    @Facebook
    private List<InstallationInsights> values;

    public List<InstallationInsights> getValues() {
        return values;
    }

    public void setValues(final List<InstallationInsights> values) {
        this.values = values;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }
}
