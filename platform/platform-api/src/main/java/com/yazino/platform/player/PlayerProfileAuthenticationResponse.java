package com.yazino.platform.player;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Encapsulates the response from an Authentication attempt.
 */
public class PlayerProfileAuthenticationResponse extends PlayerProfileServiceResponse {
    private static final long serialVersionUID = 7489500861490670473L;

    private final BigDecimal playerId;
    private final boolean blocked;

    public PlayerProfileAuthenticationResponse() {
        super(false);

        this.playerId = null;
        this.blocked = false;
    }

    public PlayerProfileAuthenticationResponse(final BigDecimal playerId) {
        super(true);

        notNull(playerId, "playerId must not be null");

        this.playerId = playerId;
        this.blocked = false;
    }

    public PlayerProfileAuthenticationResponse(final BigDecimal playerId,
                                               final boolean blocked) {
        super(!blocked);

        notNull(playerId, "playerId must not be null");

        this.blocked = blocked;
        this.playerId = playerId;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public boolean isBlocked() {
        return blocked;
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

        final PlayerProfileAuthenticationResponse rhs = (PlayerProfileAuthenticationResponse) obj;
        return new EqualsBuilder()
                .appendSuper(super.equals(rhs))
                .append(blocked, rhs.blocked)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(BigDecimals.strip(playerId))
                .append(blocked)
                .toHashCode();
    }

}
