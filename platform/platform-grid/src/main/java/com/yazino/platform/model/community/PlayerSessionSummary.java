package com.yazino.platform.model.community;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

public class PlayerSessionSummary implements Serializable {
    private static final long serialVersionUID = -2823410337149580820L;

    private final BigDecimal playerId;
    private final BigDecimal accountId;
    private final BigDecimal sessionId;
    private final String name;

    public PlayerSessionSummary(final BigDecimal playerId,
                                final BigDecimal accountId,
                                final String name,
                                final BigDecimal sessionId) {
        notNull(playerId, "playerId may not be null");
        notNull(accountId, "accountId may not be null");

        this.playerId = playerId;
        this.accountId = accountId;
        this.name = name;
        this.sessionId = sessionId;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public BigDecimal getAccountId() {
        return accountId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getSessionId() {
        return sessionId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        PlayerSessionSummary rhs = (PlayerSessionSummary) obj;
        return new EqualsBuilder()
                .append(this.name, rhs.name)
                .append(this.sessionId, rhs.sessionId)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId)
                && BigDecimals.equalByComparison(accountId, rhs.accountId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(BigDecimals.strip(accountId))
                .append(name)
                .append(sessionId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("playerId", playerId)
                .append("accountId", accountId)
                .append("name", name)
                .append("sessionId", sessionId)
                .toString();
    }
}
