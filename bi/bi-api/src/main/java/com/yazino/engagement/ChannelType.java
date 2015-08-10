package com.yazino.engagement;

/**
 * enum for Engagement Campaign Requests
 */
public enum ChannelType {
    FACEBOOK_APP_TO_USER_REQUEST("Facebook App To User Request", true),
    FACEBOOK_APP_TO_USER_NOTIFICATION("Facebook App To User Notification", false),
    IOS("IOS Push Notification", false),
    GOOGLE_CLOUD_MESSAGING_FOR_ANDROID("Android Push Notification", false),
    AMAZON_DEVICE_MESSAGING("Amazon Device Messaging", false),
    EMAIL("Email", false);

    private final String description;
    private final boolean canDeleteRequests;

    ChannelType(final String description, final boolean canDeleteRequests) {
        this.description = description;
        this.canDeleteRequests = canDeleteRequests;
    }

    public String getDescription() {
        return description;
    }

    public boolean canDeleteRequests() {
        return canDeleteRequests;
    }
}
