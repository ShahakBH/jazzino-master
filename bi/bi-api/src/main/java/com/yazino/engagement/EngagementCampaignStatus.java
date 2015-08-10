package com.yazino.engagement;

/**
 * status from AppRequests Extracted from IOS AppRequests
 */

public enum EngagementCampaignStatus {

    CREATED(0), PROCESSING(1), SENT(2), EXPIRED(3), EXPIRING(4);

    private int value;

    private EngagementCampaignStatus(final int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public static EngagementCampaignStatus ofStatusCode(final int value) {
        if (value == 0) {
            return CREATED;
        }
        if (value == 1) {
            return PROCESSING;
        }
        if (value == 2) {
            return SENT;
        }
        if (value == 3) {
            return EXPIRED;
        }
        if (value == 4) {
            return EXPIRING;
        }
        throw new IllegalArgumentException("Invalid value '" + value + "' passed to ofStatusCode()");
    }
}

