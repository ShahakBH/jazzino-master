package com.yazino.platform.test.game.status;

import com.yazino.platform.test.game.GameRuleVariation;
import org.joda.time.DateTime;
import com.yazino.game.generic.GameContext;
import com.yazino.game.generic.status.GamePlayers;

import static org.apache.commons.lang3.Validate.notNull;

public class MockGameContext {
    private GameContext<GameRuleVariation, GamePlayerStatus> gameContext;
    private MockGameStatus mockGameStatus;

    public MockGameStatus getMockGameStatus() {
        return mockGameStatus;
    }

    public MockGameContext(final GameContext<GameRuleVariation, GamePlayerStatus> gameContext) {
        notNull(gameContext, "game context is null");
        notNull(gameContext.getGameStatus(), "game status is null");
        notNull(gameContext.getGamePlayers(), "game players is null");
        this.gameContext = gameContext;
        if (!(gameContext.getGameStatus() instanceof MockGameStatus)) {
            throw new IllegalStateException(String.format("Expected MockGameStatus but found %s", gameContext.getClass().getName()));
        }
        this.mockGameStatus = (MockGameStatus) gameContext.getGameStatus();
    }

    public DateTime getCurrentDateTime() {
        return gameContext.getCurrentDateTime();
    }

    public GameContext<GameRuleVariation, GamePlayerStatus> toGameContext() {
        return gameContext;
    }

    public MockGameContext withGamePlayers(final GamePlayers<GamePlayerStatus> gamePlayers) {
        gameContext = gameContext.withGamePlayers(gamePlayers);
        return this;
    }
}
