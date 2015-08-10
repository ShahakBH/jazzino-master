package com.yazino.platform.repository.statistic;

import com.yazino.platform.model.statistic.LevelingSystem;
import com.yazino.platform.persistence.statistic.LevelingSystemDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.apache.commons.lang3.Validate.notNull;

@Component("levelingSystemRepository")
public class InMemoryLevelingSystemRepository implements LevelingSystemRepository {

    private final LevelingSystemDAO levelingSystemDAO;
    private Map<String, LevelingSystem> levelingSystems = new HashMap<String, LevelingSystem>();

    private final ReadWriteLock levelingSystemsLock = new ReentrantReadWriteLock();

    @Autowired
    public InMemoryLevelingSystemRepository(@Qualifier("levelingSystemDAO") final LevelingSystemDAO levelingSystemDAO) {
        notNull(levelingSystemDAO, "levelingSystemDAO is null");
        this.levelingSystemDAO = levelingSystemDAO;
    }

    @Override
    public LevelingSystem findByGameType(final String gameType) {
        levelingSystemsLock.readLock().lock();
        try {
            checkLevelingSystems();
            return levelingSystems.get(gameType);
        } finally {
            levelingSystemsLock.readLock().unlock();
        }
    }

    @Override
    public void refreshDefinitions() {
        loadAll();
    }

    private void checkLevelingSystems() {
        if (levelingSystems.size() == 0) {
            levelingSystemsLock.readLock().unlock();
            loadAll();
            levelingSystemsLock.readLock().lock();
        }
    }

    private void loadAll() {
        levelingSystemsLock.writeLock().lock();
        try {
            final Collection<LevelingSystem> all = levelingSystemDAO.findAll();
            for (LevelingSystem levelingSystem : all) {
                levelingSystems.put(levelingSystem.getGameType(), levelingSystem);
            }
        } finally {
            levelingSystemsLock.writeLock().unlock();
        }
    }
}
