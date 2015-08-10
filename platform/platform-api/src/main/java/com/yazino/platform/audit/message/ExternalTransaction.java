package com.yazino.platform.audit.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yazino.platform.Platform;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

public class ExternalTransaction implements Serializable {
    private static final long serialVersionUID = 6140416388170905790L;

    @JsonProperty("acc")
    private BigDecimal accountId;
    @JsonProperty("itx")
    private String internalTransactionId;
    @JsonProperty("etx")
    private String externalTransactionId;
    @JsonProperty("ccm")
    private String creditCardObscuredMessage;
    @JsonProperty("ts")
    private Date messageTimeStamp;
    @JsonProperty("cur")
    private String currency;
    @JsonProperty("csh")
    private BigDecimal amountCash;
    @JsonProperty("chi")
    private BigDecimal amountChips;
    @JsonProperty("cc")
    private String obscuredCreditCardNumber;
    @JsonProperty("src")
    private String cashierName;
    @JsonProperty("gt")
    private String gameType;
    @JsonProperty("ets")
    private String externalTransactionStatus;
    @JsonProperty("tlt")
    private String transactionLogType;
    @JsonProperty("pid")
    private BigDecimal playerId;
    @JsonProperty("pro")
    private Long promoId;
    @JsonProperty("plt")
    private Platform platform;
    @JsonProperty("poi")
    private String paymentOptionId;
    @JsonProperty("fxc")
    private String baseCurrency;
    @JsonProperty("fxa")
    private BigDecimal baseCurrencyAmount;
    @JsonProperty("fxr")
    private BigDecimal exchangeRate;
    @JsonProperty("fr")
    private String failureReason;

    public ExternalTransaction() {
    }

