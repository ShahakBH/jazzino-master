package com.yazino.platform.service.statistic;

import com.yazino.game.api.statistic.GameStatistic;

import java.math.BigDecimal;
import java.util.Collection;

public interface GameStatisticsPublisher {
    void publish(BigDecimal tableId, String gameType, String clientId, Collection<GameStatistic> statistics);
}
