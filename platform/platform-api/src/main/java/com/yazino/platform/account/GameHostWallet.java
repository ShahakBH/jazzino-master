package com.yazino.platform.account;

import com.yazino.platform.table.PlayerInformation;
import com.yazino.game.api.TransactionType;

import java.math.BigDecimal;

/**
 * Encapsulates the required wallet functionality for the game host.
 * <p/>
 * In particular, this is intended to shield the real {@link WalletService}
 * from knowledge of tables or of the game, which are needed to match our transactions
 * up.
 */
public interface GameHostWallet {

    /**
     * Post a transaction.
     * <p/>
     * {@link com.yazino.game.api.TransactionResult}s may be later matched using the
     * <code>tableId</code>, <code>gameId</code> and <code>replyLabel</code>.
     *
     * @param tableId the ID of the table triggering the transaction.
     * @param gameId the ID of the game triggering the transaction.
     * @param playerInformation the player the transaction is against.
     * @param amount the amount.
     * @param transactionType the type of transaction.
     * @param auditLabel the audit label.
     * @param reference the transaction reference.
     * @param replyLabel the reply label for matching the asynchronous result.
     * @throws WalletServiceException if the transaction setup fails.
     */
    void post(BigDecimal tableId,
              Long gameId,
              PlayerInformation playerInformation,
              BigDecimal amount,
              TransactionType transactionType,
              String auditLabel,
              String reference,
              String replyLabel)
            throws WalletServiceException;

    /**
     * Get the balance of an account.
     *
     * @param accountId the account.
     * @return the balance.
     * @throws WalletServiceException if the balance cannot be determined.
     */
    BigDecimal getBalance(BigDecimal accountId) throws WalletServiceException;

}
