package com.yazino.platform.audit.message;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionProcessedMessage implements AuditMessage {
    private static final long serialVersionUID = -684664139522910249L;

    @JsonProperty("txs")
    private List<Transaction> transactions;

    public TransactionProcessedMessage() {
    }

    public TransactionProcessedMessage(final List<Transaction> transactions) {
        notNull(transactions, "transaction may not be null");

        this.transactions = transactions;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public AuditMessageType getMessageType() {
        return AuditMessageType.TX_PROCESSED;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(final List<Transaction> transactions) {
        this.transactions = transactions;
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
        final TransactionProcessedMessage rhs = (TransactionProcessedMessage) obj;
        return new EqualsBuilder()
                .append(transactions, rhs.transactions)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(transactions)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(transactions)
                .toString();
    }

}
