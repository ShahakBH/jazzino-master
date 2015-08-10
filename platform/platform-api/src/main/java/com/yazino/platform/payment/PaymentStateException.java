package com.yazino.platform.payment;

import static org.apache.commons.lang3.Validate.notNull;

public class PaymentStateException extends Exception {
    private static final long serialVersionUID = 8441839486512101374L;

    private final PaymentState state;

    public PaymentStateException(final PaymentState state) {
        notNull(state);
        this.state = state;
    }

    public PaymentState getState() {
        return state;
    }
}
