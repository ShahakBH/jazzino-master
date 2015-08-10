package com.yazino.bi.operations.engagement;

import com.yazino.engagement.ChannelType;
import com.yazino.engagement.EngagementCampaignStatus;
import org.joda.time.DateTime;

/**
 * Builder class for FacebookAppRequests
 */

public class EngagementCampaignBuilder {

    private Integer id;
    private String title;
    private String description;
    private String message;
    private String trackingReference;
    private DateTime created;
    private DateTime sent;
    private ChannelType channelType;
    private EngagementCampaignStatus status;
    private int targetCount;
    private DateTime scheduled;
    private DateTime expires;


    public EngagementCampaignBuilder() {
    }

    public EngagementCampaignBuilder(final EngagementCampaign engagementCampaign) {
        this.id = engagementCampaign.getId();
        this.title = engagementCampaign.getTitle();
        this.description = engagementCampaign.getDescription();
        this.message = engagementCampaign.getMessage();
        this.trackingReference = engagementCampaign.getTrackingReference();
        this.created = engagementCampaign.getCreated();
        this.sent = engagementCampaign.getSent();
        this.channelType = engagementCampaign.getChannelType();
        this.status = engagementCampaign.getStatus();
        this.targetCount = engagementCampaign.getTargetCount();
        this.scheduled = engagementCampaign.getScheduled();
        this.expires = engagementCampaign.getExpires();
    }

    public EngagementCampaign build() {
        final EngagementCampaign engagementCampaign = new EngagementCampaign();
        engagementCampaign.setId(id);
        engagementCampaign.setChannelType(channelType);
        engagementCampaign.setTitle(title);
        engagementCampaign.setDescription(description);
        engagementCampaign.setMessage(message);
        engagementCampaign.setTrackingReference(trackingReference);
        engagementCampaign.setCreated(created);
        engagementCampaign.setSent(sent);
        engagementCampaign.setStatus(status);
        engagementCampaign.setTargetCount(targetCount);
        engagementCampaign.setScheduled(scheduled);
        engagementCampaign.setExpires(expires);
        return engagementCampaign;
    }

    public EngagementCampaignBuilder withId(final Integer value) {
        this.id = value;
        return this;
    }

    public EngagementCampaignBuilder withTitle(final String value) {
        this.title = value;
        return this;
    }

    public EngagementCampaignBuilder withDescription(final String value) {
        this.description = value;
        return this;
    }

    public EngagementCampaignBuilder withMessage(final String value) {
        this.message = value;
        return this;
    }

    public EngagementCampaignBuilder withTrackingReference(final String value) {
        this.trackingReference = value;
        return this;
    }

    public EngagementCampaignBuilder withCreateDate(final DateTime value) {
        this.created = value;
        return this;
    }

    public EngagementCampaignBuilder withSentDate(final DateTime value) {
        this.sent = value;
        return this;
    }

    public EngagementCampaignBuilder withChannelType(final ChannelType value) {
        this.channelType = value;
        return this;
    }

    public EngagementCampaignBuilder withStatus(final EngagementCampaignStatus value) {
        this.status = value;
        return this;
    }

    public EngagementCampaignBuilder withTargetCount(final int value) {
        this.targetCount = value;
        return this;
    }

    public EngagementCampaignBuilder withScheduled(final DateTime value) {
        this.scheduled = value;
        return this;
    }

    public EngagementCampaignBuilder withExpires(final DateTime value) {
        this.expires = value;
        return this;
    }
}
