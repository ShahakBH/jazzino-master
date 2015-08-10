package com.yazino.platform.repository.table;

import com.yazino.platform.persistence.table.JDBCGameConfigurationDAO;
import com.yazino.platform.table.GameConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class DefaultGameConfigurationRepository implements GameConfigurationRepository {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultGameConfigurationRepository.class);
    private Map<String, GameConfiguration> gameConfigurationById;
    private final JDBCGameConfigurationDAO jdbcGameConfigurationDAO;
    private final ReadWriteLock repositoryLock;

    @Autowired
    public DefaultGameConfigurationRepository(final JDBCGameConfigurationDAO jdbcGameConfigurationDAO) {
        notNull(jdbcGameConfigurationDAO, "jdbcGameConfigurationDAO may not be null");
        this.jdbcGameConfigurationDAO = jdbcGameConfigurationDAO;
        this.gameConfigurationById = new HashMap<String, GameConfiguration>();
        this.repositoryLock = new ReentrantReadWriteLock();
    }

    @Override
    public void refreshAll() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Refreshing all game configurations in local repository");
        }
        repositoryLock.writeLock().lock();
        try {
            gameConfigurationById.clear();
            final Collection<GameConfiguration> gameConfigurations = jdbcGameConfigurationDAO.retrieveAll();
            for (final GameConfiguration gameConfiguration : gameConfigurations) {
                gameConfigurationById.put(gameConfiguration.getGameId(), gameConfiguration);
            }
        } finally {
            repositoryLock.writeLock().unlock();
        }
    }

    @Override
    public Collection<GameConfiguration> retrieveAll() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Retrieving all game configurations");
        }
        repositoryLock.readLock().lock();
        try {
            loadTemplatesIfRequired();
            return new HashSet<GameConfiguration>(gameConfigurationById.values());
        } finally {
            repositoryLock.readLock().unlock();
        }
    }

    @Override
    public GameConfiguration findById(final String gameId) {
        notNull(gameId, "gameId may not be null");
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Finding game configuration with ID: %s", gameId));
        }
        repositoryLock.readLock().lock();
        try {
            loadTemplatesIfRequired();
            return gameConfigurationById.get(gameId);
        } finally {
            repositoryLock.readLock().unlock();
        }
    }

    private void loadTemplatesIfRequired() {
        if (gameConfigurationById.isEmpty()) {
            repositoryLock.readLock().unlock();
            try {
                refreshAll();
            } finally {
                repositoryLock.readLock().lock();
            }
        }
    }
}
