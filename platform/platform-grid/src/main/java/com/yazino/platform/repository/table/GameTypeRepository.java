package com.yazino.platform.repository.table;

import com.googlecode.ehcache.annotations.Cacheable;
import com.yazino.platform.table.GameTypeInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.yazino.game.api.GameMetaData;
import com.yazino.game.api.GameType;

import java.util.Collections;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class GameTypeRepository {

    private final GameRepository gameRepository;

    GameTypeRepository() {
        // CGLib constructor
        gameRepository = null;
    }

    @Autowired
    public GameTypeRepository(final GameRepository gameRepository) {
        notNull(gameRepository, "gameRepository may not be null");

        this.gameRepository = gameRepository;
    }

    @Cacheable(cacheName = "gameTypeCache")
    public GameType getGameType(final String gameTypeId) {
        notNull(gameTypeId, "gameTypeId may not be null");

        for (final GameTypeInformation info : gameTypeInformation()) {
            if (info.getId().equals(gameTypeId)) {
                return info.getGameType();
            }
        }

        return null;
    }

    private Set<GameTypeInformation> gameTypeInformation() {
        final Set<GameTypeInformation> availableGameTypes = gameRepository.getAvailableGameTypes();
        if (availableGameTypes != null) {
            return availableGameTypes;
        }
        return Collections.emptySet();
    }

    @Cacheable(cacheName = "gameMetaDataCache")
    public GameMetaData getMetaDataFor(final String gameTypeId) {
        notNull(gameTypeId, "gameTypeId may not be null");

        return gameRepository.getMetaDataFor(gameTypeId);
    }

}
