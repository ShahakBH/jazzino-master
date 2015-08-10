package com.yazino.host.account;

import com.yazino.host.community.StandalonePlayerSource;
import com.yazino.model.StandalonePlayer;
import com.yazino.platform.account.ExternalTransaction;
import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.account.WalletService;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.error.account.InsufficientFundsException;
import com.yazino.platform.model.account.Account;
import com.yazino.platform.model.account.AccountTransaction;
import org.openspaces.remoting.Routing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
public class StandaloneWalletService implements WalletService {
    private final StandalonePlayerSource playerSource;
    private final Map<BigDecimal, Account> playerAccounts = new HashMap<BigDecimal, Account>();

    @Autowired
    public StandaloneWalletService(final StandalonePlayerSource playerSource) {
        this.playerSource = playerSource;
    }

    @Override
    public BigDecimal getBalance(@Routing final BigDecimal accountId) throws WalletServiceException {
        return getOrCreateAccount(accountId).getBalance();
    }

    private Account getOrCreateAccount(final BigDecimal accountId) {
        Account account = playerAccounts.get(accountId);
        if (account == null) {
            final StandalonePlayer player = playerSource.findById(accountId);
            account = new Account(player.getPlayerId(), player.getName(), player.getChips());
            playerAccounts.put(accountId, account);
        }
        return account;
    }

    @Override
    public BigDecimal postTransaction(@Routing final BigDecimal accountId,
                                      final BigDecimal amount,
                                      final String transactionType,
                                      final String reference,
                                      final TransactionContext transactionContext)
            throws WalletServiceException {


        try {
            final Account account = getOrCreateAccount(accountId);
            account.postTransaction(new AccountTransaction(accountId, amount, transactionType, reference, transactionContext));
            playerSource.findById(accountId).setChips(account.getBalance());
            return account.getBalance();
        } catch (InsufficientFundsException e) {
            throw new WalletServiceException(e.getParameterisedMessage());
        }
    }

    @Override
    public BigDecimal record(@Routing("getAccountId") final ExternalTransaction externalTransaction) {
        return null;
    }

    @Override
    public BigDecimal getValueOfTodaysEarnedChips(@Routing final BigDecimal accountId, final String cashier) {
        throw new RuntimeException("not implemented");
    }

}
