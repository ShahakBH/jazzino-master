package com.yazino.platform.plugin;

import com.yazino.platform.table.GameConfiguration;
import com.yazino.game.api.statistic.GameStatisticConsumer;
import com.yazino.game.api.statistic.GameStatisticProducer;

/**
 * Defines a way to load a statistics producer or consumer.
 * NB: Game configuration is populated in the Control Centre.
 */
public interface GameStatisticsLoader {

    GameStatisticProducer loadStatisticsProducer(GameConfiguration gameConfiguration);

    GameStatisticConsumer loadStatisticsConsumer(GameConfiguration gameConfiguration);
}
