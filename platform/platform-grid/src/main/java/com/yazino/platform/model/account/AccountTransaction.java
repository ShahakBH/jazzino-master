package com.yazino.platform.model.account;

import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

public final class AccountTransaction implements Serializable {
    private static final long serialVersionUID = -3213838871408301408L;

    private final BigDecimal accountId;
    private final BigDecimal amount;
    private final String type;
    private final String reference;
    private final Long timestamp;
    private final BigDecimal runningBalance;
    private final TransactionContext transactionContext;

    public AccountTransaction(final BigDecimal accountId,
                              final BigDecimal amount,
                              final String type,
                              final String reference) {
        notNull(accountId, "Account ID may not be null");
        notNull(amount, "Amount may not be null");
        notNull(type, "Type may not be null");

        this.accountId = accountId;
        this.amount = amount;
        this.type = type;
        this.reference = reference;
        this.timestamp = null;
        this.runningBalance = null;
        this.transactionContext = TransactionContext.EMPTY;
    }

    public AccountTransaction(final AccountTransaction accountTransaction,
                              final Long timestamp,
                              final BigDecimal runningBalance) {
        notNull(accountTransaction, "accountTransaction may not be null");

        this.accountId = accountTransaction.accountId;
        this.amount = accountTransaction.amount;
        this.type = accountTransaction.type;
        this.reference = accountTransaction.reference;
        this.timestamp = timestamp;
        this.runningBalance = runningBalance;
        this.transactionContext = accountTransaction.getTransactionContext();
    }

    public AccountTransaction(final BigDecimal accountId,
                              final BigDecimal amount,
                              final String transactionType,
                              final String reference,
                              final TransactionContext transactionContext) {
        notNull(accountId, "Account ID may not be null");
        notNull(amount, "Amount may not be null");
        notNull(transactionType, "Type may not be null");
        notNull(transactionContext, "transactionContext may not be null");

        this.accountId = accountId;
        this.amount = amount;
        this.type = transactionType;
        this.reference = reference;
        this.timestamp = null;
        this.runningBalance = null;
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

    public String getReference() {
        return reference;
    }

    public String getType() {
        return type;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public BigDecimal getRunningBalance() {
        return runningBalance;
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

        final AccountTransaction rhs = (AccountTransaction) obj;
        return new EqualsBuilder()
                .append(amount, rhs.amount)
                .append(reference, rhs.reference)
                .append(type, rhs.type)
                .append(timestamp, rhs.timestamp)
                .append(runningBalance, rhs.runningBalance)
                .append(transactionContext, rhs.transactionContext)
                .isEquals()
                && BigDecimals.equalByComparison(accountId, rhs.accountId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(accountId))
                .append(amount)
                .append(reference)
                .append(type)
                .append(timestamp)
                .append(runningBalance)
                .append(transactionContext)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
