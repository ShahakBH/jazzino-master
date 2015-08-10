package com.yazino.bi.operations.campaigns.controller;

import com.yazino.engagement.ChannelType;
import com.yazino.engagement.campaign.domain.NotificationChannelConfigType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class Campaign {
    private boolean enabled;

    private CampaignScheduleWithName campaignScheduleWithName;
    private String sqlQuery;
    private Map<String, String> content;
    private List<ChannelType> channels;
    private boolean promo;
    private Map<NotificationChannelConfigType, String> channelConfig;
    private boolean delayNotifications;

    // For MVC binding
    public Campaign() {
        campaignScheduleWithName = new CampaignScheduleWithName();
    }

    public Campaign(final Long id,
                    final String name,
                    final DateTime nextRun,
                    final DateTime endTime,
                    final Long runHours,
                    final Long runMinutes,
                    final String sqlQuery,
                    final Map<String, String> content,
                    final List<ChannelType> channels,
                    final Boolean promo,
                    final Map<NotificationChannelConfigType, String> channelConfig,
                    final boolean delayNotifications) {

        this.campaignScheduleWithName = new CampaignScheduleWithName(id, name, nextRun, endTime, runHours, runMinutes);
        this.delayNotifications = delayNotifications;
        this.sqlQuery = sqlQuery;
        this.content = content;
        this.channels = channels;
        this.promo = promo;
        this.channelConfig = channelConfig;
        this.enabled = true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public Map<NotificationChannelConfigType, String> getChannelConfig() {
        return channelConfig;
    }

    public void setGameTypes(List<String> gameTypes) {
        channelConfig.put(NotificationChannelConfigType.GAME_TYPE_FILTER, StringUtils.join(gameTypes, ","));
    }

    public List<String> getGameTypes() {
        if (channelConfig == null) {
            return newArrayList();
        }
        final String gameTypeFilter = channelConfig.get(NotificationChannelConfigType.GAME_TYPE_FILTER);
        if (isBlank(gameTypeFilter)) {
            return newArrayList();
        }
        return newArrayList(gameTypeFilter.split(","));
    }

    public void setChannelConfig(final Map<NotificationChannelConfigType, String> channelConfig) {
        this.channelConfig = channelConfig;
    }

    public String getSqlQuery() {
        return sqlQuery;
    }

    public void setSqlQuery(final String sqlQuery) {
        this.sqlQuery = sqlQuery;
    }

    public Map<String, String> getContent() {
        return content;
    }

    public void setContent(final Map<String, String> content) {
        this.content = content;
    }

    public List<ChannelType> getChannels() {
        return channels;
    }

    public void setChannels(final List<ChannelType> channels) {
        this.channels = channels;
    }

    public boolean isPromo() {
        return promo;
    }

    public boolean getPromo() {
        return promo;
    }

    public void setPromo(boolean promo) {
        this.promo = promo;
    }

    public CampaignScheduleWithName getCampaignScheduleWithName() {
        return campaignScheduleWithName;
    }

    public String getName() {
        return getCampaignScheduleWithName().getName();
    }

    public Long getCampaignId() {
        return getCampaignScheduleWithName().getCampaignId();
    }

    public void setCampaignScheduleWithName(final CampaignScheduleWithName campaignScheduleWithName) {
        this.campaignScheduleWithName = campaignScheduleWithName;
    }

    public void setDelayNotifications(final boolean delayNotifications) {
        this.delayNotifications = delayNotifications;
    }

    public boolean isDelayNotifications() {
        return delayNotifications;
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
        Campaign rhs = (Campaign) obj;
        return new EqualsBuilder()
                .append(this.campaignScheduleWithName, rhs.campaignScheduleWithName)
                .append(this.sqlQuery, rhs.sqlQuery)
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
                .append(campaignScheduleWithName)
                .append(sqlQuery)
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
                .append("campaignScheduleWithName", campaignScheduleWithName)
                .append("sqlQuery", sqlQuery)
                .append("content", content)
                .append("channels", channels)
                .append("promo", promo)
                .append("channelConfig", channelConfig)
                .append("enabled", enabled)
                .append("delayNotifications", delayNotifications)
                .toString();
    }

    public Map<String, String> toStringMap() {
        Map<String, String> campaignMap = new LinkedHashMap<String, String>();
        campaignMap.put("Campaign Id", getValueAsStringWithNullCheck(campaignScheduleWithName.getCampaignId()));
        campaignMap.put("Name", getValueAsStringWithNullCheck(campaignScheduleWithName.getName()));
        campaignMap.put("Next Run", getValueAsStringWithNullCheck(campaignScheduleWithName.getNextRunTs()));
        campaignMap.put("Run Hours", getValueAsStringWithNullCheck(campaignScheduleWithName.getRunHours()));
        campaignMap.put("Run Minutes", getValueAsStringWithNullCheck(campaignScheduleWithName.getRunMinutes()));
        campaignMap.put("Expire Time", getValueAsStringWithNullCheck(campaignScheduleWithName.getEndTime()));
        campaignMap.put("SQL Query", getSqlQuery());
        campaignMap.put("Has Promotion", getValueAsStringWithNullCheck(isPromo()));
        campaignMap.put("Enabled", getValueAsStringWithNullCheck(isEnabled()));
        campaignMap.put("Delay Notification Sending", getValueAsStringWithNullCheck(delayNotifications));
        if (content != null) {
            for (String key : content.keySet()) {
                campaignMap.put(key, content.get(key));
            }
        }

        campaignMap.put("Notification Channels", getValueAsStringWithNullCheck(channels));

        return campaignMap;
    }

    private String getValueAsStringWithNullCheck(Object object) {
        if (object == null) {
            return "";
        } else {
            return object.toString();
        }
    }
}
