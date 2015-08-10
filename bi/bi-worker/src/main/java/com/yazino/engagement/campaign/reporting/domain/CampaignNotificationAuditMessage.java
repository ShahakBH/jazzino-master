package com.yazino.engagement.campaign.reporting.domain;

import com.yazino.engagement.ChannelType;
import com.yazino.platform.messaging.Message;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

import java.math.BigDecimal;

import static com.yazino.engagement.campaign.reporting.domain.CampaignAuditMessageType.CAMPAIGN_NOTIFICATION;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CampaignNotificationAuditMessage implements Message {

    private static final long serialVersionUID = 953111219377168811L;

    private final long campaignRunId;
    private final BigDecimal playerId;
    private final ChannelType channel;
    private final String gameType;
    private final NotificationAuditType notificationAuditType;
    private final DateTime timeStamp;

    //TODO modify this to use builder pattern
    @JsonCreator
    public CampaignNotificationAuditMessage(@JsonProperty("campaignRunId") final long campaignRunId,
                                            @JsonProperty("playerId") BigDecimal playerId,
                                            @JsonProperty("channel") ChannelType channel,
                                            @JsonProperty("gameType") String gameType,
                                            @JsonProperty("notificationAuditType") NotificationAuditType notificationAuditType,
                                            @JsonProperty("timeStamp") DateTime timeStamp) {
        this.campaignRunId = campaignRunId;
        this.playerId = playerId;
        this.channel = channel;
        this.gameType = gameType;
        this.notificationAuditType = notificationAuditType;
        this.timeStamp = timeStamp;
    }

    public long getCampaignRunId() {
        return campaignRunId;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public ChannelType getChannel() {
        return channel;
    }

    public String getGameType() {
        return gameType;
    }

    public NotificationAuditType getNotificationAuditType() {
        return notificationAuditType;
    }

    public DateTime getTimeStamp() {
        return timeStamp;
    }

    @Override
    public int getVersion() {
        return Message.VERSION;
    }

    @Override
    public Object getMessageType() {
        return CAMPAIGN_NOTIFICATION.toString();
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
        CampaignNotificationAuditMessage rhs = (CampaignNotificationAuditMessage) obj;
        return new EqualsBuilder()
                .append(this.campaignRunId, rhs.campaignRunId)
                .append(this.channel, rhs.channel)
                .append(this.gameType, rhs.gameType)
                .append(this.notificationAuditType, rhs.notificationAuditType)
                .append(this.timeStamp, rhs.timeStamp)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(campaignRunId)
                .append(BigDecimals.strip(playerId))
                .append(channel)
                .append(gameType)
                .append(notificationAuditType)
                .append(timeStamp)
                .toHashCode();
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("serialVersionUID", serialVersionUID)
                .append("campaignRunId", campaignRunId)
                .append("playerId", playerId)
                .append("channel", channel)
                .append("gameType", gameType)
                .append("notificationAuditType", notificationAuditType)
                .append("timeStamp", timeStamp)
                .toString();
    }
}