    public ExternalTransaction(final BigDecimal accountId,
                               final String internalTransactionId,
                               final String externalTransactionId,
                               final String creditCardObscuredMessage,
                               final Date messageTimeStamp,
                               final String currency,
                               final BigDecimal amountCash,
                               final BigDecimal amountChips,
                               final String obscuredCreditCardNumber,
                               final String cashierName,
                               final String gameType,
                               final String externalTransactionStatus,
                               final String transactionLogType,
                               final BigDecimal playerId,
                               final Long promoId,
                               final Platform platform,
                               final String paymentOptionId,
                               final String baseCurrency,
                               final BigDecimal baseCurrencyAmount,
                               final BigDecimal exchangeRate,
                               final String failureReason) {
        this.accountId = accountId;
        this.internalTransactionId = internalTransactionId;
        this.externalTransactionId = externalTransactionId;
        this.creditCardObscuredMessage = creditCardObscuredMessage;
        this.messageTimeStamp = messageTimeStamp;
        this.currency = currency;
        this.amountCash = amountCash;
        this.amountChips = amountChips;
        this.obscuredCreditCardNumber = obscuredCreditCardNumber;
        this.cashierName = cashierName;
        this.gameType = gameType;
        this.externalTransactionStatus = externalTransactionStatus;
        this.transactionLogType = transactionLogType;
        this.playerId = playerId;
        this.promoId = promoId;
        this.platform = platform;
        this.paymentOptionId = paymentOptionId;
        this.baseCurrency = baseCurrency;
        this.baseCurrencyAmount = baseCurrencyAmount;
        this.exchangeRate = exchangeRate;
        this.failureReason = failureReason;
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

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public void setPromoId(final Long promoId) {
        this.promoId = promoId;
    }

    public void setPlatform(final Platform platform) {
        this.platform = platform;
    }

    public String getTransactionLogType() {
        return transactionLogType;
    }

    public void setTransactionLogType(final String transactionLogType) {
        this.transactionLogType = transactionLogType;
    }

    public BigDecimal getAccountId() {
        return accountId;
    }

    public void setAccountId(final BigDecimal accountId) {
        this.accountId = accountId;
    }

    public String getInternalTransactionId() {
        return internalTransactionId;
    }

    public void setInternalTransactionId(final String internalTransactionId) {
        this.internalTransactionId = internalTransactionId;
    }

    public String getExternalTransactionId() {
        return externalTransactionId;
    }

    public void setExternalTransactionId(final String externalTransactionId) {
        this.externalTransactionId = externalTransactionId;
    }

    public String getCreditCardObscuredMessage() {
        return creditCardObscuredMessage;
    }

    public void setCreditCardObscuredMessage(final String creditCardObscuredMessage) {
        this.creditCardObscuredMessage = creditCardObscuredMessage;
    }

    public Date getMessageTimeStamp() {
        return messageTimeStamp;
    }

    public void setMessageTimeStamp(final Date messageTimeStamp) {
        this.messageTimeStamp = messageTimeStamp;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(final String currency) {
        this.currency = currency;
    }

    public BigDecimal getAmountCash() {
        return amountCash;
    }

    public void setAmountCash(final BigDecimal amountCash) {
        this.amountCash = amountCash;
    }

    public BigDecimal getAmountChips() {
        return amountChips;
    }

    public void setAmountChips(final BigDecimal amountChips) {
        this.amountChips = amountChips;
    }

    public String getObscuredCreditCardNumber() {
        return obscuredCreditCardNumber;
    }

    public void setObscuredCreditCardNumber(final String obscuredCreditCardNumber) {
        this.obscuredCreditCardNumber = obscuredCreditCardNumber;
    }

    public String getCashierName() {
        return cashierName;
    }

    public void setCashierName(final String cashierName) {
        this.cashierName = cashierName;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(final String gameType) {
        this.gameType = gameType;
    }

    public String getExternalTransactionStatus() {
        return externalTransactionStatus;
    }

    public void setExternalTransactionStatus(final String externalTransactionStatus) {
        this.externalTransactionStatus = externalTransactionStatus;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(final String failureReason) {
        this.failureReason = failureReason;
    }

    public String getPaymentOptionId() {
        return paymentOptionId;
    }

    public void setPaymentOptionId(final String paymentOptionId) {
        this.paymentOptionId = paymentOptionId;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(final String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public BigDecimal getBaseCurrencyAmount() {
        return baseCurrencyAmount;
    }

    public void setBaseCurrencyAmount(final BigDecimal baseCurrencyAmount) {
        this.baseCurrencyAmount = baseCurrencyAmount;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(final BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ExternalTransaction rhs = (ExternalTransaction) o;
        return new EqualsBuilder()
                .append(internalTransactionId, rhs.internalTransactionId)
                .append(externalTransactionId, rhs.externalTransactionId)
                .append(creditCardObscuredMessage, rhs.creditCardObscuredMessage)
                .append(messageTimeStamp, rhs.messageTimeStamp)
                .append(currency, rhs.currency)
                .append(amountCash, rhs.amountCash)
                .append(amountChips, rhs.amountChips)
                .append(obscuredCreditCardNumber, rhs.obscuredCreditCardNumber)
                .append(cashierName, rhs.cashierName)
                .append(externalTransactionStatus, rhs.externalTransactionStatus)
                .append(gameType, rhs.gameType)
                .append(transactionLogType, rhs.transactionLogType)
                .append(paymentOptionId, rhs.paymentOptionId)
                .append(baseCurrency, rhs.baseCurrency)
                .append(baseCurrencyAmount, rhs.baseCurrencyAmount)
                .append(exchangeRate, rhs.exchangeRate)
                .append(failureReason, rhs.failureReason)
                .isEquals()
                && BigDecimals.equalByComparison(accountId, rhs.accountId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(accountId))
                .append(internalTransactionId)
                .append(externalTransactionId)
                .append(creditCardObscuredMessage)
                .append(messageTimeStamp)
                .append(currency)
                .append(amountCash)
                .append(amountChips)
                .append(obscuredCreditCardNumber)
                .append(cashierName)
                .append(externalTransactionStatus)
                .append(gameType)
                .append(transactionLogType)
                .append(paymentOptionId)
                .append(baseCurrency)
                .append(baseCurrencyAmount)
                .append(exchangeRate)
                .append(failureReason)
                .toHashCode();
    }

    public String toString() {
        return new ToStringBuilder(this)
                .append(accountId)
                .append(internalTransactionId)
                .append(externalTransactionId)
                .append(creditCardObscuredMessage)
                .append(messageTimeStamp)
                .append(currency)
                .append(amountCash)
                .append(amountChips)
                .append(obscuredCreditCardNumber)
                .append(cashierName)
                .append(externalTransactionStatus)
                .append(gameType)
                .append(transactionLogType)
                .append(paymentOptionId)
                .append(baseCurrency)
                .append(baseCurrencyAmount)
                .append(exchangeRate)
                .append(failureReason)
                .toString();
    }
}
