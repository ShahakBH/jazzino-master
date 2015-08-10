package com.yazino.engagement;

import com.yazino.platform.messaging.Message;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import static org.apache.commons.lang3.Validate.notNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CampaignDeliverMessage implements Message {

    private static final long serialVersionUID = 1755981796182147814L;

    @JsonProperty
    private Long from;
    @JsonProperty
    private Long to;

    @JsonProperty("id")
    private Long campaignRunId;

    @JsonProperty("channel")
    private ChannelType channel;

    protected CampaignDeliverMessage() {
    }

    public CampaignDeliverMessage(final Long campaignRunId, final ChannelType channel) {
        notNull(campaignRunId, "campaignRunId may not be null");
        notNull(channel, "channel may not be null");
        this.campaignRunId = campaignRunId;
        this.channel = channel;
        this.from = null;
        this.to = null;

    }

    public CampaignDeliverMessage(final Long campaignRunId, final ChannelType channel, Long from, Long to) {
        notNull(campaignRunId, "campaignRunId may not be null");
        notNull(channel, "channel may not be null");
        this.campaignRunId = campaignRunId;
        this.channel = channel;
        this.from = from;
        this.to = to;
    }

    public Long getTo() {
        return to;
    }

    public Long getFrom() {
        return from;
    }

    public Long getCampaignRunId() {
        return campaignRunId;
    }

    public ChannelType getChannel() {
        return channel;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final CampaignDeliverMessage rhs = (CampaignDeliverMessage) obj;
        return new EqualsBuilder()
                .append(campaignRunId, rhs.campaignRunId)
                .append(channel, rhs.channel)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(campaignRunId)
                .append(channel)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

    @Override
    public int getVersion() {
        return Message.VERSION;
    }

    @Override
    public Object getMessageType() {
        return "CampaignDeliverMessage";
    }

}
