package com.yazino.game.api;

public class ApplicationException extends Exception {
    private static final long serialVersionUID = -2702593342688205494L;

    private final ParameterisedMessage parameterisedMessage;

    public ApplicationException(final ParameterisedMessage parameterisedMessage) {
        super(parameterisedMessage.toString());
        this.parameterisedMessage = parameterisedMessage;
    }

    public ParameterisedMessage getParameterisedMessage() {
        return parameterisedMessage;
    }
}
