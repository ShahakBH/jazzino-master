package com.yazino.platform.service.statistic;

import com.yazino.platform.playerstatistic.StatisticEvent;

import java.math.BigDecimal;
import java.util.Collection;

public interface StatisticEventService {

    void publishStatistics(BigDecimal playerId, String gameType, Collection<StatisticEvent> events);

}
