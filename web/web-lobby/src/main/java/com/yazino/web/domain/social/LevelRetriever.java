package com.yazino.web.domain.social;

import com.yazino.web.data.LevelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Qualifier("playerInformationRetriever")
public class LevelRetriever implements PlayerInformationRetriever {
    private final LevelRepository repository;

    @Autowired
    public LevelRetriever(final LevelRepository repository) {
        this.repository = repository;
    }

    @Override
    public PlayerInformationType getType() {
        return PlayerInformationType.LEVEL;
    }

    @Override
    public Object retrieveInformation(final BigDecimal playerId, final String gameType) {
        if (gameType == null) {
            return null;
        }
        return repository.getLevel(playerId, gameType);
    }
}
