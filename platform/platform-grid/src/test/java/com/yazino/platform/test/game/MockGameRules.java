package com.yazino.platform.test.game;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.yazino.game.api.*;
import com.yazino.game.generic.EventsScheduler;
import com.yazino.game.generic.GameContext;
import com.yazino.game.generic.GenericGameRules;
import com.yazino.game.generic.events.PendingGameEvent;
import com.yazino.game.generic.status.GameChanges;
import com.yazino.game.generic.status.GamePlayers;
import com.yazino.game.generic.status.PendingGameEvents;
import com.yazino.platform.test.game.gamelogic.MockGameLogicExecution;
import com.yazino.platform.test.game.playerlogic.PlayerLogic;
import com.yazino.platform.test.game.status.GamePlayerStatus;
import com.yazino.platform.test.game.status.MockGameContext;
import com.yazino.platform.test.game.status.MockGameStatus;

import java.math.BigDecimal;
import java.util.*;

public class MockGameRules extends GenericGameRules<GameRuleVariation, GamePlayerStatus> {
    public static final Function<com.yazino.game.api.GamePlayer, com.yazino.game.api.PlayerAtTableInformation> GAME_PLAYER_TRANSFORM
            = new Function<com.yazino.game.api.GamePlayer, com.yazino.game.api.PlayerAtTableInformation>() {
        @Override
        public com.yazino.game.api.PlayerAtTableInformation apply(final com.yazino.game.api.GamePlayer gamePlayer) {
            return new com.yazino.game.api.PlayerAtTableInformation(gamePlayer, new HashMap<String, String>());
        }
    };
    public static final String GAME_NAME = "MOCK_GAME";
    private static final com.yazino.game.api.GameMetaData META_DATA = new com.yazino.game.api.GameMetaDataBuilder().build();
    private MockGameLogicExecution mockGameLogicExecution;

    public MockGameLogicExecution getMockGameLogicExecution() {
        return mockGameLogicExecution;
    }

    public MockGameRules() {
        super(GAME_NAME, META_DATA);
        mockGameLogicExecution = new MockGameLogicExecution(getEventHandler());
    }

    @Override
    public com.yazino.game.api.GameStatus createNewGameContext(final com.yazino.game.api.GameCreationContext creationContext,
                                                               final com.yazino.game.api.Randomizer randomizer) {
        final GameRuleVariation gameRuleVariation = GameRuleVariation.withProperties(creationContext.getProperties());
        return new GameStatus(new MockGameStatus(gameRuleVariation,
                new GamePlayers<GamePlayerStatus>(),
                new PendingGameEvents(Collections.unmodifiableList(new ArrayList<PendingGameEvent>()),
                        Collections.unmodifiableList(new ArrayList<PendingGameEvent>())),
                GameChanges.emptyChanges()
        ));
    }

    @Override
    public GameContext<GameRuleVariation, GamePlayerStatus> createNextGameContext(final com.yazino.game.api.ExecutionContext executionContext,
                                                                                  final com.yazino.game.api.Randomizer randomizer,
                                                                                  final EventsScheduler eventHandler) {
        return new MockGameContext(GameContext.<GameRuleVariation, GamePlayerStatus>createFromExecutionContext(
                executionContext, randomizer)).toGameContext();
    }

    @Override
    public GameContext<GameRuleVariation, GamePlayerStatus> executeCommand(final com.yazino.game.api.Command command,
                                                                           final GameContext<GameRuleVariation, GamePlayerStatus> gameContext)
            throws com.yazino.game.api.GameException {
        final List<Object> arguments = new ArrayList<Object>();
        arguments.add(command.getPlayer());
        return new MockGameContext(mockGameLogicExecution.execute(command.getType(), new MockGameContext(gameContext),
                arguments.toArray())).toGameContext();
    }

