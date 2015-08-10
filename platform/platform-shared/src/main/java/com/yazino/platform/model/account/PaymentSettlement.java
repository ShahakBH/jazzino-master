package com.yazino.platform.model.account;

import com.yazino.platform.Platform;
import com.yazino.platform.account.ExternalTransactionType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.math.BigDecimal;
import java.util.Currency;

import static org.apache.commons.lang3.Validate.notNull;

public final class PaymentSettlement {
    private final String internalTransactionId;
    private final String externalTransactionId;
    private final BigDecimal accountId;
    private final BigDecimal playerId;
    private final String cashierName;
    private final DateTime timestamp;
    private final String accountNumber;
    private final BigDecimal price;
    private final Currency currency;
    private final BigDecimal chips;
    private final ExternalTransactionType transactionType;
    private final String gameType;
    private final Platform platform;
    private final String paymentOptionId;
    private final Long promotionId;
    private final BigDecimal baseCurrencyAmount;
    private final Currency baseCurrency;
    private final BigDecimal exchangeRate;

    private PaymentSettlement(final String internalTransactionId,
                              final String externalTransactionId,
                              final BigDecimal playerId,
                              final BigDecimal accountId,
                              final String cashierName,
                              final DateTime timestamp,
                              final String accountNumber,
                              final BigDecimal price,
                              final Currency currency,
                              final BigDecimal chips,
                              final ExternalTransactionType transactionType,
                              final String gameType,
                              final Platform platform,
                              final String paymentOptionId,
                              final Long promotionId,
                              final BigDecimal baseCurrencyAmount,
                              final Currency baseCurrency,
                              final BigDecimal exchangeRate) {
        notNull(internalTransactionId, "internalTransactionId may not be null");
        notNull(externalTransactionId, "externalTransactionId may not be null");
        notNull(playerId, "playerId may not be null");
        notNull(accountId, "accountId may not be null");
        notNull(cashierName, "cashierName may not be null");
        notNull(timestamp, "timestamp may not be null");
        notNull(accountNumber, "accountNumber may not be null");
        notNull(price, "price may not be null");
        notNull(currency, "currency may not be null");
        notNull(chips, "chips may not be null");
        notNull(transactionType, "transactionType may not be null");

        this.internalTransactionId = internalTransactionId;
        this.externalTransactionId = externalTransactionId;
        this.playerId = playerId;
        this.accountId = accountId;
        this.cashierName = cashierName;
        this.timestamp = timestamp;
        this.accountNumber = accountNumber;
        this.price = price;
        this.currency = currency;
        this.chips = chips;
        this.transactionType = transactionType;
        this.gameType = gameType;
        this.platform = platform;
        this.paymentOptionId = paymentOptionId;
        this.promotionId = promotionId;
        this.baseCurrencyAmount = baseCurrencyAmount;
        this.baseCurrency = baseCurrency;
        this.exchangeRate = exchangeRate;
    }

    public static PaymentSettlementBuilder newSettlement(final String internalTransactionId,
                                                         final String externalTransactionId,
                                                         final BigDecimal playerId,
                                                         final BigDecimal accountId,
                                                         final String cashierName,
                                                         final DateTime timestamp,
                                                         final String accountNumber,
                                                         final BigDecimal price,
                                                         final Currency currency,
                                                         final BigDecimal chips,
                                                         final ExternalTransactionType transactionType) {
        return new PaymentSettlementBuilder(internalTransactionId,
                externalTransactionId,
                playerId,
                accountId,
                cashierName,
                timestamp,
                accountNumber,
                price,
                currency,
                chips,
                transactionType);
    }

    public String getInternalTransactionId() {
        return internalTransactionId;
    }

