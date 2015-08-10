package com.yazino.engagement.campaign.reporting.domain;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CampaignRunAuditMessage extends CampaignAuditMessage {
    private static final long serialVersionUID = -8503711375771447720L;

    private Long campaignId;
    private Long promoId;
    private String name;
    private Integer size;
    private String status;
    private String message;

    @JsonCreator
    public CampaignRunAuditMessage(@JsonProperty("campaignId") final Long campaignId,
                                   @JsonProperty("campaignRunId") final Long campaignRunId,
                                   @JsonProperty("name") final String name,
                                   @JsonProperty("size") final Integer size,
                                   @JsonProperty("timestamp") final DateTime timestamp,
                                   @JsonProperty("promoId") final Long promoId,
                                   @JsonProperty("status") final String status,
                                   @JsonProperty("message") final String message) {

        super(campaignRunId, timestamp);
        this.campaignId = campaignId;
        this.name = name;
        this.size = size;
        this.promoId = promoId;
        this.status = status;
        this.message = message;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public String getName() {
        return name;
    }

    public Integer getSize() {
        return size;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Long getPromoId() {
        return promoId;
    }

    @Override
    public Object getMessageType() {
        return CampaignAuditMessageType.CAMPAIGN_RUN_MESSAGE.toString();
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
        CampaignRunAuditMessage rhs = (CampaignRunAuditMessage) obj;
        return new EqualsBuilder()
                .appendSuper(super.equals(obj))
                .append(this.campaignId, rhs.campaignId)
                .append(this.name, rhs.name)
                .append(this.size, rhs.size)
                .append(this.status, rhs.status)
                .append(this.message, rhs.message)
                .append(this.promoId, rhs.promoId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(campaignId)
                .append(name)
                .append(size)
                .append(status)
                .append(message)
                .append(promoId)
                .toHashCode();
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .appendSuper(super.toString())
                .append("campaignId", campaignId)
                .append("name", name)
                .append("size", size)
                .append("status", status)
                .append("message", message)
                .append("promoId", promoId)
                .toString();
    }
}
