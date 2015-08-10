package com.yazino.bi.operations.persistence.facebook;

import java.util.List;

import com.restfb.Facebook;

public class PermBoxInsightsHolder {
    @Facebook
    private String id;

    @Facebook
    private List<PermBoxInsights> values;

    public List<PermBoxInsights> getValues() {
        return values;
    }

    public void setValues(final List<PermBoxInsights> values) {
        this.values = values;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }
}
