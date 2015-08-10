package com.yazino.platform.processor.statistic;

import com.yazino.platform.playerstatistic.StatisticEvent;

import java.math.BigDecimal;
import java.util.Collection;

public interface PlayerStatisticEventConsumer {
    void processEvents(BigDecimal playerId, String gameType, Collection<StatisticEvent> events);
}
