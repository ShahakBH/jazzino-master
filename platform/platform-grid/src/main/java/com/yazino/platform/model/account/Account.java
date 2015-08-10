package com.yazino.platform.model.account;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.yazino.game.api.ParameterisedMessage;
import com.yazino.platform.error.account.InsufficientFundsException;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;

import static org.apache.commons.lang3.Validate.notNull;

@SpaceClass
public class Account implements Serializable {
    private static final long serialVersionUID = 678125528735277397L;
    private static final Logger LOG = LoggerFactory.getLogger(Account.class);

    private BigDecimal accountId;
    private String name;
    private BigDecimal balance;
    private BigDecimal creditLimit;
    private Boolean open;
    private Collection<AccountTransaction> newAccountTransactions = new ArrayList<AccountTransaction>();

    public Account() {
    }

    public Account(final BigDecimal accountId,
                   final String name,
                   final BigDecimal balance) {
        this.accountId = accountId;
        this.name = name;
        this.balance = balance;
        this.open = Boolean.TRUE;
    }

    public Account(final BigDecimal accountId,
                   final String name,
                   final BigDecimal balance,
                   final BigDecimal creditLimit) {
        this.accountId = accountId;
        this.name = name;
        this.balance = balance;
        this.creditLimit = creditLimit;
        this.open = Boolean.TRUE;
    }

    public Account(final BigDecimal accountId) {
        this.accountId = accountId;
    }

    @SpaceId
    public BigDecimal getAccountId() {
        return accountId;
    }

    public void setAccountId(final BigDecimal accountId) {
        this.accountId = accountId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(final BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(final BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public Boolean isOpen() {
        return open;
    }

    public void setOpen(final Boolean open) {
        this.open = open;
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
        final Account rhs = (Account) obj;
        return BigDecimals.equalByComparison(accountId, rhs.accountId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(accountId))
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this).toString();
    }

    public BigDecimal availableBalance() {
        BigDecimal availableBalance = getBalance();
        if (availableBalance == null) {
            availableBalance = BigDecimal.ZERO;
        }

        if (getCreditLimit() != null) {
            availableBalance = availableBalance.add(getCreditLimit());
        }

        return availableBalance;
    }

    /**
     * Record a transaction against the account.
     *
     * @param accountTransaction the transaction to record. Non-null.
     * @throws InsufficientFundsException if the account is unable to service the transaction.
     */
    public void postTransaction(final AccountTransaction accountTransaction)
            throws InsufficientFundsException {
        notNull(accountTransaction, "accountTransaction is null");

        if (!accountTransaction.getAccountId().equals(accountId)) {
            throw new IllegalArgumentException("invalid account id " + accountTransaction.getAccountId());
        }

        final BigDecimal currentBalance;
        if (this.balance != null) {
            currentBalance = this.balance;
        } else {
            currentBalance = BigDecimal.ZERO;
        }
        final BigDecimal currentCreditLimit;
        if (this.creditLimit != null) {
            currentCreditLimit = this.creditLimit;
        } else {
            currentCreditLimit = BigDecimal.ZERO;
        }

        final BigDecimal availableBalance = currentBalance.add(currentCreditLimit);

        assert accountTransaction.getAmount() != null : "Null transaction amount submitted for transaction";
        if (availableBalance.add(accountTransaction.getAmount()).compareTo(BigDecimal.ZERO) < 0) {
            LOG.debug("Could not process transaction for account {} due to insufficient funds: "
                    + "balance: {}, credit limit: {}, available balance: {}, transaction amount: {}",
                    accountId, currentBalance, currentCreditLimit,
                    availableBalance, accountTransaction.getAmount());

            throw new InsufficientFundsException(new ParameterisedMessage(
                    "Cannot process transaction: available balance %s, requested withdrawal %s",
                    availableBalance, accountTransaction.getAmount()));
        }
        this.balance = this.balance.add(accountTransaction.getAmount());
        newAccountTransactions.add(new AccountTransaction(accountTransaction, new DateTime().getMillis(), this.balance));
    }

    public Collection<AccountTransaction> popNewAccountTransactions() {
        final Collection<AccountTransaction> pop = new ArrayList<AccountTransaction>(newAccountTransactions);
        newAccountTransactions.clear();
        return pop;
    }
}
