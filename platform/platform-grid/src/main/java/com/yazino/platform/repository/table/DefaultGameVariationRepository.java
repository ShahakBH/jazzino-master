package com.yazino.platform.repository.table;

import com.yazino.platform.model.table.Table;
import com.yazino.platform.persistence.table.JDBCGameVariationDAO;
import com.yazino.platform.table.GameVariation;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class DefaultGameVariationRepository implements GameVariationRepository {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultGameVariationRepository.class);

    private final Map<BigDecimal, GameVariation> templatesById = new HashMap<BigDecimal, GameVariation>();
    private final Map<Pair<String, String>, GameVariation> templatesByNameAndGameType
            = new HashMap<Pair<String, String>, GameVariation>();

    private final ReadWriteLock templatesLock = new ReentrantReadWriteLock();

    private final JDBCGameVariationDAO gameVariationDAO;

    @Autowired
    public DefaultGameVariationRepository(final JDBCGameVariationDAO gameVariationDAO) {
        notNull(gameVariationDAO, "gameVariationDAO may not be null");

        this.gameVariationDAO = gameVariationDAO;
    }

    @Override
    public void refreshAll() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Refreshing all game templates in local repository");
        }

        templatesLock.writeLock().lock();

        try {
            templatesById.clear();
            templatesByNameAndGameType.clear();

            final Collection<GameVariation> gameVariations = gameVariationDAO.retrieveAll();

            for (final GameVariation gameVariation : gameVariations) {
                templatesById.put(gameVariation.getId(), gameVariation);
                templatesByNameAndGameType.put(Pair.of(gameVariation.getName(),
                        gameVariation.getGameType()), gameVariation);
            }

        } finally {
            templatesLock.writeLock().unlock();
        }
    }

    @Override
    public GameVariation findById(final BigDecimal id) {
        notNull(id, "id may not be null");

        templatesLock.readLock().lock();
        try {
            loadTemplatesIfRequired();

            return templatesById.get(id);

        } finally {
            templatesLock.readLock().unlock();
        }
    }

    @Override
    public BigDecimal getIdForName(final String name,
                                   final String gameType) {
        notNull(name, "name may not be null");
        notNull(gameType, "gameType may not be null");

        templatesLock.readLock().lock();

        try {
            loadTemplatesIfRequired();

            final GameVariation result = templatesByNameAndGameType.get(Pair.of(name, gameType));
            if (result != null) {
                return result.getId();
            }
            return null;

        } finally {
            templatesLock.readLock().unlock();
        }
    }

    @Override
    public Set<GameVariation> variationsFor(final String gameType) {
        notNull(gameType, "gameType may not be null");

        templatesLock.readLock().lock();

        try {
            loadTemplatesIfRequired();

            final Set<GameVariation> variationsForGameType = new HashSet<GameVariation>();
            for (GameVariation gameVariation : templatesById.values()) {
                if (gameVariation.getGameType().equals(gameType)) {
                    variationsForGameType.add(gameVariation);
                }
            }
            return variationsForGameType;

        } finally {
            templatesLock.readLock().unlock();
        }
    }

    @Override
    public void loadTemplatesIfRequired() {
        if (templatesById.isEmpty()) {
            templatesLock.readLock().unlock();
            refreshAll();
            templatesLock.readLock().lock();
        }
    }

    @Override
    public void populateProperties(final Table table) {
        notNull(table, "table may not be null");

        final GameVariation gameTemplate = findById(table.getTemplateId());
        if (gameTemplate == null) {
            throw new IllegalStateException("No game template exists for ID " + table.getTemplateId());
        }

        table.setTemplateName(gameTemplate.getName());
        table.setVariationProperties(gameTemplate.getProperties());
    }
}
