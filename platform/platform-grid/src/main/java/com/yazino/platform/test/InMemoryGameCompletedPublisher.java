package com.yazino.platform.test;

import com.yazino.platform.processor.table.GameCompletePublisher;
import com.yazino.game.api.GameStatus;

import java.math.BigDecimal;

public class InMemoryGameCompletedPublisher implements GameCompletePublisher {
    public void publishCompletedGame(final GameStatus gameStatus,
                                     final String gameType,
                                     final BigDecimal tableId,
                                     final String clientId) {
    }
}
