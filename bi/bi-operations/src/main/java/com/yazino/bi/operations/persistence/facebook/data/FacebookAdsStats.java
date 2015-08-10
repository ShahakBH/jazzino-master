package com.yazino.bi.operations.persistence.facebook.data;

import java.util.Map;

import com.restfb.Facebook;

/**
 * Ad statistics holder for Facebook
 */
public class FacebookAdsStats {
    @Facebook("time_start")
    private Long timeStart;

    @Facebook("time_stop")
    private Long timeStop;

    @Facebook
    private Map<String, FacebookAdsStatsData> stats;

    public Long getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(final Long timeStart) {
        this.timeStart = timeStart;
    }

    public Long getTimeStop() {
        return timeStop;
    }

    public void setTimeStop(final Long timeStop) {
        this.timeStop = timeStop;
    }

    public Map<String, FacebookAdsStatsData> getStats() {
        return stats;
    }

    public void setStats(final Map<String, FacebookAdsStatsData> stats) {
        this.stats = stats;
    }
}
