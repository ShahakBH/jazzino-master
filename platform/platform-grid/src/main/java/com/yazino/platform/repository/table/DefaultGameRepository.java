package com.yazino.platform.repository.table;

import com.yazino.game.api.GameFeature;
import com.yazino.game.api.GameMetaData;
import com.yazino.game.api.GameRules;
import com.yazino.game.api.GameType;
import com.yazino.platform.plugin.game.GameRulesService;
import com.yazino.platform.table.GameConfiguration;
import com.yazino.platform.table.GameTypeInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.apache.commons.lang3.Validate.notNull;

@Repository("gameRepository")
public class DefaultGameRepository implements GameRepository, GameRulesService {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultGameRepository.class);

    private Map<String, CachedGameRules> gameRules = new ConcurrentHashMap<String, CachedGameRules>();
    private final ReentrantReadWriteLock gameRulesLock = new ReentrantReadWriteLock();
    private final GameConfigurationRepository gameConfigurationRepository;

    @Autowired
    public DefaultGameRepository(final GameConfigurationRepository gameConfigurationRepository) throws Exception {
        notNull(gameConfigurationRepository, "gameConfigurationRepository cannot be null");
        this.gameConfigurationRepository = gameConfigurationRepository;
    }

    @Override
    public GameRules getGameRules(final String gameType) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("locating game rules for " + gameType);
        }
        final CachedGameRules cachedRules = lookupRules(gameType);
        if (cachedRules != null) {
            return cachedRules.getGameRules();
        }
        return null;
    }

    @Override
    public boolean isGameAvailable(final String gameType) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("isGameAvailable for " + gameType);
        }
        final boolean available = lookupRules(gameType).isAvailable();
        if (LOG.isDebugEnabled()) {
            LOG.debug("..." + available);
        }
        return available;
    }

    @Override
    public void setGameAvailable(final String gameTypeId, final boolean isAvailable) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("setGameAvailable " + gameTypeId + " " + isAvailable);
        }
        final CachedGameRules cachedGameRules = lookupRules(gameTypeId);
        if (cachedGameRules != null) {
            cachedGameRules.setAvailable(isAvailable);
        }
    }

    @Override
    public Set<GameTypeInformation> getAvailableGameTypes() {
        LOG.debug("getAvailableGameTypes");

        final Set<GameTypeInformation> types = new HashSet<GameTypeInformation>();
        for (GameConfiguration gameConfiguration : gameConfigurationRepository.retrieveAll()) {
            final GameType gameType = new GameType(gameConfiguration.getGameId(),
                    gameConfiguration.getDisplayName(),
                    new HashSet<String>(gameConfiguration.getAliases()),
                    featuresOf(gameConfiguration));

            final CachedGameRules cachedGameRules = lookupRules(gameConfiguration.getGameId());
            if (cachedGameRules != null) {
                types.add(new GameTypeInformation(gameType, cachedGameRules.isAvailable()));
            } else {
                LOG.warn("Couldn't find game rules for game type {}. Ignoring.", gameType);
            }
        }
        return types;
    }

    private Set<GameFeature> featuresOf(final GameConfiguration gameConfiguration) {
        final String supportsTournaments = gameConfiguration.getProperty("supportsTournaments");
        if (supportsTournaments != null && "true".equals(supportsTournaments)) {
            return Collections.singleton(GameFeature.TOURNAMENT);
        }
        return Collections.emptySet();
    }

    @Override
    public GameType getGameTypeFor(final String gameTypeId) {
        final GameConfiguration gameConfiguration = gameConfigurationRepository.findById(gameTypeId);
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Game type ID: %s - returned game configuration \n\t%s",
                    gameTypeId, gameConfiguration));
        }
        if (gameConfiguration == null) {
            throw new IllegalArgumentException(String.format(
                    "Game %s does not exist. Ensure that it has been created in Control Centre", gameTypeId));
        }
        return new GameType(gameConfiguration.getGameId(), gameConfiguration.getDisplayName(),
                new HashSet<String>(gameConfiguration.getAliases()), featuresOf(gameConfiguration));
    }

    @Override
    public GameMetaData getMetaDataFor(final String gameTypeId) {
        return lookupRules(gameTypeId).getGameRules().getMetaData();
    }

    private CachedGameRules lookupRules(final String gameTypeId) {
        if (gameRules.containsKey(gameTypeId)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Found rules for game with ID: %s", gameTypeId));
            }
            return gameRules.get(gameTypeId);
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Cannot locate rules for game with ID: %s", gameTypeId));
            }
        }
        return null;
    }

    @Override
    public void addGameRules(final GameRules rules) {
        gameRulesLock.writeLock().lock();
        try {
            final GameConfiguration gameConfiguration = gameConfigurationRepository.findById(rules.getGameType());
            if (gameConfiguration != null) {
                gameRules.put(gameConfiguration.getGameId(), new CachedGameRules(rules, gameConfiguration));
            } else {
                LOG.error(String.format("Could not update game rules. Missing game configuration for game with ID %s",
                        rules.getGameType()));
            }
        } finally {
            gameRulesLock.writeLock().unlock();
        }
    }

    @Override
    public void removeGameRules(final GameRules rules) {
        gameRulesLock.writeLock().lock();
        try {
            gameRules.remove(rules.getGameType());
            setGameAvailable(rules.getGameType(), false);
        } finally {
            gameRulesLock.writeLock().unlock();
        }
    }

    private final class CachedGameRules {
        private final GameRules gameRules;
        private GameConfiguration gameConfiguration;
        private boolean available = true;

        private CachedGameRules(final GameRules gameRules, final GameConfiguration gameConfiguration) {
            this.gameRules = gameRules;
            this.gameConfiguration = gameConfiguration;
        }

        public GameRules getGameRules() {
            return gameRules;
        }

        public GameConfiguration getGameConfiguration() {
            return gameConfiguration;
        }

        public boolean isAvailable() {
            return available;
        }

        public void setAvailable(final boolean available) {
            this.available = available;
        }
    }

}
