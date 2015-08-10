package com.yazino.yaps;

/**
 * An checked exception thrown by the {@link PushMessageBinaryBuilder} when attempting to build with an invalid field.
 */
public class InvalidFieldException extends Exception {

    private static final long serialVersionUID = 5048652827842011136L;

    InvalidFieldException(final Throwable cause) {
        super(cause);
    }

    InvalidFieldException(final String message) {
        super(message);
    }

    InvalidFieldException(final int expected,
                          final int actual) {
        super(String.format("Expected length was %d, but was %d", expected, actual));
    }
}
