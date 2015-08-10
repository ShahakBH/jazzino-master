package com.yazino.platform.test;


import com.yazino.game.api.TransactionResult;
import com.yazino.game.api.TransactionType;
import com.yazino.platform.account.GameHostWallet;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.table.PlayerInformation;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static com.yazino.platform.account.TransactionContext.transactionContext;
import static org.apache.commons.lang3.Validate.notNull;

public class InMemoryGameHostWallet implements GameHostWallet {

    private final TestWalletService walletService;

    @Autowired
    public InMemoryGameHostWallet(final TestWalletService walletService) {
        notNull(walletService, "walletService may not be null");

        this.walletService = walletService;
    }

    @Override
    public void post(final BigDecimal tableId,
                     final Long gameId,
                     final PlayerInformation playerInformation,
                     final BigDecimal amount,
                     final TransactionType transactionType,
                     final String auditLabel,
                     final String reference,
                     final String replyLabel)
            throws WalletServiceException {

        TransactionResult result;
        try {
            final BigDecimal balance = walletService.postTransaction(playerInformation.getAccountId(),
                    amount, transactionType.toString(), reference,
                    transactionContext()
                            .withTableId(tableId)
                            .withGameId(gameId)
                            .withSessionId(playerInformation.getSessionId())
                            .withPlayerId(playerInformation.getPlayerId())
                            .build());
            result = new TransactionResult(replyLabel, true, null, playerInformation.getAccountId(),
                    balance, playerInformation.getPlayerId());

        } catch (Throwable e) {
            result = new TransactionResult(replyLabel, false, e.getMessage(), playerInformation.getAccountId(),
                    null, playerInformation.getPlayerId());
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
        }
        walletService.addPendingResult(result);
    }

    @Override
    public BigDecimal getBalance(final BigDecimal accountId)
            throws WalletServiceException {
        return walletService.getBalance(accountId);
    }
}
