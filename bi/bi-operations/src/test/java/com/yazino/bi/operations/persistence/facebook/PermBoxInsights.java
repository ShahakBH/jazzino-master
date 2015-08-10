package com.yazino.bi.operations.persistence.facebook;

import java.util.List;

import com.restfb.Facebook;

public class PermBoxInsights {
    @Facebook("value")
    private PermBoxInsightsStats stats = new PermBoxInsightsStats();

    @Facebook("value")
    private List<String> emptyStats;

    public List<String> getEmptyStats() {
        return emptyStats;
    }

    public void setEmptyStats(final List<String> emptyStats) {
        this.emptyStats = emptyStats;
    }

    @Facebook("end_time")
    private String endTime;

    public PermBoxInsightsStats getStats() {
        return stats;
    }

    public void setStats(final PermBoxInsightsStats stats) {
        this.stats = stats;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(final String endTime) {
        this.endTime = endTime;
    }
}
