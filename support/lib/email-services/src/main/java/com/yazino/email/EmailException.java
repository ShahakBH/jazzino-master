package com.yazino.email;

public class EmailException extends Exception {
    private static final long serialVersionUID = -5429136501357876154L;

    public EmailException(final String message) {
        super(message);
    }

    public EmailException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
