package com.yazino.game.api;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

public final class TransactionResult implements Serializable {
    private static final long serialVersionUID = -4604476897149855066L;
    private final String uniqueId;
    private final boolean successful;
    private final String errorReason;
    private BigDecimal accountId;
    private BigDecimal balance;
    private final BigDecimal playerId;

    public TransactionResult(final String uniqueId,
                             final boolean successful,
                             final String errorReason,
                             final BigDecimal accountId,
                             final BigDecimal balance,
                             final BigDecimal playerId) {
        this.uniqueId = uniqueId;
        this.successful = successful;
        this.errorReason = errorReason;
        this.accountId = accountId;
        this.balance = balance;
        this.playerId = playerId;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getErrorReason() {
        return errorReason;
    }

    public BigDecimal getAccountId() {
        return accountId;
    }

    public BigDecimal getBalance() {
        return balance;
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
        final TransactionResult rhs = (TransactionResult) obj;
        return new EqualsBuilder()
                .append(uniqueId, rhs.uniqueId)
                .append(successful, rhs.successful)
                .append(errorReason, rhs.errorReason)
                .append(accountId, rhs.accountId)
                .append(balance, rhs.balance)
                .append(playerId, rhs.playerId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(uniqueId)
                .append(successful)
                .append(errorReason)
                .append(accountId)
                .append(balance)
                .append(playerId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(uniqueId)
                .append(successful)
                .append(errorReason)
                .append(accountId)
                .append(balance)
                .append(playerId)
                .toString();
    }

}
