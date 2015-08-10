package com.yazino.web.payment.creditcard;

public enum PurchaseOutcome {

    /**
     * The request failed validation.
     */
    VALIDATION_ERROR,

    /**
     * The payment option was invalid.
     */
    INVALID_PAYMENT_OPTION,

    /**
     * The transaction was approved.
     */
    APPROVED,

    /**
     * The transaction was declined.
     */
    DECLINED,

    /**
     * The transaction was referred.
     */
    REFERRED,

    /**
     * The account details (e.g. card number) were invalid.
     */
    INVALID_ACCOUNT,

    /**
     * The expiry date is invalid.
     */
    INVALID_EXPIRY,

    /**
     * There are insufficient funds in the account to process the transaction.
     */
    INSUFFICIENT_FUNDS,

    /**
     * The transaction would exceed the card's limit.
     */
    EXCEEDS_TRANSACTION_LIMIT,

    /**
     * Address checking failed.
     */
    AVS_CHECK_FAILED,

    /**
     * CSV/CVV2 checking failed.
     */
    CSC_CHECK_FAILED,

    /**
     * The system fraud rules blocked the transaction.
     */
    RISK_FAILED,

    /**
     * A generic system failure occurred, e.g. unable to connect.
     */
    SYSTEM_FAILURE,

    /**
     * Card is reported lost or stolen.
     */
    LOST_OR_STOLEN_CARD,

    /**
     * The submitting player has been blocked due to the result of the purchase.
     */
    PLAYER_BLOCKED,

    /**
     * The transaction failed for an unknown reason.
     */
    UNKNOWN


}
