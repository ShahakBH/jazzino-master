package com.yazino.game.api;

public class GameException extends ApplicationException {
    private static final long serialVersionUID = -9191976452603198915L;
    private final boolean throwForEvents;

    public GameException(final String message,
                         final Object... args) {
        this(new ParameterisedMessage(message, args));
    }

    public GameException(final ParameterisedMessage msg) {
        this(msg, false);
    }

    public GameException(final ParameterisedMessage parameterisedMessage,
                         final boolean throwForEvents) {
        super(parameterisedMessage);
        this.throwForEvents = throwForEvents;
    }

    public GameException(final String message,
                         final boolean throwForEvents,
                         final Object... args) {
        this(new ParameterisedMessage(message, args), throwForEvents);
    }

    public boolean isThrowForEvents() {
        return throwForEvents;
    }
}
