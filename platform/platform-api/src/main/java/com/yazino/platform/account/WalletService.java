package com.yazino.platform.account;


import org.openspaces.remoting.Routing;

import java.math.BigDecimal;

/**
 * This service handles operations on single accounts.
 */
public interface WalletService {

    /**
     * Find the balance for the given account.
     *
     * @param accountId the ID of the account. May not be null.
     * @return the balance of the account. Never null.
     * @throws WalletServiceException if the account ID is invalid.
     */
    BigDecimal getBalance(@Routing BigDecimal accountId) throws WalletServiceException;

    /**
     * Post a transaction to the account.
     *
     * @param accountId          the ID of the account. May not be null.
     * @param amountOfChips      the amount of chips to add to the account. May not be null.
     * @param transactionType    the type of transaction. May not be null.
     * @param reference          the transaction reference.
     * @param transactionContext contextual information for the transaction. May not be null.
     *                           Use <code>TransactionContext.EMPTY</code> if no context is applicable.
     * @return the final balance of the account.
     * @throws WalletServiceException   if the transaction cannot be recorded.
     * @throws IllegalArgumentException if the account does not exist.
     */
    BigDecimal postTransaction(@Routing BigDecimal accountId,
                               BigDecimal amountOfChips,
                               String transactionType,
                               String reference,
                               TransactionContext transactionContext)
            throws WalletServiceException;

    /**
     * Record an external transaction.
     * <p/>
     * If the transaction is successful this will result in the posting of the transaction to the account.
     *
     * @param externalTransaction the transaction to record and, if appropriate, post.
     * @return the new balance if a post was performed, otherwise null.
     */
    BigDecimal record(@Routing("getAccountId") ExternalTransaction externalTransaction)
            throws WalletServiceException;

    /**
     * adds value of all chip purchases for today for particular account and cashier
     *
     * @param accountId the account ID to filter on
     * @param cashier   cashier name
     * @return the total value of chip purchases for te current day, account and cashier.
     */
    BigDecimal getValueOfTodaysEarnedChips(@Routing BigDecimal accountId, String cashier);
}

