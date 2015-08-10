package com.yazino.platform.player;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

public class PlayerProfileLoginResponse implements Serializable {
    private static final long serialVersionUID = 8127587410227529014L;

    private final LoginResult loginResult;
    private final BigDecimal playerId;

    public PlayerProfileLoginResponse(final LoginResult loginResult) {
        notNull(loginResult, "loginResult may not be null");

        this.loginResult = loginResult;
        this.playerId = null;
    }

    public PlayerProfileLoginResponse(final BigDecimal playerId,
                                      final LoginResult loginResult) {
        notNull(loginResult, "loginResult may not be null");

        this.playerId = playerId;
        this.loginResult = loginResult;
    }

    public LoginResult getLoginResult() {
        return loginResult;
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
        final PlayerProfileLoginResponse rhs = (PlayerProfileLoginResponse) obj;
        return new EqualsBuilder()
                .append(loginResult, rhs.loginResult)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(loginResult)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .append(loginResult)
                .toString();
    }
}
