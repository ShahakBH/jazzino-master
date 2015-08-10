package com.yazino.platform.gamehost.wallet;

import java.math.BigDecimal;

/**
 * A factory for {@link BufferedGameHostWallet}.
 * <p/>
 * This class serves two purposes:
 * <ul>
 * <li>Abstract the buffering from the GameHost</li>
 * <li>Remove the need for the GameHost to know about the space</li>
 * </ul>
 */
public interface BufferedGameHostWalletFactory {

    /**
     * Create a buffered factory where results are not of interest.
     *
     * @param tableId the table ID.
     * @return the wallet.
     */
    BufferedGameHostWallet create(BigDecimal tableId);

    /**
     * Create a buffered factory where results are of interest.
     *
     * @param tableId    the table ID.
     * @param replyLabel the reply label, for matching results.
     * @return the wallet.
     */
    BufferedGameHostWallet create(BigDecimal tableId, String replyLabel);

}
