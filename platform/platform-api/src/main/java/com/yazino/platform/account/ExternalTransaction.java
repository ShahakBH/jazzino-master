package com.yazino.platform.account;

import com.yazino.platform.Platform;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;

public class ExternalTransaction implements Serializable {
    private static final long serialVersionUID = 6140416388170975690L;

    private final BigDecimal accountId;
    private final String internalTransactionId;
    private final String externalTransactionId;
    private final String creditCardObscuredMessage;
    private final DateTime messageTimeStamp;
    private final Amount amount;
    private final BigDecimal amountChips;
    private final String obscuredCreditCardNumber;
    private final String cashierName;
    private final String gameType;
    private final ExternalTransactionStatus status;
    private final ExternalTransactionType type;
    private final BigDecimal playerId;
    private final BigDecimal sessionId;
    private final Long promoId;
    private final Platform platform;
    private final String paymentOptionId;
    private final BigDecimal exchangeRate;
    private final Amount baseCurrencyAmount;
    private final String failureReason;

    ExternalTransaction(final BigDecimal accountId,
                        final String internalTransactionId,
                        final String externalTransactionId,
                        final String creditCardObscuredMessage,
                        final DateTime messageTimeStamp,
                        final Amount amount,
                        final BigDecimal amountChips,
                        final String obscuredCreditCardNumber,
                        final String cashierName,
                        final ExternalTransactionStatus status,
                        final ExternalTransactionType type,
                        final String gameType,
                        final BigDecimal playerId,
                        final BigDecimal sessionId,
                        final Long promoId,
                        final Platform platform,
                        final String paymentOptionId,
                        final BigDecimal exchangeRate,
                        final Amount baseCurrencyAmount,
                        final String failureReason) {
        this.accountId = accountId;
        this.internalTransactionId = internalTransactionId;
        this.externalTransactionId = externalTransactionId;
        this.creditCardObscuredMessage = creditCardObscuredMessage;
        this.messageTimeStamp = messageTimeStamp;
        this.amount = amount;
        this.amountChips = amountChips;
        this.obscuredCreditCardNumber = obscuredCreditCardNumber;
        this.cashierName = cashierName;
        this.status = status;
        this.type = type;
        this.gameType = gameType;
        this.playerId = playerId;
        this.sessionId = sessionId;
        this.promoId = promoId;
        this.platform = platform;
        this.paymentOptionId = paymentOptionId;
        this.exchangeRate = exchangeRate;
        this.baseCurrencyAmount = baseCurrencyAmount;
        this.failureReason = failureReason;
    }

    public static ExternalTransactionBuilder newExternalTransaction(final BigDecimal accountId) {
        return new ExternalTransactionBuilder(accountId);
    }

    public static ExternalTransactionBuilder copyExternalTransaction(final ExternalTransaction externalTransaction) {
        return new ExternalTransactionBuilder(externalTransaction);
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public Long getPromoId() {
        return promoId;
    }

    public Platform getPlatform() {
        return platform;
    }

    public ExternalTransactionType getType() {
        return type;
    }

    public BigDecimal getAccountId() {
        return accountId;
    }

    public String getCashierName() {
        return cashierName;
    }

    public BigDecimal getAmountChips() {
        return amountChips;
    }

    public String getInternalTransactionId() {
        return internalTransactionId;
    }

    public String getExternalTransactionId() {
        return externalTransactionId;
    }

    public String getCreditCardObscuredMessage() {
        return creditCardObscuredMessage;
    }

    public DateTime getMessageTimeStamp() {
        return messageTimeStamp;
    }

    public String getFailureReason() {
        return failureReason;
    }

    /**
     * @deprecated use {@link #getAmount()}
     */
    public Currency getCurrency() {
        if (amount != null) {
            return amount.getCurrency();
        }
        return null;
    }

    /**
     * @deprecated use {@link #getAmount()}
     */
    public BigDecimal getAmountCash() {
        if (amount != null) {
            return amount.getQuantity();
        }
        return null;
    }

    public Amount getAmount() {
        return amount;
    }

    public String getPaymentOptionId() {
        return paymentOptionId;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public Amount getBaseCurrencyAmount() {
        return baseCurrencyAmount;
    }

    public String getObscuredCreditCardNumber() {
        return obscuredCreditCardNumber;
    }

    public ExternalTransactionStatus getStatus() {
        return status;
    }

    public BigDecimal getSessionId() {
        return sessionId;
    }

    public String getTransactionLogType() {
        final String transactionTypeString = type.toString();
        final String subStr = transactionTypeString.substring(1, transactionTypeString.length()).toLowerCase();
        return cashierName + " " + transactionTypeString.toCharArray()[0] + subStr;
    }

    public String getGameType() {
        return gameType;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ExternalTransaction that = (ExternalTransaction) o;
        return new EqualsBuilder()
                .append(internalTransactionId, that.internalTransactionId)
                .append(externalTransactionId, that.externalTransactionId)
                .append(creditCardObscuredMessage, that.creditCardObscuredMessage)
                .append(messageTimeStamp, that.messageTimeStamp)
                .append(baseCurrencyAmount, that.baseCurrencyAmount)
                .append(amountChips, that.amountChips)
                .append(obscuredCreditCardNumber, that.obscuredCreditCardNumber)
                .append(cashierName, that.cashierName)
                .append(status, that.status)
                .append(type, that.type)
                .append(gameType, that.gameType)
                .append(promoId, that.promoId)
                .append(platform, that.platform)
                .append(paymentOptionId, that.paymentOptionId)
                .append(baseCurrencyAmount, that.baseCurrencyAmount)
                .append(exchangeRate, that.exchangeRate)
                .append(failureReason, that.failureReason)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, that.playerId)
                && BigDecimals.equalByComparison(sessionId, that.sessionId)
                && BigDecimals.equalByComparison(accountId, that.accountId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(accountId))
                .append(internalTransactionId)
                .append(externalTransactionId)
                .append(creditCardObscuredMessage)
                .append(messageTimeStamp)
                .append(amount)
                .append(amountChips)
                .append(obscuredCreditCardNumber)
                .append(cashierName)
                .append(status)
                .append(type)
                .append(gameType)
                .append(BigDecimals.strip(playerId))
                .append(BigDecimals.strip(sessionId))
                .append(promoId)
                .append(platform)
                .append(paymentOptionId)
                .append(baseCurrencyAmount)
                .append(exchangeRate)
                .append(failureReason)
                .toHashCode();
    }

    public String toString() {
        return new ReflectionToStringBuilder(this).toString();
    }
}
