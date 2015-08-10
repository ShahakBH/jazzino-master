package com.yazino.platform.gamehost.wallet;

import com.yazino.platform.account.GameHostWallet;

public interface BufferedGameHostWallet extends GameHostWallet {

    /**
     * Flush any buffered requests.
     */
    void flush();

    int numberOfPendingTransactions();

}
