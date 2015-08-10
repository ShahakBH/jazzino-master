package com.yazino.game.api;

import java.math.BigDecimal;

public interface GamePlayerWallet {

    /**
     * Increase a player's wallet by a certain amount.
     *
     * @param amount     never null
     * @param auditLabel never null
     * @param reference, some table specific reference, may be null
     * @return a unique identifier for this change
     * @throws GameException should a problem occur whilst accessing the players account
     */
    String increaseBalanceBy(BigDecimal amount, String auditLabel, String reference) throws GameException;

    /**
     * Decrease a player's wallet by a certain amount.
     *
     * @param amount     never null
     * @param auditLabel never null
     * @param reference, some table specific reference, may be null
     * @return a unique identifier for this change
     * @throws GameException should a problem occur whilst accessing the players account
     */
    String decreaseBalanceBy(BigDecimal amount, String auditLabel, String reference) throws GameException;

    /**
     * Return the balance for this player.
     *
     * @return the balance, never null
     * @throws GameException should a problem occur whilst accessing the players account
     */
    BigDecimal getBalance() throws GameException;
}
