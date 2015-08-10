package com.yazino.test.statistic;

import com.yazino.platform.model.statistic.Achievement;
import com.yazino.platform.repository.statistic.AchievementRepository;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class TestAchievementRepository implements AchievementRepository {
    private Set<Achievement> achievements = new HashSet<Achievement>();

    @Override
    public Collection<Achievement> findAll() {
        return achievements;
    }

    @Override
    public Collection<Achievement> findByGameType(final String gameType) {
        return findAll();
    }

    @Override
    public void refreshDefinitions() {
    }

    public void addAchievement(final String gameType,
                               final SimplifiedAchievementDefinition definition) {
        final String threshold;
        if (definition.getThreshold() == 0) {
            threshold = null;
        } else {
            threshold = String.valueOf(definition.getThreshold());
        }
        achievements.add(new Achievement(definition.getAchievementId(),
                0,
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                gameType,
                new HashSet<String>(Arrays.asList(definition.getEvents())),
                definition.getType().getAccumulator(),
                threshold,
                true));
    }
}
