package com.yazino.web.payment.googlecheckout;

/**
 * Possible states of an android in app purchase.
 */
public enum OrderStatus {
    /**
     * error handling the purchase
     */
    ERROR,

    /**
     * Transaction is complete, chips have been credited
     */
    DELIVERED
}