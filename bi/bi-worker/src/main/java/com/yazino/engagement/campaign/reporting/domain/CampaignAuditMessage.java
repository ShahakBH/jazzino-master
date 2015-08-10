package com.yazino.engagement.campaign.reporting.domain;

import com.yazino.platform.messaging.Message;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class CampaignAuditMessage implements Message {
    private static final long serialVersionUID = -8503711375771447720L;


    private Long campaignRunId;

    private DateTime timestamp;

    @JsonCreator
    public CampaignAuditMessage(@JsonProperty("campaignRunId") final Long campaignRunId,
                                @JsonProperty("timestamp") final DateTime timestamp) {
        this.campaignRunId = campaignRunId;
        this.timestamp = timestamp;
    }

    public Long getCampaignRunId() {
        return campaignRunId;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public Object getMessageType() {
        return "campaignAuditMessage";
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("campaignRunId", campaignRunId)
                .append("timestamp", timestamp)
                .toString();
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
        CampaignAuditMessage rhs = (CampaignAuditMessage) obj;
        return new EqualsBuilder()
                .append(this.campaignRunId, rhs.campaignRunId)
                .append(this.timestamp, rhs.timestamp)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(campaignRunId)
                .append(timestamp)
                .toHashCode();
    }
}
