package com.yazino.platform.repository.community;

import com.yazino.platform.community.Trophy;
import com.yazino.platform.persistence.community.TrophyDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class CachingTrophyRepository implements TrophyRepository {
    private static final Logger LOG = LoggerFactory.getLogger(CachingTrophyRepository.class);

    private final Map<BigDecimal, Trophy> trophies = new HashMap<BigDecimal, Trophy>();
    private final ReadWriteLock trophiesLock = new ReentrantReadWriteLock();

    private final TrophyDAO trophyDAO;

    @Autowired
    public CachingTrophyRepository(final TrophyDAO trophyDAO) {
        notNull(trophyDAO, "trophyDAO may not be null");

        this.trophyDAO = trophyDAO;
    }

    @PostConstruct
    public void refreshTrophies() {
        trophiesLock.writeLock().lock();

        try {
            trophies.clear();

            final Collection<Trophy> dbTrophies = trophyDAO.retrieveAll();
            if (dbTrophies != null) {
                for (Trophy dbTrophy : dbTrophies) {
                    trophies.put(dbTrophy.getId(), dbTrophy);
                }
            }
        } finally {
            trophiesLock.writeLock().unlock();
        }
    }

    @Override
    public Collection<Trophy> findForGameType(final String gameType) {
        notNull(gameType, "Game type  may not be null");

        if (LOG.isDebugEnabled()) {
            LOG.debug("Entering find for game type " + gameType);
        }

        final List<Trophy> trophiesForGameType = new ArrayList<Trophy>();

        trophiesLock.readLock().lock();
        try {
            for (Trophy trophy : trophies.values()) {
                if (trophy.getGameType().equals(gameType)) {
                    trophiesForGameType.add(trophy);
                }
            }
        } finally {
            trophiesLock.readLock().unlock();
        }

        return trophiesForGameType;
    }

    @Override
    public Trophy findById(final BigDecimal trophyId) {
        notNull(trophyId, "Trophy ID may not be null");

        if (LOG.isDebugEnabled()) {
            LOG.debug("Entering find by id " + trophyId);
        }

        trophiesLock.readLock().lock();
        try {
            return trophies.get(trophyId);

        } finally {
            trophiesLock.readLock().unlock();
        }
    }


    @Override
    public Trophy findByNameAndGameType(final String name,
                                        final String gameType) {
        notNull(name, "Trophy name may not be null");

        if (LOG.isDebugEnabled()) {
            LOG.debug("Entering find by name " + name);
        }

        trophiesLock.readLock().lock();
        try {
            for (Trophy trophy : trophies.values()) {
                if (trophy.getGameType().equals(gameType)
                        && trophy.getName().equals(name)) {
                    return trophy;
                }
            }
        } finally {
            trophiesLock.readLock().unlock();
        }

        return null;
    }

    @Override
    public List<Trophy> findAll() {
        trophiesLock.readLock().lock();
        try {
            return new ArrayList<Trophy>(trophies.values());
        } finally {
            trophiesLock.readLock().unlock();
        }
    }

    @Override
    public void save(final Trophy trophy) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Entering save for id %s: %s", trophy.getId(), trophy));
        }

        trophiesLock.writeLock().lock();
        try {
            trophyDAO.save(trophy);
            trophies.put(trophy.getId(), trophy);

        } finally {
            trophiesLock.writeLock().unlock();
        }
    }

    @Override
    public Collection<Trophy> findByName(final String trophyName) {
        notNull(trophyName, "trophyName must not be null");

        final List<Trophy> trophiesForName = new ArrayList<Trophy>();

        trophiesLock.readLock().lock();
        try {
            for (Trophy trophy : trophies.values()) {
                if (trophy.getName().equals(trophyName)) {
                    trophiesForName.add(trophy);
                }
            }
        } finally {
            trophiesLock.readLock().unlock();
        }

        return trophiesForName;
    }
}
