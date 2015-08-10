package com.yazino.engagement.campaign.domain;

public enum MessageContentType {

    TITLE("title"),
    MESSAGE("message"),
    DESCRIPTION("description"),
    TYPE("type"),
    CAMPAIGN_RUN_ID("campaignrunid"),
    TIME_TO_LIVE_IN_SECS("timetoliveinsecs"),
    TRACKING("tracking");

    private final String key;

    private MessageContentType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
