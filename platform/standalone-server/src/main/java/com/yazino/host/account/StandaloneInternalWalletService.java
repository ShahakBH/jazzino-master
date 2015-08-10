package com.yazino.host.account;

import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.service.account.InternalWalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component
public class StandaloneInternalWalletService implements InternalWalletService {
    private final StandaloneWalletService walletService;

    @Autowired
    public StandaloneInternalWalletService(final StandaloneWalletService walletService) {
        this.walletService = walletService;
    }

    @Override
    public Map<BigDecimal, BigDecimal> getBalances(final Collection<BigDecimal> accountIds)
            throws WalletServiceException {
        final Map<BigDecimal, BigDecimal> result = new HashMap<BigDecimal, BigDecimal>();
        for (BigDecimal accountId : accountIds) {
            result.put(accountId, walletService.getBalance(accountId));
        }
        return result;
    }

    @Override
    public BigDecimal getBalance(final BigDecimal accountId) throws WalletServiceException {
        return walletService.getBalance(accountId);
    }

    @Override
    public void closeAccount(final BigDecimal accountId) {
    }

    @Override
    public BigDecimal createAccount(final String accountName) throws WalletServiceException {
        return null;
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
