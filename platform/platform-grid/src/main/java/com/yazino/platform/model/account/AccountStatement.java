package com.yazino.platform.model.account;

import com.yazino.platform.account.ExternalTransactionStatus;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;

import static org.apache.commons.lang3.Validate.notNull;

public final class AccountStatement implements Serializable {
    private static final long serialVersionUID = -4396606760047955149L;

    private final String internalTransactionId;
    private final BigDecimal accountId;
    private final String cashierName;
    private final Currency purchaseCurrency;
    private final BigDecimal purchaseAmount;
    private final BigDecimal chipsAmount;
    private final DateTime timestamp;
    private final String gameType;
    private final ExternalTransactionStatus transactionStatus;

    private AccountStatement(final String internalTransactionId,
                             final BigDecimal accountId,
                             final String cashierName,
                             final Currency purchaseCurrency,
                             final BigDecimal purchaseAmount,
                             final BigDecimal chipsAmount,
                             final DateTime timestamp,
                             final String gameType,
                             final ExternalTransactionStatus transactionStatus) {
        notNull(internalTransactionId, "internalTransactionId may not be null");
        notNull(accountId, "accountId may not be null");
        notNull(cashierName, "cashierName may not be null");
        notNull(purchaseCurrency, "purchaseCurrency may not be null");
        notNull(purchaseAmount, "purchaseAmount may not be null");

        this.internalTransactionId = internalTransactionId;
        this.accountId = accountId;
        this.cashierName = cashierName;
        this.purchaseCurrency = purchaseCurrency;
        this.purchaseAmount = purchaseAmount;
        this.gameType = gameType;
        this.transactionStatus = transactionStatus;

        if (timestamp != null) {
            this.timestamp = timestamp;
        } else {
            this.timestamp = new DateTime();
        }

        if (chipsAmount != null) {
            this.chipsAmount = chipsAmount;
        } else {
            this.chipsAmount = BigDecimal.ZERO;
        }
    }

    public static AccountStatementBuilder forAccount(final BigDecimal accountId) {
        return new AccountStatementBuilder(accountId);
    }

    public String getInternalTransactionId() {
        return internalTransactionId;
    }

    public BigDecimal getAccountId() {
        return accountId;
    }

    public String getCashierName() {
        return cashierName;
    }

    public Currency getPurchaseCurrency() {
        return purchaseCurrency;
    }

    public BigDecimal getPurchaseAmount() {
        return purchaseAmount;
    }

    public BigDecimal getChipsAmount() {
        return chipsAmount;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public String getGameType() {
        return gameType;
    }

    public ExternalTransactionStatus getTransactionStatus() {
        return transactionStatus;
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
        final AccountStatement rhs = (AccountStatement) obj;
        return new EqualsBuilder()
                .append(internalTransactionId, rhs.internalTransactionId)
                .append(cashierName, rhs.cashierName)
                .append(purchaseCurrency, rhs.purchaseCurrency)
                .append(purchaseAmount, rhs.purchaseAmount)
                .append(chipsAmount, rhs.chipsAmount)
                .append(gameType, rhs.gameType)
                .append(transactionStatus, rhs.transactionStatus)
                .append(timestamp, rhs.timestamp)
                .isEquals()
                && BigDecimals.equalByComparison(accountId, rhs.accountId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(internalTransactionId)
                .append(BigDecimals.strip(accountId))
                .append(cashierName)
                .append(purchaseCurrency)
                .append(purchaseAmount)
                .append(chipsAmount)
                .append(gameType)
                .append(transactionStatus)
                .append(timestamp)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(internalTransactionId)
                .append(accountId)
                .append(cashierName)
                .append(purchaseCurrency)
                .append(purchaseAmount)
                .append(chipsAmount)
                .append(gameType)
                .append(transactionStatus)
                .append(timestamp)
                .toString();
    }

    public static class AccountStatementBuilder {
        private String internalTransactionId = "123";
        private BigDecimal accountId;
        private String cashierName = "cashier";
        private Currency purchaseCurrency = Currency.getInstance("USD");
        private BigDecimal purchaseAmount = BigDecimal.ONE;
        private BigDecimal chipsAmount = BigDecimal.TEN;
        private DateTime timestamp;
        private String gameType;
        private ExternalTransactionStatus transactionStatus;

        public AccountStatementBuilder(final BigDecimal accountId) {
            this.accountId = accountId;
        }

        public AccountStatementBuilder withInternalTransactionId(final String theInternalTransactionId) {
            this.internalTransactionId = theInternalTransactionId;
            return this;
        }

        public AccountStatementBuilder withCashierName(final String theCashierName) {
            this.cashierName = theCashierName;
            return this;
        }

        public AccountStatementBuilder withPurchaseCurrency(final Currency thePurchaseCurrency) {
            this.purchaseCurrency = thePurchaseCurrency;
            return this;
        }

        public AccountStatementBuilder withPurchaseAmount(final BigDecimal thePurchaseAmount) {
            this.purchaseAmount = thePurchaseAmount;
            return this;
        }

        public AccountStatementBuilder withChipsAmount(final BigDecimal theChipsAmount) {
            this.chipsAmount = theChipsAmount;
            return this;
        }

        public AccountStatementBuilder withTimestamp(final DateTime theTimestamp) {
            this.timestamp = theTimestamp;
            return this;
        }

        public AccountStatementBuilder withGameType(final String theGameType) {
            this.gameType = theGameType;
            return this;
        }

        public AccountStatementBuilder withTransactionStatus(final ExternalTransactionStatus theStatus) {
            this.transactionStatus = theStatus;
            return this;
        }

        public AccountStatement asStatement() {
            return new AccountStatement(internalTransactionId,
                    accountId,
                    cashierName,
                    purchaseCurrency,
                    purchaseAmount,
                    chipsAmount,
                    timestamp,
                    gameType,
                    transactionStatus);
        }
    }
}
