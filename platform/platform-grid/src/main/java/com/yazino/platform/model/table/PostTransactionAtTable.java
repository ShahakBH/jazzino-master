package com.yazino.platform.model.table;

import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

public class PostTransactionAtTable implements Serializable {
    private static final long serialVersionUID = -6683144460629886797L;

    private final BigDecimal playerId;
    private final BigDecimal accountId;
    private final BigDecimal amount;
    private final String transactionType;
    private final String reference;
    private final TransactionContext transactionContext;
    private final String uniqueId;

    public PostTransactionAtTable(final BigDecimal playerId,
                                  final BigDecimal accountId,
                                  final BigDecimal amount,
                                  final String transactionType,
                                  final String reference,
                                  final String uniqueId,
                                  final TransactionContext transactionContext) {
        notNull(transactionContext, "transactionContext may not be null");

        this.playerId = playerId;
        this.accountId = accountId;
        this.amount = amount;
        this.transactionType = transactionType;
        this.reference = reference;
        this.uniqueId = uniqueId;
        this.transactionContext = transactionContext;
    }

    public TransactionContext getTransactionContext() {
        return transactionContext;
    }

    public BigDecimal getAccountId() {
        return accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public String getReference() {
        return reference;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public String getTransactionReference() {
        return reference;
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
        final PostTransactionAtTable rhs = (PostTransactionAtTable) obj;
        return new EqualsBuilder()
                .append(amount, rhs.amount)
                .append(reference, rhs.reference)
                .append(transactionContext, rhs.transactionContext)
                .append(transactionType, rhs.transactionType)
                .append(uniqueId, rhs.uniqueId)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId)
                && BigDecimals.equalByComparison(accountId, rhs.accountId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(accountId))
                .append(amount)
                .append(BigDecimals.strip(playerId))
                .append(reference)
                .append(transactionContext)
                .append(transactionType)
                .append(uniqueId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(accountId)
                .append(amount)
                .append(playerId)
                .append(reference)
                .append(transactionContext)
                .append(transactionType)
                .append(uniqueId)
                .toString();
    }
}
