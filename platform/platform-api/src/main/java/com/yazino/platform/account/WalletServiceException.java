package com.yazino.platform.account;

import com.yazino.game.api.ApplicationException;
import com.yazino.game.api.ParameterisedMessage;

public class WalletServiceException extends ApplicationException {
    private static final long serialVersionUID = -2969572977408976228L;

    private final boolean unexpected;

    public WalletServiceException(final String message,
                                  final Object... args) {
        super(new ParameterisedMessage(message, args));

        this.unexpected = true;
    }

    public WalletServiceException(final ParameterisedMessage parameterisedMessage) {
        super(parameterisedMessage);

        this.unexpected = true;
    }

    public WalletServiceException(final ParameterisedMessage parameterisedMessage,
                                  final boolean unexpected) {
        super(parameterisedMessage);

        this.unexpected = unexpected;
    }

    public boolean isUnexpected() {
        return unexpected;
    }
}
