package com.yazino.test.game;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import com.yazino.game.api.GamePlayer;
import com.yazino.platform.test.TestWalletService;
import com.yazino.platform.gamehost.GamePlayerService;

import java.math.BigDecimal;

public class InMemoryWalletPlayerService implements GamePlayerService {
    private TestWalletService walletService;

    @Autowired
    public InMemoryWalletPlayerService(@Qualifier("walletService") final TestWalletService walletService) {
        this.walletService = walletService;
    }

    public GamePlayer getPlayer(final BigDecimal playerId) {
        final String name = walletService.getAccountName(playerId);
        return new GamePlayer(playerId, null, name);
    }
}
