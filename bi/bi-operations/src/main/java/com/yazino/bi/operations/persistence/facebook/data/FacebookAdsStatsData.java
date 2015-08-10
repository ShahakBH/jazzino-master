package com.yazino.bi.operations.persistence.facebook.data;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.restfb.Facebook;

/**
 * Individual campaign statistics
 */
public class FacebookAdsStatsData {
    @Facebook("adgroup_id")
    private String id;

    @Facebook
    private Long impressions;

    @Facebook
    private Long clicks;

    @Facebook
    private Long spent;

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("id", id).append("impressions", impressions)
                .append("clicks", clicks).append("spent", spent).toString();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof FacebookAdsStatsData)) {
            return false;
        }
        final FacebookAdsStatsData castOther = (FacebookAdsStatsData) other;
        return new EqualsBuilder().append(id, castOther.id).append(impressions, castOther.impressions)
                .append(clicks, castOther.clicks).append(spent, castOther.spent).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(id).append(impressions).append(clicks).append(spent).toHashCode();
    }

    public Long getClicks() {
        return clicks;
    }

    public void setClicks(final Long clicks) {
        this.clicks = clicks;
    }

    public Long getSpent() {
        return spent;
    }

    public void setSpent(final Long spent) {
        this.spent = spent;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public Long getImpressions() {
        return impressions;
    }

    public void setImpressions(final Long impressions) {
        this.impressions = impressions;
    }
}
