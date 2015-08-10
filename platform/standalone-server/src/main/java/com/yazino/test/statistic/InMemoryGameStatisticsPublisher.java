package com.yazino.test.statistic;

import com.yazino.game.api.statistic.GameStatistic;
import com.yazino.platform.service.statistic.GameStatisticsPublisher;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;

public class InMemoryGameStatisticsPublisher implements GameStatisticsPublisher {
    private Collection<GameStatistic> statistics;

    public Collection<GameStatistic> getStatistics() {
        return statistics;
    }

    public InMemoryGameStatisticsPublisher() {
        this.statistics = new HashSet<GameStatistic>();
    }

    @Override
    public void publish(final BigDecimal tableId,
                        final String gameType,
                        final String clientId,
                        final Collection<GameStatistic> statisticsToPublish) {
        this.statistics.addAll(statisticsToPublish);
    }
}
