package com.yazino.bi.payment;

import com.google.common.base.Optional;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;

import static org.apache.commons.lang3.Validate.notNull;

public class Chargeback implements Serializable {
    private static final long serialVersionUID = -5747565962166115795L;

    private final String reference;
    private final DateTime processingDate;
    private final String internalTransactionId;
    private final DateTime transactionDate;
    private final BigDecimal playerId;
    private final String displayName;
    private final String reasonCode;
    private final String reason;
    private final String accountNumber;
    private final BigDecimal amount;
    private final Currency currency;

    public Chargeback(final String reference,
                      final DateTime processingDate,
                      final String internalTransactionId,
                      final DateTime transactionDate,
                      final BigDecimal playerId,
                      final String displayName,
                      final String reasonCode,
                      final String reason,
                      final String accountNumber,
                      final BigDecimal amount,
                      final Currency currency) {
        notNull(reference, "reference may not be null");
        notNull(processingDate, "processingDate may not be null");
        notNull(internalTransactionId, "internalTransactionId may not be null");
        notNull(transactionDate, "transactionDate may not be null");
        notNull(playerId, "playerId may not be null");
        notNull(reason, "reason may not be null");
        notNull(accountNumber, "accountNumber may not be null");
        notNull(amount, "amount may not be null");
        notNull(currency, "currency may not be null");

        this.reference = reference;
        this.processingDate = processingDate;
        this.internalTransactionId = internalTransactionId;
        this.transactionDate = transactionDate;
        this.playerId = playerId;
        this.displayName = displayName;
        this.reasonCode = reasonCode;
        this.reason = reason;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.currency = currency;
    }

    public String getReference() {
        return reference;
    }

    public DateTime getProcessingDate() {
        return processingDate;
    }

    public String getInternalTransactionId() {
        return internalTransactionId;
    }

    public DateTime getTransactionDate() {
        return transactionDate;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public Optional<String> getReasonCode() {
        return Optional.fromNullable(reasonCode);
    }

    public String getReason() {
        return reason;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public String getDisplayName() {
        return displayName;
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
        Chargeback rhs = (Chargeback) obj;
        return new EqualsBuilder()
                .append(this.reference, rhs.reference)
                .append(this.processingDate, rhs.processingDate)
                .append(this.internalTransactionId, rhs.internalTransactionId)
                .append(this.transactionDate, rhs.transactionDate)
                .append(this.playerId, rhs.playerId)
                .append(this.displayName, rhs.displayName)
                .append(this.reasonCode, rhs.reasonCode)
                .append(this.reason, rhs.reason)
                .append(this.accountNumber, rhs.accountNumber)
                .append(this.amount, rhs.amount)
                .append(this.currency, rhs.currency)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(reference)
                .append(processingDate)
                .append(internalTransactionId)
                .append(transactionDate)
                .append(playerId)
                .append(displayName)
                .append(reasonCode)
                .append(reason)
                .append(accountNumber)
                .append(amount)
                .append(currency)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("reference", reference)
                .append("processingDate", processingDate)
                .append("internalTransactionId", internalTransactionId)
                .append("transactionDate", transactionDate)
                .append("playerId", playerId)
                .append("displayName", displayName)
                .append("reasonCode", reasonCode)
                .append("reason", reason)
                .append("accountNumber", accountNumber)
                .append("amount", amount)
                .append("currency", currency)
                .toString();
    }
}
