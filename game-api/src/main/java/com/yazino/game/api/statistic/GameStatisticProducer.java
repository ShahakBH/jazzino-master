package com.yazino.game.api.statistic;

import com.yazino.game.api.GameStatus;

import java.util.Collection;

public interface GameStatisticProducer {
    String getGameType();

    Collection<GameStatistic> processCompletedGame(GameStatus gameStatus);

}
