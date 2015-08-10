package com.yazino.platform.service.account;

import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.account.TransactionContext;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

public interface InternalWalletService {

    /**
     * Retrieve balance for multiple accounts.
     *
     * @param accountIds the IDs of the account to query.
     * @return a map of account IDs to balances. Never null.
     * @throws com.yazino.platform.account.WalletServiceException
     *          on failure.
     */
    Map<BigDecimal, BigDecimal> getBalances(Collection<BigDecimal> accountIds)
            throws WalletServiceException;

    BigDecimal getBalance(BigDecimal accountId)
            throws WalletServiceException;

    BigDecimal createAccount(String accountName) throws WalletServiceException;

    void closeAccount(BigDecimal accountId);

    BigDecimal postTransaction(BigDecimal accountId,
                               BigDecimal amount,
                               String transactionType,
                               String transactionReference,
                               TransactionContext transactionContext)
            throws WalletServiceException;
}
