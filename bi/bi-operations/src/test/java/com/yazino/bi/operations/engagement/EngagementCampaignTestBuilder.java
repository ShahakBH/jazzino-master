package com.yazino.bi.operations.engagement;


import com.yazino.engagement.ChannelType;
import org.joda.time.DateTime;

public class EngagementCampaignTestBuilder extends EngagementCampaignBuilder {

    public static final Integer id = 123;
    public static final String title = "Title of my Life";
    public static final String description = "descriptions are over rated";
    public static final String message = "this is a message";
    public static final String trackingReference = "trackity track track";
    public static final DateTime create = new DateTime();
    public static final DateTime sent = new DateTime();
    public static final ChannelType applicationType = ChannelType.FACEBOOK_APP_TO_USER_REQUEST;

    public EngagementCampaignTestBuilder() {
        withId(id);
        withTitle(title);
        withDescription(description);
        withMessage(message);
        withTrackingReference(trackingReference);
        withCreateDate(create);
        withSentDate(sent);
        withChannelType(applicationType);
    }
}
