package com.yazino.bi.operations.engagement;

import com.yazino.engagement.ChannelType;
import com.yazino.engagement.EngagementCampaignStatus;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.joda.time.DateTime;

import java.io.Serializable;

public class EngagementCampaign implements Serializable {
    private static final long serialVersionUID = -962713174041167610L;

    private Integer id;
    private ChannelType channelType;
    private String title;
    private String description;
    private String message;
    private DateTime created = new DateTime();
    private DateTime sent;
    private Integer targetCount = 0;
    private EngagementCampaignStatus status = EngagementCampaignStatus.CREATED;
    private String trackingReference;
    private DateTime scheduled;
    private DateTime expires;

    public EngagementCampaign() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public ChannelType getChannelType() {
        return channelType;
    }

    public void setChannelType(final ChannelType channelType) {
        this.channelType = channelType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public DateTime getCreated() {
        return created;
    }

    public void setCreated(final DateTime created) {
        this.created = created;
    }

    public DateTime getSent() {
        return sent;
    }

    public void setSent(final DateTime sent) {
        this.sent = sent;
    }

    public Integer getTargetCount() {
        return targetCount;
    }

    public void setTargetCount(final Integer targetCount) {
        this.targetCount = targetCount;
    }

    public EngagementCampaignStatus getStatus() {
        return status;
    }

    public void setStatus(final EngagementCampaignStatus status) {
        this.status = status;
    }

    public String getTrackingReference() {
        return trackingReference;
    }

    public void setTrackingReference(final String trackingReference) {
        this.trackingReference = trackingReference;
    }

    public DateTime getScheduled() {
        return scheduled;
    }

    public void setScheduled(final DateTime scheduled) {
        this.scheduled = scheduled;
    }

    public DateTime getExpires() {
        return expires;
    }

    public void setExpires(final DateTime expires) {
        this.expires = expires;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("id", id)
                .append("channelType", channelType)
                .append("title", title)
                .append("desc", description)
                .append("msg", message)
                .append("track", trackingReference)
                .append("created", created)
                .append("sent", sent)
                .append("targets", targetCount)
                .append("status", status)
                .append("scheduled", scheduled)
                .append("expires", expires)
                .toString();
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
        final EngagementCampaign rhs = (EngagementCampaign) obj;
        return new EqualsBuilder().append(id, rhs.id)
                                  .append(channelType, rhs.channelType)
                                  .append(title, rhs.title)
                                  .append(description, rhs.description)
                                  .append(message, rhs.message)
                                  .append(trackingReference, rhs.trackingReference)
                                  .append(created, rhs.created)
                                  .append(sent, rhs.sent)
                                  .append(targetCount, rhs.targetCount)
                                  .append(status, rhs.status)
                                  .append(scheduled, rhs.scheduled)
                                  .append(expires, rhs.expires)
                                  .isEquals();

    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(id)
                .append(channelType)
                .append(title)
                .append(description)
                .append(message)
                .append(trackingReference)
                .append(created)
                .append(sent)
                .append(targetCount)
                .append(status)
                .append(scheduled)
                .append(expires)
                .toHashCode();
    }
}

