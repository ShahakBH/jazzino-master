package com.yazino.web.domain.social;

import com.yazino.web.data.BalanceSnapshotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Qualifier("playerInformationRetriever")
public class BalanceRetriever implements PlayerInformationRetriever {
    private final BalanceSnapshotRepository balanceSnapshotRepository;

    @Autowired
    public BalanceRetriever(final BalanceSnapshotRepository balanceSnapshotRepository) {
        this.balanceSnapshotRepository = balanceSnapshotRepository;
    }

    @Override
    public PlayerInformationType getType() {
        return PlayerInformationType.BALANCE;
    }

    @Override
    public BigDecimal retrieveInformation(final BigDecimal playerId,
                                          final String gameType) {
        BigDecimal balance = null;
        try {
            balance = balanceSnapshotRepository.getBalanceSnapshot(playerId);
        } catch (Exception e) {
            //ignore
        }
        return balance;
    }
}
