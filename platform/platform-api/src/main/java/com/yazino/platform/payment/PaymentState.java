package com.yazino.platform.payment;

/**
 * An enum of valid payment states.
 * Nb. These are directly translated into DB values (by using name()) so
 * changes to these names may result in a DB error.
 */
public enum PaymentState {

    /**
     * The state of the payment cannot be determined.
     * This could be because the Dao failed.
     */
    Unknown,

    /**
     * The payment has been started.
     * No further attempts for this payment should be made.
     */
    Started,

    /**
     * The payment has finished.
     * No further attempts for this payment should be made.
     */
    Finished,

    /**
     * The payment has finished in a failed state.
     * No further attempts for this payment should be made.
     */
    FinishedFailed,

    /**
     * The payment has failed.
     * Retries for this payment are accepted.
     */
    Failed

}
