package com.yazino.engagement;

import com.yazino.platform.messaging.Message;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PushNotificationMessage implements Message {

    private static final long serialVersionUID = -5996375201028618397L;
    private final PlayerTarget playerTarget;
    private final Map<String, String> content;
    private final ChannelType channel;
    private Long campaignRunId;

    @JsonCreator
    public PushNotificationMessage(@JsonProperty("playerTarget") PlayerTarget playerTarget,
                                   @JsonProperty("content") Map<String, String> content,
                                   @JsonProperty("channel") ChannelType channel,
                                   @JsonProperty("campaignRunId") final Long campaignRunId) {
        this.playerTarget = playerTarget;
        this.content = content;
        this.channel = channel;
        this.campaignRunId = campaignRunId;
    }

    public Long getCampaignRunId() {
        return campaignRunId;
    }

    public PlayerTarget getPlayerTarget() {
        return playerTarget;
    }

    public Map<String, String> getContent() {
        return content;
    }

    public ChannelType getChannel() {
        return channel;
    }

    @Override
    public int getVersion() {
        return Message.VERSION;
    }

    @Override
    public Object getMessageType() {
        return channel.toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(this.playerTarget)
                .append(this.content)
                .append(this.campaignRunId)
                .append(this.channel).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PushNotificationMessage other = (PushNotificationMessage) obj;
        return new EqualsBuilder()
                .append(this.playerTarget, other.playerTarget)
                .append(this.content, other.content)
                .append(this.campaignRunId, other.campaignRunId)
                .append(this.channel, other.channel).isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("playerTarget", playerTarget)
                .append("content", content)
                .append("channel", channel)
                .append("campaignRunId", campaignRunId)
                .toString();
    }
}
