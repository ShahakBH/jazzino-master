package com.yazino.platform.test;

import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.service.account.InternalWalletService;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class InMemoryInternalWalletService implements InternalWalletService {

    private final InMemoryWalletService walletService;

    public InMemoryInternalWalletService(final InMemoryWalletService walletService) {
        this.walletService = walletService;
    }

    @Override
    public Map<BigDecimal, BigDecimal> getBalances(final Collection<BigDecimal> accountIds) {
        final Map<BigDecimal, BigDecimal> accountsToBalances = new HashMap<BigDecimal, BigDecimal>();

        if (accountIds != null) {
            for (BigDecimal accountId : accountIds) {
                final InMemoryWalletService.AccountImpl account = walletService.getAccountsList().get(accountId);
                if (account != null) {
                    accountsToBalances.put(accountId, account.getBalance());
                }
            }
        }

        return accountsToBalances;
    }

    @Override
    public BigDecimal getBalance(final BigDecimal accountId) throws WalletServiceException {
        return walletService.getAccountsList().get(accountId).getBalance();
    }

    @Override
    public void closeAccount(final BigDecimal accountId) {
    }

    @Override
    public BigDecimal createAccount(final String accountName) throws WalletServiceException {
        return walletService.createAccount(accountName);
    }

    @Override
    public BigDecimal postTransaction(final BigDecimal accountId,
                                      final BigDecimal amount,
                                      final String transactionType,
                                      final String transactionReference,
                                      final TransactionContext transactionContext) throws WalletServiceException {
        return walletService.postTransaction(accountId, amount, transactionType, transactionReference, transactionContext);
    }
}
