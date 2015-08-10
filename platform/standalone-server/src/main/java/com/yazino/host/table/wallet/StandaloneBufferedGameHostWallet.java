package com.yazino.host.table.wallet;

import com.yazino.game.api.TransactionResult;
import com.yazino.game.api.TransactionType;
import com.yazino.host.TableRequestWrapperQueue;
import com.yazino.platform.account.WalletService;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.gamehost.wallet.BufferedGameHostWallet;
import com.yazino.platform.model.table.PostTransactionAtTable;
import com.yazino.platform.model.table.TableRequestWrapper;
import com.yazino.platform.model.table.TransactionResultWrapper;
import com.yazino.platform.table.PlayerInformation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.yazino.platform.account.TransactionContext.transactionContext;

public class StandaloneBufferedGameHostWallet implements BufferedGameHostWallet {
    private final List<PostTransactionAtTable> pendingTransactions = new ArrayList<>();

    private final WalletService walletService;
    private final TableRequestWrapperQueue tableRequestQueue;

    public StandaloneBufferedGameHostWallet(final WalletService walletService,
                                            final TableRequestWrapperQueue tableRequestQueue) {
        this.walletService = walletService;
        this.tableRequestQueue = tableRequestQueue;
    }

    @Override
    public void flush() {
        executeTransactions(pendingTransactions);
        pendingTransactions.clear();
    }

    @Override
    public int numberOfPendingTransactions() {
        return pendingTransactions.size();
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
        pendingTransactions.add(new PostTransactionAtTable(playerInformation.getPlayerId(),
                playerInformation.getAccountId(),
                amount,
                transactionType.name(),
                reference,
                replyLabel,
                transactionContext()
                        .withTableId(tableId)
                        .withGameId(gameId)
                        .withSessionId(playerInformation.getSessionId())
                        .withPlayerId(playerInformation.getPlayerId())
                        .build()));
    }

    @Override
    public BigDecimal getBalance(final BigDecimal accountId)
            throws WalletServiceException {
        return walletService.getBalance(accountId);
    }

    private void executeTransactions(final List<PostTransactionAtTable> transactionsAtTable) {
        for (final PostTransactionAtTable tx : transactionsAtTable) {
            try {
                final BigDecimal balance = walletService.postTransaction(
                        tx.getAccountId(),
                        tx.getAmount(),
                        tx.getTransactionType(),
                        tx.getReference(),
                        tx.getTransactionContext());
                addTableRequest(tx, balance, true, null);
            } catch (WalletServiceException e) {
                addTableRequest(tx, null, false, e.getMessage());
            }
        }
    }

    private void addTableRequest(final PostTransactionAtTable tx,
                                 final BigDecimal balance,
                                 final boolean successful,
                                 final String errorReason) {
        final TransactionResult result = new TransactionResult(tx.getUniqueId(),
                successful,
                errorReason,
                tx.getAccountId(),
                balance,
                tx.getPlayerId());
        final TransactionResultWrapper wrapper = new TransactionResultWrapper(tx.getTransactionContext().getTableId(),
                tx.getTransactionContext().getGameId(),
                result,
                tx.getUniqueId());
        tableRequestQueue.addRequest(new TableRequestWrapper(wrapper));
    }
}
