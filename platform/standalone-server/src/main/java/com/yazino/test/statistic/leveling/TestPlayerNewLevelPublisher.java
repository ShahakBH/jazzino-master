package com.yazino.test.statistic.leveling;

import com.yazino.platform.model.statistic.PlayerLevels;
import com.yazino.platform.processor.statistic.level.PlayerNewLevelPublisher;

import java.math.BigDecimal;

public class TestPlayerNewLevelPublisher implements PlayerNewLevelPublisher {
    @Override
    public void publishNewLevel(final PlayerLevels player, final String gameType, final BigDecimal chipBonus) {
    }
}
