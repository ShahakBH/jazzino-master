package com.yazino.yaps;

/**
 * This class
 */
public class MessageTransformationException extends Exception {

    private static final long serialVersionUID = 8686815160208289441L;

    public MessageTransformationException(final String message) {
        super(message);
    }

    public MessageTransformationException(final Throwable cause) {
        super(cause);
    }
}
