package com.yazino.platform.test.game.gamelogic;

import com.yazino.platform.test.game.GameRuleVariation;
import com.yazino.platform.test.game.events.MockGameEventsScheduler;
import com.yazino.platform.test.game.playerlogic.PlayerLogic;
import com.yazino.platform.test.game.status.GamePlayerStatus;
import com.yazino.platform.test.game.status.MockGameContext;
import com.yazino.game.api.GameException;
import com.yazino.game.generic.EventsScheduler;
import com.yazino.game.generic.GameContext;

public class MockGameLogicExecution {
    private enum Command {DO_STUFF}

    private GameChangeLogic gameChangeLogic;
    private PlayerLogic playersLogic;

    public GameChangeLogic getGameChangeLogic() {
        return gameChangeLogic;
    }

    public PlayerLogic getPlayersLogic() {
        return playersLogic;
    }

    public MockGameLogicExecution(final EventsScheduler eventsScheduler) {
        this.gameChangeLogic = new GameChangeLogic();
        this.playersLogic = new PlayerLogic(gameChangeLogic);
        MockGameEventsScheduler mockGameEventsScheduler = new MockGameEventsScheduler(eventsScheduler);
        // NB: any game logic may require the events scheduler
    }

    public GameContext<GameRuleVariation, GamePlayerStatus> execute(String commandAsString,
                                                                    MockGameContext mockGameContext,
                                                                    Object... commandArguments) throws GameException {
        try {
            Command command = Command.valueOf(commandAsString);
            switch (command) {
                default:
                    return mockGameContext.toGameContext();
            }
        } catch (IllegalArgumentException e) {
            throw new GameException(String.format("Command '%s' is not implemented", commandAsString));
        }
    }
}
