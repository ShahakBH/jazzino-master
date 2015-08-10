package com.yazino.platform.test;

import com.yazino.platform.account.WalletService;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.game.api.TransactionResult;

import java.math.BigDecimal;

public interface TestWalletService extends WalletService {
    String getAccountName(BigDecimal accountId);

    void voidGame(BigDecimal tableId, Long gameId, String username, String reason) throws WalletServiceException;

    void addPendingResult(final TransactionResult result);
}
