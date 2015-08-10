package com.yazino.web.domain.facebook;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class LegacyEngagementData {
    public static final String TRACKING_KEY = "tracking";

    @JsonProperty(value = "Engagement")
    private Map<String, String> engagement;

    public LegacyEngagementData() {
    }

    public LegacyEngagementData(final Map<String, String> engagement) {
        this.engagement = engagement;
    }

    @JsonIgnore
    public Map<String, String> getEngagement() {
        return engagement;
    }

    public void setEngagement(final Map<String, String> engagement) {
        this.engagement = engagement;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final LegacyEngagementData rhs = (LegacyEngagementData) obj;
        return new EqualsBuilder()
                .append(engagement, rhs.engagement)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(engagement)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(null)
                .append(engagement)
                .toString();
    }
}
