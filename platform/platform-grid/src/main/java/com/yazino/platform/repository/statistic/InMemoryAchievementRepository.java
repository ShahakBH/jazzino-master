package com.yazino.platform.repository.statistic;

import com.yazino.platform.model.statistic.Achievement;
import com.yazino.platform.persistence.statistic.AchievementDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.apache.commons.lang3.Validate.notNull;

@Component("achievementRepository")
public class InMemoryAchievementRepository implements AchievementRepository {
    private final AchievementDAO achievementDAO;
    private Collection<Achievement> achievements = new HashSet<Achievement>();
    private Map<String, Collection<Achievement>> achievementsByGameType
            = new HashMap<String, Collection<Achievement>>();


    private final ReadWriteLock achievementsLock = new ReentrantReadWriteLock();

    @Autowired
    public InMemoryAchievementRepository(@Qualifier("achievementDAO") final AchievementDAO achievementDAO) {
        notNull(achievementDAO, "achievementDAO is null");
        this.achievementDAO = achievementDAO;
    }

    @Override
    public Collection<Achievement> findAll() {
        achievementsLock.readLock().lock();
        try {
            loadIfRequired();
            return achievements;
        } finally {
            achievementsLock.readLock().unlock();
        }
    }

    @Override
    public Collection<Achievement> findByGameType(final String gameType) {
        achievementsLock.readLock().lock();
        try {
            loadIfRequired();
            if (!achievementsByGameType.containsKey(gameType)) {
                return Collections.emptySet();
            }
            return achievementsByGameType.get(gameType);
        } finally {
            achievementsLock.readLock().unlock();
        }

    }

    @Override
    public void refreshDefinitions() {
        loadAll();
    }

    private void loadIfRequired() {
        if (achievements.size() == 0) {
            achievementsLock.readLock().unlock();
            loadAll();
            achievementsLock.readLock().lock();
        }
    }

    private void loadAll() {
        achievementsLock.writeLock().lock();
        try {
            achievements.clear();
            achievementsByGameType.clear();
            achievements.addAll(achievementDAO.findAll());
            for (Achievement achievement : achievements) {
                if (!achievementsByGameType.containsKey(achievement.getGameType())) {
                    achievementsByGameType.put(achievement.getGameType(), new HashSet<Achievement>());
                }
                achievementsByGameType.get(achievement.getGameType()).add(achievement);
            }
        } finally {
            achievementsLock.writeLock().unlock();
        }
    }
}
