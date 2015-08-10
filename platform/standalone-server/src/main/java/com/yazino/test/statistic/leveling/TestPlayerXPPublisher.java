package com.yazino.test.statistic.leveling;

import com.yazino.platform.model.statistic.PlayerLevel;
import com.yazino.platform.processor.statistic.level.PlayerXPPublisher;
import com.yazino.platform.model.statistic.LevelingSystem;

import java.math.BigDecimal;

public class TestPlayerXPPublisher implements PlayerXPPublisher {
    @Override
    public void publish(final BigDecimal playerId, final String gameType, final PlayerLevel playerLevel, final LevelingSystem levelingSystem) {
    }
}
