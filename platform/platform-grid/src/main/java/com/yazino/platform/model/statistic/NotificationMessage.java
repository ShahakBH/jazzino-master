package com.yazino.platform.model.statistic;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

/**
 * The only purpose of this class is to keep compatibility with ParameterizedMessage
 */
public class NotificationMessage implements Serializable {
    private static final long serialVersionUID = -5731133738178059915L;

    private final String message;

    public NotificationMessage(final String message, final Object... eventParameters) {
        String formattedMessage;
        try {
            formattedMessage = String.format(message, eventParameters);
        } catch (final Exception e) {
            formattedMessage = message;
        }
        this.message = formattedMessage;
    }

    public String getMessage() {
        return message;
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
        final NotificationMessage rhs = (NotificationMessage) obj;
        return new EqualsBuilder()
                .append(message, rhs.message)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(message)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(message)
                .toString();
    }

}