    public String getExternalTransactionId() {
        return externalTransactionId;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public BigDecimal getAccountId() {
        return accountId;
    }

    public String getCashierName() {
        return cashierName;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Currency getCurrency() {
        return currency;
    }

    public BigDecimal getChips() {
        return chips;
    }

    public ExternalTransactionType getTransactionType() {
        return transactionType;
    }

    public String getGameType() {
        return gameType;
    }

    public Platform getPlatform() {
        return platform;
    }

    public String getPaymentOptionId() {
        return paymentOptionId;
    }

    public Long getPromotionId() {
        return promotionId;
    }

    public BigDecimal getBaseCurrencyAmount() {
        return baseCurrencyAmount;
    }

    public Currency getBaseCurrency() {
        return baseCurrency;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
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
        PaymentSettlement rhs = (PaymentSettlement) obj;
        return new EqualsBuilder()
                .append(this.internalTransactionId, rhs.internalTransactionId)
                .append(this.externalTransactionId, rhs.externalTransactionId)
                .append(this.playerId, rhs.playerId)
                .append(this.accountId, rhs.accountId)
                .append(this.cashierName, rhs.cashierName)
                .append(this.timestamp, rhs.timestamp)
                .append(this.accountNumber, rhs.accountNumber)
                .append(this.price, rhs.price)
                .append(this.currency, rhs.currency)
                .append(this.chips, rhs.chips)
                .append(this.transactionType, rhs.transactionType)
                .append(this.gameType, rhs.gameType)
                .append(this.platform, rhs.platform)
                .append(this.paymentOptionId, rhs.paymentOptionId)
                .append(this.promotionId, rhs.promotionId)
                .append(this.baseCurrencyAmount, rhs.baseCurrencyAmount)
                .append(this.baseCurrency, rhs.baseCurrency)
                .append(this.exchangeRate, rhs.exchangeRate)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(internalTransactionId)
                .append(externalTransactionId)
                .append(playerId)
                .append(accountId)
                .append(cashierName)
                .append(timestamp)
                .append(accountNumber)
                .append(price)
                .append(currency)
                .append(chips)
                .append(transactionType)
                .append(gameType)
                .append(platform)
                .append(paymentOptionId)
                .append(promotionId)
                .append(baseCurrencyAmount)
                .append(baseCurrency)
                .append(exchangeRate)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("internalTransactionId", internalTransactionId)
                .append("externalTransactionId", externalTransactionId)
                .append("playerId", playerId)
                .append("accountId", accountId)
                .append("cashierName", cashierName)
                .append("timestamp", timestamp)
                .append("accountNumber", accountNumber)
                .append("price", price)
                .append("currency", currency)
                .append("chips", chips)
                .append("transactionType", transactionType)
                .append("gameType", gameType)
                .append("platform", platform)
                .append("paymentOptionId", paymentOptionId)
                .append("promotionId", promotionId)
                .append("baseCurrencyAmount", baseCurrencyAmount)
                .append("baseCurrency", baseCurrency)
                .append("exchangeRate", exchangeRate)
                .toString();
    }

    public static class PaymentSettlementBuilder {
        private String internalTransactionId;
        private String externalTransactionId;
        private BigDecimal playerId;
        private BigDecimal accountId;
        private String cashierName;
        private DateTime timestamp;
        private String accountNumber;
        private BigDecimal price;
        private Currency currency;
        private BigDecimal chips;
        private ExternalTransactionType transactionType;
        private String gameType;
        private Platform platform;
        private String paymentOptionId;
        private Long promotionId;
        private BigDecimal baseCurrencyAmount;
        private Currency baseCurrency;
        private BigDecimal exchangeRate;

        public PaymentSettlementBuilder(final String internalTransactionId,
                                        final String externalTransactionId,
                                        final BigDecimal playerId,
                                        final BigDecimal accountId,
                                        final String cashierName,
                                        final DateTime timestamp,
                                        final String accountNumber,
                                        final BigDecimal price,
                                        final Currency currency,
                                        final BigDecimal chips,
                                        final ExternalTransactionType transactionType) {
            this.internalTransactionId = internalTransactionId;
            this.externalTransactionId = externalTransactionId;
            this.playerId = playerId;
            this.accountId = accountId;
            this.cashierName = cashierName;
            this.timestamp = timestamp;
            this.accountNumber = accountNumber;
            this.price = price;
            this.currency = currency;
            this.chips = chips;
            this.transactionType = transactionType;
        }

        public PaymentSettlementBuilder withGameType(final String newGameType) {
            this.gameType = newGameType;
            return this;
        }

        public PaymentSettlementBuilder withPlatform(final Platform newPlatform) {
            this.platform = newPlatform;
            return this;
        }

        public PaymentSettlementBuilder withPaymentOptionId(final String newPaymentOptionId) {
            this.paymentOptionId = newPaymentOptionId;
            return this;
        }

        public PaymentSettlementBuilder withPromotionId(final Long promotionId) {
            this.promotionId = promotionId;
            return this;
        }

        public PaymentSettlementBuilder withBaseCurrencyAmount(final BigDecimal newBaseCurrencyAmount) {
            this.baseCurrencyAmount = newBaseCurrencyAmount;
            return this;
        }

        public PaymentSettlementBuilder withBaseCurrency(final Currency newBaseCurrency) {
            this.baseCurrency = newBaseCurrency;
            return this;
        }

        public PaymentSettlementBuilder withExchangeRate(final BigDecimal newExchangeRate) {
            this.exchangeRate = newExchangeRate;
            return this;
        }

        public PaymentSettlement build() {
            return new PaymentSettlement(internalTransactionId,
                    externalTransactionId,
                    playerId,
                    accountId,
                    cashierName,
                    timestamp,
                    accountNumber,
                    price,
                    currency,
                    chips,
                    transactionType,
                    gameType,
                    platform,
                    paymentOptionId,
                    promotionId,
                    baseCurrencyAmount,
                    baseCurrency,
                    exchangeRate);
        }
    }
}
