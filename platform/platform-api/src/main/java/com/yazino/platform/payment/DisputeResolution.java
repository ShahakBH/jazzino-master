package com.yazino.platform.payment;

public enum DisputeResolution {
    REFUNDED_PLAYER_ERROR,
    REFUNDED_FRAUD,
    REFUNDED_OTHER,
    CHIPS_CREDITED,
    REFUSED,
    REFUSED_BANNED;

    public static DisputeResolution valueOfOrNull(final String name) {
        for (DisputeResolution disputeResolution : values()) {
            if (disputeResolution.name().equals(name)) {
                return disputeResolution;
            }
        }
        return null;
    }
}
