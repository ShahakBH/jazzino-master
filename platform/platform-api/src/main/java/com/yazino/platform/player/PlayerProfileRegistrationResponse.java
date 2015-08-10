package com.yazino.platform.player;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.yazino.game.api.ParameterisedMessage;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

public class PlayerProfileRegistrationResponse extends PlayerProfileServiceResponse {
    private static final long serialVersionUID = -1365037865755534135L;

    private final BigDecimal playerId;

    public PlayerProfileRegistrationResponse(final ParameterisedMessage... errors) {
        this(new HashSet<ParameterisedMessage>(Arrays.asList(errors)));
    }

    public PlayerProfileRegistrationResponse(final Set<ParameterisedMessage> errors) {
        super(errors, false);

        this.playerId = null;
    }

    public PlayerProfileRegistrationResponse(final BigDecimal playerId) {
        super(true);

        notNull(playerId, "playerId must not be null");

        this.playerId = playerId;
    }

    public BigDecimal getPlayerId() {
        return playerId;
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
        final PlayerProfileRegistrationResponse rhs = (PlayerProfileRegistrationResponse) obj;
        return new EqualsBuilder()
                .appendSuper(super.equals(obj))
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(BigDecimals.strip(playerId))
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .appendSuper(super.toString())
                .append(playerId)
                .toString();
    }
}
