package com.yazino.host.table.statistic;

import org.springframework.stereotype.Component;
import com.yazino.game.api.statistic.GameStatistic;
import com.yazino.platform.service.statistic.GameStatisticsPublisher;

import java.math.BigDecimal;
import java.util.Collection;

@Component("gameStatisticsPublisher")
public class StandaloneGameStatisticPublisher implements GameStatisticsPublisher {
    @Override
    public void publish(final BigDecimal tableId,
                        final String gameType,
                        final String clientId,
                        final Collection<GameStatistic> statistics) {
    }
}
