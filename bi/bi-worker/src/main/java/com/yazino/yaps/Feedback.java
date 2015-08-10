package com.yazino.yaps;

import java.util.Date;

/**
 * The feedback we get from Apple.
 */
public class Feedback {
    private final Date removalDate;
    private final String deviceToken;

    public Feedback(final String deviceToken,
                    final Date removalDate) {
        this.removalDate = removalDate;
        this.deviceToken = deviceToken;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public Date getRemovalDate() {
        return removalDate;
    }

}
