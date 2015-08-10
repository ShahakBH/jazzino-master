package com.yazino.host.table.game;

import com.yazino.model.StandaloneServerConfiguration;
import com.yazino.platform.plugin.game.GameRulesService;
import com.yazino.platform.table.GameTypeInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.yazino.game.api.GameMetaData;
import com.yazino.game.api.GameRules;
import com.yazino.game.api.GameType;
import com.yazino.platform.repository.table.GameRepository;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class StandaloneGameRepository implements GameRepository, GameRulesService {
    private static final Logger LOG = LoggerFactory.getLogger(StandaloneGameRepository.class);

    private final StandaloneServerConfiguration config;

    private final Map<String, GameRules> gameRules = new HashMap<String, GameRules>();
    private final ReentrantReadWriteLock gameRulesLock = new ReentrantReadWriteLock();

    @Autowired
    public StandaloneGameRepository(final StandaloneServerConfiguration config) {
        this.config = config;
    }

    @Override
    public Set<GameTypeInformation> getAvailableGameTypes() {
        return Collections.singleton(new GameTypeInformation(
                new GameType("default", "default", Collections.<String>emptySet()), true));
    }

    @Override
    public GameRules getGameRules(final String gameType) {
        gameRulesLock.readLock().lock();
        try {
            GameRules result = gameRules.get(gameType);
            if (result == null) {
                LOG.warn("Game rules for " + gameType + " not available as plugin! Trying to load from classpath");
                result = loadFromClasspath(gameType);
                if (result == null) {
                    throw new RuntimeException("Could not find game rules for " + gameType);
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            gameRulesLock.readLock().unlock();
        }
    }

    private GameRules loadFromClasspath(final String gameType) throws Exception {
        if (gameRules.containsKey(gameType)) {
            return gameRules.get(gameType);
        }
        gameRulesLock.readLock().unlock();
        gameRulesLock.writeLock().lock();
        try {
            final GameRules rules = (GameRules) Class.forName(config.getGameRulesClassName()).newInstance();
            gameRules.put(gameType, rules);
            return rules;
        } finally {
            gameRulesLock.writeLock().unlock();
            gameRulesLock.readLock().lock();
        }
    }

    @Override
    public boolean isGameAvailable(final String gameType) {
        return true;
    }

    @Override
    public void setGameAvailable(final String gameType, final boolean isAvailable) {
    }

    @Override
    public GameType getGameTypeFor(final String gameTypeId) {
        return new GameType(gameRules.get(gameTypeId).getGameType(), gameTypeId, Collections.<String>emptySet());
    }

    @Override
    public GameMetaData getMetaDataFor(final String gameTypeId) {
        return gameRules.get(gameTypeId).getMetaData();
    }

    @Override
    public void addGameRules(final GameRules rules) {
        gameRulesLock.writeLock().lock();
        try {
            gameRules.put(rules.getGameType(), rules);
        } finally {
            gameRulesLock.writeLock().unlock();
        }
    }

    @Override
    public void removeGameRules(final GameRules rules) {
        gameRulesLock.writeLock().lock();
        try {
            gameRules.remove(rules.getGameType());
        } finally {
            gameRulesLock.writeLock().unlock();
        }
    }
}
