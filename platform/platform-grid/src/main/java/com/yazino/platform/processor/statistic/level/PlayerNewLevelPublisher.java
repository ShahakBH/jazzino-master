package com.yazino.platform.processor.statistic.level;

import com.yazino.platform.model.statistic.PlayerLevels;

import java.math.BigDecimal;

public interface PlayerNewLevelPublisher {
    void publishNewLevel(PlayerLevels player,
                         String gameType,
                         BigDecimal chipBonus);
}
