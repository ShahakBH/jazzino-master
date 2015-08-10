package com.yazino.platform.audit.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;


@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalTransactionMessage extends ExternalTransaction implements AuditMessage {
    private static final long serialVersionUID = 9022944648975236757L;

    public ExternalTransactionMessage() {
    }

    public ExternalTransactionMessage(final ExternalTransaction externalTransaction) {
        super(externalTransaction.getAccountId(),
                externalTransaction.getInternalTransactionId(),
                externalTransaction.getExternalTransactionId(),
                externalTransaction.getCreditCardObscuredMessage(),
                externalTransaction.getMessageTimeStamp(),
                externalTransaction.getCurrency(),
                externalTransaction.getAmountCash(),
                externalTransaction.getAmountChips(),
                externalTransaction.getObscuredCreditCardNumber(),
                externalTransaction.getCashierName(),
                externalTransaction.getGameType(),
                externalTransaction.getExternalTransactionStatus(),
                externalTransaction.getTransactionLogType(),
                externalTransaction.getPlayerId(),
                externalTransaction.getPromoId(),
                externalTransaction.getPlatform(),
                externalTransaction.getPaymentOptionId(),
                externalTransaction.getBaseCurrency(),
                externalTransaction.getBaseCurrencyAmount(),
                externalTransaction.getExchangeRate(),
                externalTransaction.getFailureReason());
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public AuditMessageType getMessageType() {
        return AuditMessageType.EXTERNAL_TX;
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
        final ExternalTransactionMessage rhs = (ExternalTransactionMessage) obj;
        return new EqualsBuilder()
                .append(getVersion(), rhs.getVersion())
                .appendSuper(super.equals(obj))
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 19)
                .append(getVersion())
                .append(getMessageType())
                .appendSuper(super.hashCode())
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(getVersion())
                .append(getMessageType())
                .appendSuper(super.toString())
                .toString();
    }

}
