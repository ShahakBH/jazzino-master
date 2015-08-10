package com.yazino.platform.player;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.yazino.game.api.ParameterisedMessage;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Encapsulates a response that may contain errors or success.
 */
public class PlayerProfileServiceResponse implements Serializable {

    private static final long serialVersionUID = 4258373688681736585L;

    private final Set<ParameterisedMessage> errors = new HashSet<ParameterisedMessage>();

    private final boolean successful;

    public PlayerProfileServiceResponse(final boolean successful) {
        this.successful = successful;
    }

    public PlayerProfileServiceResponse(final Set<ParameterisedMessage> errors,
                                        final boolean successful) {
        notNull(errors, "errors must not be null");

        if (errors != null) {
            this.errors.addAll(errors);
        }
        this.successful = successful;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public Set<ParameterisedMessage> getErrors() {
        return errors;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj.getClass() != getClass()) {
            return false;
        }

        final PlayerProfileServiceResponse rhs = (PlayerProfileServiceResponse) obj;
        return new EqualsBuilder()
                .append(successful, rhs.successful)
                .append(errors, rhs.errors)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(successful)
                .append(errors)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
