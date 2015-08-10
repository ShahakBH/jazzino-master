package com.yazino.game.api;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;

public final class ParameterisedMessage implements Serializable {
    private static final long serialVersionUID = -854846328664494497L;

    private final String message;

    private final Object[] parameters;

    private static final Object[] EMPTY = new Object[0];

    public ParameterisedMessage(final String message) {
        this.message = message;
        parameters = EMPTY;
    }

    public ParameterisedMessage(final String message,
                                final Object... parameters) {
        this.message = message;
        this.parameters = parameters;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final ParameterisedMessage rhs = (ParameterisedMessage) obj;
        return new EqualsBuilder()
                .append(message, rhs.message)
                .append(parameters, rhs.parameters)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(message)
                .append(parameters)
                .toHashCode();
    }

    public String getMessage() {
        return message;
    }

    public Object[] getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return String.format(message, parameters);
    }
}
