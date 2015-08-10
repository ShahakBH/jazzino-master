package com.yazino.web.session;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

public class ReferralResult implements Serializable {
    private static final long serialVersionUID = 7135763818524207918L;
    private BigDecimal amount;
    private final BigDecimal playerId;

    public ReferralResult(final BigDecimal playerId, final BigDecimal amount) {
        notNull(playerId, "playerId is null");
        notNull(amount, "amount is null");
        this.playerId = playerId;
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
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
        final ReferralResult rhs = (ReferralResult) obj;
        return new EqualsBuilder()
                .append(playerId, rhs.playerId)
                .append(amount, rhs.amount)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(playerId)
                .append(amount)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .append(amount)
                .toString();
    }

}
