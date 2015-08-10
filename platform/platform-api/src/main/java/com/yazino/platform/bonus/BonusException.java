package com.yazino.platform.bonus;

public class BonusException extends Exception {
    public BonusException(final String message) {
        super(message);
    }

    public BonusException(final String message, final Exception cause) {
        super(message, cause);
    }
}
