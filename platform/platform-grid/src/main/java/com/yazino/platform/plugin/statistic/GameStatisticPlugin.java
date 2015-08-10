package com.yazino.platform.plugin.statistic;

import com.yazino.game.api.statistic.GameStatisticConsumer;
import com.yazino.game.api.statistic.GameStatisticProducer;

/**
 * Manage the initial load, update or removal of a producer or consumer for a game
 */
public interface GameStatisticPlugin {

    void addGameStatisticsProducer(GameStatisticProducer gameStatisticProducer);

    void addGameStatisticsConsumer(GameStatisticConsumer gameStatisticConsumer);

    void removeGameStatisticsProducer(GameStatisticProducer gameStatisticProducer);

    void removeGameStatisticsConsumer(GameStatisticConsumer gameStatisticConsumer);
}
