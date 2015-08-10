package com.yazino.platform.processor.table;

import com.yazino.game.api.GameStatus;

import java.math.BigDecimal;

public interface GameCompletePublisher {
    void publishCompletedGame(GameStatus gameStatus, String gameType, BigDecimal tableId, String clientId);
}
