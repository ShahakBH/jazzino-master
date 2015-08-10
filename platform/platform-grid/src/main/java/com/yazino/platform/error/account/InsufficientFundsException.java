package com.yazino.platform.error.account;

import com.yazino.game.api.ParameterisedMessage;

/**
 * Thrown when an account operation cannot be completed due to insufficient funds.
 */
public class InsufficientFundsException extends Exception {
    private static final long serialVersionUID = -3701646961259832933L;

    private final ParameterisedMessage parameterisedMessage;

    public InsufficientFundsException(final ParameterisedMessage parameterisedMessage) {
        super(parameterisedMessage.toString());

        this.parameterisedMessage = parameterisedMessage;
    }

    public ParameterisedMessage getParameterisedMessage() {
        return parameterisedMessage;
    }
}
