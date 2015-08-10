package com.yazino.platform.repository.statistic;

import com.yazino.platform.model.statistic.LevelingSystem;

public interface LevelingSystemRepository {
    LevelingSystem findByGameType(String gameType);

    void refreshDefinitions();
}
