package com.yazino.host.table;

import org.springframework.stereotype.Component;
import com.yazino.game.api.GameStatus;
import com.yazino.platform.processor.table.GameCompletePublisher;

import java.math.BigDecimal;

@Component
public class StandaloneGameCompletePublisher implements GameCompletePublisher {
    @Override
    public void publishCompletedGame(final GameStatus gameStatus,
                                     final String gameType,
                                     final BigDecimal tableId,
                                     final String clientId) {
    }
}