    @Override
    public GameContext<GameRuleVariation, GamePlayerStatus> executeGameEvent(final GameContext<GameRuleVariation, GamePlayerStatus> gameContext,
                                                                             final PendingGameEvent gameEvent) throws com.yazino.game.api.GameException {
        return mockGameLogicExecution.execute(gameEvent.getLogicToExecute(), new MockGameContext(gameContext));
    }

    @Override
    public GameContext<GameRuleVariation, GamePlayerStatus> executePlayerEvent(final GameContext<GameRuleVariation, GamePlayerStatus> gameContext,
                                                                               final PendingGameEvent gameEvent,
                                                                               final BigDecimal playerID) throws com.yazino.game.api.GameException {
        return mockGameLogicExecution.execute(gameEvent.getLogicToExecute(), new MockGameContext(gameContext), playerID);
    }

    @Override
    public GameContext<GameRuleVariation, GamePlayerStatus> successfulTransaction(final GameContext<GameRuleVariation, GamePlayerStatus> gameContext,
                                                                                  final String reference,
                                                                                  final BigDecimal balance) throws com.yazino.game.api.GameException {
        final MockGameContext mockGameContext = new MockGameContext(gameContext);
        final PlayerLogic playerLogic = mockGameLogicExecution.getPlayersLogic();
        final GamePlayerStatus playerStatus = playerLogic.getPlayerWithTransactionReference(mockGameContext, reference);
        if (playerStatus == null) {
            return mockGameContext.toGameContext();
        }
        return mockGameContext.toGameContext().withGamePlayers(playerLogic.removeReference(mockGameContext, reference));
    }

    @Override
    public GameContext<GameRuleVariation, GamePlayerStatus> failedTransaction(final GameContext<GameRuleVariation, GamePlayerStatus> gameContext,
                                                                              final String reference,
                                                                              final BigDecimal balance,
                                                                              final String errorReason) throws com.yazino.game.api.GameException {
        MockGameContext mockGameContext = new MockGameContext(gameContext);
        final GamePlayerStatus playerStatus = mockGameLogicExecution.getPlayersLogic().getPlayerWithTransactionReference(mockGameContext, reference);
        if (playerStatus == null) {
            return mockGameContext.toGameContext();
        }
        return mockGameContext.withGamePlayers(mockGameLogicExecution.getPlayersLogic().removeReference(mockGameContext, reference)).toGameContext();
    }

    @Override
    public boolean isAvailableForPlayerJoining(final com.yazino.game.api.GameStatus gameStatus) {
        return true;
    }

    @Override
    public ExecutionResult processExternalCallResult(ExecutionContext context, ExternalCallResult result) throws GameException {
        return new ExecutionResult.Builder(this, context.getGameStatus()).build();
    }

    @Override
    public Collection<com.yazino.game.api.PlayerAtTableInformation> getPlayerInformation(final com.yazino.game.api.GameStatus gameStatus) {
        final MockGameStatus mockStatus = new MockGameStatus(gameStatus);
        Collection<com.yazino.game.api.GamePlayer> gamePlayers = new HashSet<com.yazino.game.api.GamePlayer>();
        for (GamePlayerStatus gamePlayerStatus : mockStatus.getGamePlayers().getPlayers()) {
            gamePlayers.add(gamePlayerStatus.getPlayer());
        }
        return new HashSet<com.yazino.game.api.PlayerAtTableInformation>(Collections2.transform(gamePlayers, GAME_PLAYER_TRANSFORM));
    }

    @Override
    public boolean isComplete(final com.yazino.game.api.GameStatus gameStatus) {
        final MockGameStatus mockStatus = new MockGameStatus(gameStatus);
        return MockGameStatus.MockGamePhase.GameFinished.equals(mockStatus.getGamePhase());
    }

    @Override
    public boolean canBeClosed(final com.yazino.game.api.GameStatus gameStatus) {
        return isComplete(gameStatus);
    }

    @Override
    public String toAuditString(final com.yazino.game.api.GameStatus gameStatus) {
        return "";
    }
}
