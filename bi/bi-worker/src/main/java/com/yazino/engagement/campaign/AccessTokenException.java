package com.yazino.engagement.campaign;

/**
 * An exception that is thrown if when we fail to fetch an access token
 */
public class AccessTokenException extends Exception {

    public AccessTokenException(final String message) {
        super(message);
    }

    public AccessTokenException(final String message, final Throwable throwable) {
        super(message, throwable);
    }
}
