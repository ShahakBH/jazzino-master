package com.yazino.platform.processor.tournament;

import com.yazino.game.api.statistic.GameStatistic;

import java.math.BigDecimal;
import java.util.Collection;

public interface TournamentPlayerStatisticPublisher {

    /**
     * Publish statistics for a particular player
     *
     * @param playerId   player's id
     * @param gameType   the game type
     * @param statistics the statistic
     */
    void publishStatistics(BigDecimal playerId, String gameType, Collection<GameStatistic> statistics);
}
