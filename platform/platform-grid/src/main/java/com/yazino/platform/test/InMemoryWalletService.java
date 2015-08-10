package com.yazino.platform.test;


import com.yazino.game.api.TransactionResult;
import com.yazino.platform.account.ExternalTransaction;
import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.account.WalletServiceException;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.openspaces.remoting.Routing;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryWalletService implements TestWalletService, PaymentsQueue {
    private Map<BigDecimal, String> sessionMap = new HashMap<BigDecimal, String>();

    private Map<String, AccountImpl> userMap = new HashMap<String, AccountImpl>();
    private Map<BigDecimal, AccountImpl> accountsList = new HashMap<BigDecimal, AccountImpl>();
    private long nextTopupId = 0;

    private final AtomicLong idSource = new AtomicLong(System.currentTimeMillis());

    private final List<TransactionResult> pendingResults
            = Collections.synchronizedList(new LinkedList<TransactionResult>());
    private boolean rejectTransactions;
    private boolean groundTransactions;
    private boolean failOnInsufficientBalance = false;

    public InMemoryWalletService() {
    }

    public InMemoryWalletService(final boolean failOnInsufficientBalance) {
        this.failOnInsufficientBalance = failOnInsufficientBalance;
    }

    Map<BigDecimal, AccountImpl> getAccountsList() {
        return accountsList;
    }

    public List<TransactionResult> pendingResults() {
        final List<TransactionResult> result;
        synchronized (pendingResults) {
            result = new ArrayList<TransactionResult>(pendingResults);
            pendingResults.clear();
        }
        return result;
    }

    public void add(final TransactionResult result) {
        pendingResults.add(result);
    }

    public BigDecimal getTotalOfAllAccountBalances() {
        BigDecimal all = BigDecimal.ZERO;
        for (AccountImpl acc : accountsList.values()) {
            all = all.add(acc.getBalance());
        }
        return all;
    }

    public void createAccountIfRequired(final BigDecimal accountId) {
        createAccountIfRequired(accountId, "test " + accountId, BigDecimal.ZERO);
    }

    public void createAccountIfRequired(final BigDecimal accountId,
                                        final String name,
                                        final BigDecimal startingBalance) {
        if (accountsList.containsKey(accountId)) {
            return;
        }
        final AccountImpl acc = new AccountImpl(accountId, name);
        acc.setBalance(startingBalance);
        accountsList.put(accountId, acc);
    }

    private AccountImpl getAccountImpl(final BigDecimal accountId) {
        createAccountIfRequired(accountId);
        return accountsList.get(accountId);
    }

    public boolean authenticate(final BigDecimal accountId,
                                final String sessionKey) {
        return sessionKey.equals(sessionMap.get(accountId));
    }

    public BigDecimal createAccount(final BigDecimal parentAccountId,
                                    final String accountName,
                                    final BigDecimal initialBalance,
                                    final String reference) {
        final BigDecimal nextAccount = getOrCreateAccount(accountName, "" + parentAccountId);
        getAccountImpl(nextAccount).setBalance(BigDecimal.ZERO);
        // FIXME: - determine transaction type
        this.transfer(parentAccountId, nextAccount, initialBalance, "create account", null, reference);
        return nextAccount;
    }

    public void addPendingResult(final TransactionResult result) {
        if (!groundTransactions) {
            pendingResults.add(result);
        }
    }

    public void rejectTransactions(final boolean newRejectTransactions) {
        this.rejectTransactions = newRejectTransactions;
    }

    public void groundTransactions(final boolean newGroundTransactions) {
        this.groundTransactions = newGroundTransactions;
    }

    static class AccountImpl {
        private BigDecimal id;
        private BigDecimal parentId;
        private BigDecimal balance = BigDecimal.ZERO;
        private String name;

        public AccountImpl(final BigDecimal id,
                           final String name) {
            this.setId(id);
            this.setName(name);
        }

        public AccountImpl(final BigDecimal id, final BigDecimal parentId, final BigDecimal balance) {
            this.setId(id);
            this.setParentId(parentId);
            this.setBalance(balance);
        }

        public BigDecimal getId() {
            return id;
        }

        public void setId(final BigDecimal id) {
            this.id = id;
        }

        public BigDecimal getParentId() {
            return parentId;
        }

        public void setParentId(final BigDecimal parentId) {
            this.parentId = parentId;
        }

        public BigDecimal getBalance() {
            return balance;
        }

        public void setBalance(final BigDecimal balance) {
            this.balance = balance;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    public BigDecimal getOrCreateAccount(final String partnerId,
                                         final String userId) {
        return getOrCreateAccount(partnerId, userId, userId);
    }

    public BigDecimal getOrCreateAccount(final String partnerId,
                                         final String userId,
                                         final String userName) {
        AccountImpl existing = userMap.get(partnerId + ":" + userId);
        if (existing == null) {
            existing = getAccountImpl(BigDecimal.valueOf(idSource.getAndIncrement()));
            existing.setName(userName);
            userMap.put(partnerId + ":" + userId, existing);
        }
        return existing.getId();
    }

    public BigDecimal getBalance(final BigDecimal accountId) {
        return getAccountImpl(accountId).getBalance();
    }

    private void transfer(final BigDecimal fromAccountId,
                          final BigDecimal toAccountId,
                          final BigDecimal amount,
                          final String transactionType,
                          final String auditLabel,
                          final String reference) {
        final AccountImpl from = getAccountImpl(fromAccountId);
        final AccountImpl to = getAccountImpl(toAccountId);
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
    }

    @Override
    public BigDecimal postTransaction(final BigDecimal accountId,
                                      final BigDecimal amountOfChips,
                                      final String transactionType,
                                      final String reference,
                                      final TransactionContext transactionContext) throws WalletServiceException {
        if (rejectTransactions) {
            throw new WalletServiceException("rejected");
        }
        final AccountImpl acc = getAccountImpl(accountId);
        final BigDecimal newBalance = acc.getBalance().add(amountOfChips);
        if (this.failOnInsufficientBalance && newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new WalletServiceException(String.format(
                    "Account transaction failed for account %s: balance: %s, amount: %s",
                    accountId, acc.getBalance(), amountOfChips));
        }
        acc.setBalance(newBalance);
        return acc.getBalance();
    }

    public String getAccountName(final BigDecimal accountId) {
        return getAccountImpl(accountId).getName();
    }

    public BigDecimal createAccount(final String accountName) {
        final BigDecimal accountId = BigDecimal.valueOf(idSource.getAndIncrement());
        createAccountIfRequired(accountId);
        return accountId;
    }

    public void voidGame(final BigDecimal tableId,
                         final Long gameId,
                         final String username,
                         final String reason) {
    }

    public BigDecimal record(final ExternalTransaction externalTransaction) {
        return null;
    }

    @Override
    public BigDecimal getValueOfTodaysEarnedChips(@Routing final BigDecimal accountId, final String cashier) {
        throw new RuntimeException("not implemented");
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
        final InMemoryWalletService rhs = (InMemoryWalletService) obj;
        return new EqualsBuilder()
                .append(idSource, rhs.idSource)
                .append(nextTopupId, rhs.nextTopupId)
                .append(accountsList, rhs.accountsList)
                .append(sessionMap, rhs.sessionMap)
                .append(userMap, rhs.userMap)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(idSource)
                .append(nextTopupId)
                .append(accountsList)
                .append(sessionMap)
                .append(userMap)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(idSource)
                .append(nextTopupId)
                .append(accountsList)
                .append(sessionMap)
                .append(userMap)
                .toString();
    }
}
