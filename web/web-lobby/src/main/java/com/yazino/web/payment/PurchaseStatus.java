package com.yazino.web.payment;

public enum PurchaseStatus {
    /**
     * promotion has either expired, or is unknown
     */
    STALE_PROMOTION,

    /**
     * Purchase request was successfully created
     */
    CREATED,

    /**
     * Failed - see errorMessage for details
     */
    FAILED,

    SUCCESS, CANCELLED,
}
