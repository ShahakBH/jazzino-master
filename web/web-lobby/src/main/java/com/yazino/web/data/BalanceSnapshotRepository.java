package com.yazino.web.data;

import com.googlecode.ehcache.annotations.Cacheable;
import com.yazino.platform.account.WalletService;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@Repository("balanceSnapshotRepository")
public class BalanceSnapshotRepository {
    private final WalletService walletService;
    private final PlayerService playerService;

    //cglib
    BalanceSnapshotRepository() {
        walletService = null;
        playerService = null;
    }

    @Autowired
    public BalanceSnapshotRepository(final WalletService walletService,
                                     final PlayerService playerService) {
        notNull(walletService, "walletService may not be null");
        notNull(playerService, "playerService may not be null");

        this.walletService = walletService;
        this.playerService = playerService;
    }

    @Cacheable(cacheName = "balanceSnapshotCache")
    public BigDecimal getBalanceSnapshot(final BigDecimal playerId) {
        try {
            return walletService.getBalance(playerService.getAccountId(playerId));
        } catch (final WalletServiceException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
