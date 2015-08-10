package com.yazino.platform.processor.statistic.level;

import com.yazino.platform.model.statistic.LevelingSystem;
import com.yazino.platform.model.statistic.PlayerLevel;

import java.math.BigDecimal;

public interface PlayerXPPublisher {
    void publish(BigDecimal playerId,
                 String gameType,
                 PlayerLevel playerLevel,
                 LevelingSystem levelingSystem);
}
