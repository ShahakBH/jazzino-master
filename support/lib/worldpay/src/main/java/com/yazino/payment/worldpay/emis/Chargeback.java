package com.yazino.payment.worldpay.emis;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;

import static org.apache.commons.lang3.Validate.notNull;

public class Chargeback implements Serializable {
    private static final long serialVersionUID = 8608500032734940663L;

    private final BigDecimal acquirerReference;
    private final String cardNumber;
    private final CardScheme cardScheme;
    private final String merchantId;
    private final String transactionId;
    private final DateTime transactionDate;
    private final BigDecimal amount;
    private final Currency currency;
    private final String cardCentreRef;
    private final String chargebackReason;
    private final DateTime processingDate;
    private final ChargebackTransactionType transactionType;
    private final BigDecimal chargebackAmount;
    private final String reasonCode;

    public Chargeback(final BigDecimal acquirerReference,
                      final String cardNumber,
                      final CardScheme cardScheme,
                      final String merchantId,
                      final String transactionId,
                      final DateTime transactionDate,
                      final BigDecimal amount,
                      final Currency currency,
                      final String cardCentreRef,
                      final String chargebackReason,
                      final DateTime processingDate,
                      final ChargebackTransactionType transactionType,
                      final BigDecimal chargebackAmount,
                      final String reasonCode) {
        notNull(acquirerReference, "acquirerReference may not be null");
        notNull(cardNumber, "cardNumber may not be null");
        notNull(cardScheme, "cardScheme may not be null");
        notNull(merchantId, "merchantId may not be null");
        notNull(transactionId, "transactionId may not be null");
        notNull(transactionDate, "transactionDate may not be null");
        notNull(amount, "amount may not be null");
        notNull(currency, "currency may not be null");
        notNull(cardCentreRef, "cardCentreRef may not be null");
        notNull(chargebackReason, "chargebackReason may not be null");
        notNull(chargebackAmount, "chargebackAmount may not be null");

        this.acquirerReference = acquirerReference;
        this.cardNumber = cardNumber;
        this.cardScheme = cardScheme;
        this.merchantId = merchantId;
        this.transactionId = transactionId;
        this.transactionDate = transactionDate;
        this.amount = amount;
        this.currency = currency;
        this.cardCentreRef = cardCentreRef;
        this.chargebackReason = chargebackReason;
        this.processingDate = processingDate;
        this.transactionType = transactionType;
        this.chargebackAmount = chargebackAmount;
        this.reasonCode = reasonCode;
    }

    public BigDecimal getAcquirerReference() {
        return acquirerReference;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public CardScheme getCardScheme() {
        return cardScheme;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public DateTime getTransactionDate() {
        return transactionDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public String getCardCentreRef() {
        return cardCentreRef;
    }

    public String getChargebackReason() {
        return chargebackReason;
    }

    public DateTime getProcessingDate() {
        return processingDate;
    }

    public ChargebackTransactionType getTransactionType() {
        return transactionType;
    }

    public BigDecimal getChargebackAmount() {
        return chargebackAmount;
    }

    public String getReasonCode() {
        return reasonCode;
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
                .append(this.acquirerReference, rhs.acquirerReference)
                .append(this.cardNumber, rhs.cardNumber)
                .append(this.cardScheme, rhs.cardScheme)
                .append(this.merchantId, rhs.merchantId)
                .append(this.transactionId, rhs.transactionId)
                .append(this.transactionDate, rhs.transactionDate)
                .append(this.amount, rhs.amount)
                .append(this.currency, rhs.currency)
                .append(this.cardCentreRef, rhs.cardCentreRef)
                .append(this.chargebackReason, rhs.chargebackReason)
                .append(this.processingDate, rhs.processingDate)
                .append(this.transactionType, rhs.transactionType)
                .append(this.chargebackAmount, rhs.chargebackAmount)
                .append(this.reasonCode, rhs.reasonCode)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(acquirerReference)
                .append(cardNumber)
                .append(cardScheme)
                .append(merchantId)
                .append(transactionId)
                .append(transactionDate)
                .append(amount)
                .append(currency)
                .append(cardCentreRef)
                .append(chargebackReason)
                .append(processingDate)
                .append(transactionType)
                .append(chargebackAmount)
                .append(reasonCode)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("acquirerReference", acquirerReference)
                .append("cardNumber", cardNumber)
                .append("cardScheme", cardScheme)
                .append("merchantId", merchantId)
                .append("transactionId", transactionId)
                .append("transactionDate", transactionDate)
                .append("amount", amount)
                .append("currency", currency)
                .append("cardCentreRef", cardCentreRef)
                .append("chargebackReason", chargebackReason)
                .append("processingDate", processingDate)
                .append("transactionType", transactionType)
                .append("chargebackAmount", chargebackAmount)
                .append("reasonCode", reasonCode)
                .toString();
    }
}
