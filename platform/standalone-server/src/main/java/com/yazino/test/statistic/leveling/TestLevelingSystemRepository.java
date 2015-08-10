package com.yazino.test.statistic.leveling;


import com.yazino.platform.model.statistic.LevelingSystem;
import com.yazino.platform.repository.statistic.LevelingSystemRepository;

public class TestLevelingSystemRepository implements LevelingSystemRepository {
    private LevelingSystem levelingSystem;

    public void setLevelingSystem(final LevelingSystem levelingSystem) {
        this.levelingSystem = levelingSystem;
    }

    @Override
    public LevelingSystem findByGameType(final String gameType) {
        return levelingSystem;
    }

    @Override
    public void refreshDefinitions() {
    }
}
