package com.yazino.engagement.campaign.domain;

import com.yazino.platform.messaging.Message;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

import static org.apache.commons.lang3.Validate.notNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CampaignRunMessage implements Message {
    private static final long serialVersionUID = -6706113770520822459L;

    @JsonProperty("id")
    private Long campaignId;
    @JsonProperty("rt")
    private Date reportTime;

    public CampaignRunMessage() {
    }

    public CampaignRunMessage(Long campaignId, final Date reportTime) {
        notNull(campaignId, "campaignId should not be null");
        notNull(reportTime, "reportTime should not be null");

        this.campaignId = campaignId;
        this.reportTime = reportTime;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public Date getReportTime() {
        return reportTime;
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
        final CampaignRunMessage rhs = (CampaignRunMessage) obj;
        return new EqualsBuilder()
                .append(campaignId, rhs.campaignId)
                .append(reportTime, rhs.reportTime)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(campaignId)
                .append(reportTime)
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
        return "CampaignRunMessage";
    }
}
