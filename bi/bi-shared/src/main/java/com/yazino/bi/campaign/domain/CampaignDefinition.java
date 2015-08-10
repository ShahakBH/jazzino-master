package com.yazino.bi.campaign.domain;

import com.yazino.engagement.ChannelType;
import com.yazino.engagement.campaign.domain.NotificationChannelConfigType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;
import java.util.Map;

public class CampaignDefinition {

    private final Long id;
    private final String name;
    private final String segmentSelectionQuery;
    private final Map<String, String> content;
    private final List<ChannelType> channels;
    private final Boolean promo;
    private final Map<NotificationChannelConfigType, String> channelConfig;
    private final boolean enabled;
    private final boolean delayNotifications;

    public CampaignDefinition(final Long id,
                              final String name,
                              final String segmentSelectionQuery,
                              final Map<String, String> content,
                              final List<ChannelType> channels,
                              final Boolean promo,
                              final Map<NotificationChannelConfigType, String> channelConfig,
                              final boolean enabled,
                              final boolean delayNotifications) {
        this.id = id;
        this.name = name;
        this.segmentSelectionQuery = segmentSelectionQuery;
        this.content = content;
        this.channels = channels;
        this.promo = promo;
        this.channelConfig = channelConfig;
        this.enabled = enabled;
        this.delayNotifications = delayNotifications;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<ChannelType> getChannels() {
        return channels;
    }

    public String getSegmentSelectionQuery() {
        return segmentSelectionQuery;
    }

    public Map<String, String> getContent() {
        return content;
    }

    public Map<NotificationChannelConfigType, String> getChannelConfig() {
        return channelConfig;
    }

    public Boolean hasPromo() {
        return promo;
    }

    public boolean delayNotifications() {
        return delayNotifications;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        CampaignDefinition rhs = (CampaignDefinition) obj;
        return new EqualsBuilder()
                .append(this.id, rhs.id)
                .append(this.name, rhs.name)
                .append(this.segmentSelectionQuery, rhs.segmentSelectionQuery)
                .append(this.content, rhs.content)
                .append(this.channels, rhs.channels)
                .append(this.promo, rhs.promo)
                .append(this.channelConfig, rhs.channelConfig)
                .append(this.enabled, rhs.enabled)
                .append(this.delayNotifications, rhs.delayNotifications)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(id)
                .append(name)
                .append(segmentSelectionQuery)
                .append(content)
                .append(channels)
                .append(promo)
                .append(channelConfig)
                .append(enabled)
                .append(delayNotifications)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("name", name)
                .append("segmentSelectionQuery", segmentSelectionQuery)
                .append("content", content)
                .append("channels", channels)
                .append("promo", promo)
                .append("channelConfig", channelConfig)
                .append("enabled", enabled)
                .append("delayNotifications", delayNotifications)
                .toString();
    }
}
