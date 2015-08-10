package com.yazino.platform.test.game.status;

import com.yazino.game.api.GamePlayer;
import com.yazino.game.api.document.DocumentBuilder;
import com.yazino.game.generic.events.PendingGameEvent;
import com.yazino.game.generic.status.GameChanges;
import com.yazino.game.generic.status.GamePlayers;
import com.yazino.game.generic.status.GenericGameStatus;
import com.yazino.game.generic.status.PendingGameEvents;
import com.yazino.platform.test.game.GameRuleVariation;

import java.io.Serializable;
import java.util.*;

import static com.yazino.game.api.document.DocumentAccessors.enumFor;
import static org.apache.commons.lang3.Validate.notNull;

public class MockGameStatus extends GenericGameStatus<GameRuleVariation, GamePlayerStatus> implements Serializable {
    public static final List<PendingGameEvent> EMPTY_EVENTS = Collections.unmodifiableList(new ArrayList<PendingGameEvent>());
    private static final long serialVersionUID = -3220203462403877517L;

    public enum MockGamePhase {Playing, GameFinished}

    private MockGamePhase gamePhase;

    public MockGamePhase getGamePhase() {
        return gamePhase;
    }

    public static MockGameStatus emptyStatus(Map<String, String> initialProperties) {
        return new MockGameStatus(
                GameRuleVariation.withProperties(initialProperties),
                new GamePlayers<GamePlayerStatus>(),
                new PendingGameEvents(EMPTY_EVENTS, EMPTY_EVENTS),
                GameChanges.emptyChanges()
        );
    }

    public MockGameStatus(final GameRuleVariation variation,
                          final GamePlayers<GamePlayerStatus> gamePlayerStatusGamePlayers,
                          final PendingGameEvents gameEvents,
                          final GameChanges gameChanges) {
        super(variation, gamePlayerStatusGamePlayers, gameEvents, gameChanges);
    }

    public MockGameStatus(final Map<String, Object> document) {
        super(document);

        notNull(document, "document may not be null");

        gamePhase = enumFor(document, MockGamePhase.class, "gamePhase");
    }

    @Override
    public Map<String, Object> toDocument() {
        return new DocumentBuilder(super.toDocument())
                .withEnum("gamePhase", gamePhase)
                .toDocument();
    }

    public MockGameStatus withPhase(MockGamePhase gamePhase) {
        this.gamePhase = gamePhase;
        return this;
    }

    @Override
    public GenericGameStatus<GameRuleVariation, GamePlayerStatus> withGamePlayers(final GamePlayers<GamePlayerStatus> gamePlayers) {
        return new MockGameStatus(getVariation(), gamePlayers, getGameEvents(), getGameChanges());
    }

    @Override
    public GenericGameStatus<GameRuleVariation, GamePlayerStatus> withGameChanges(final GameChanges gameChanges) {
        return new MockGameStatus(getVariation(), getGamePlayers(), getGameEvents(), gameChanges);
    }

    @Override
    public GenericGameStatus<GameRuleVariation, GamePlayerStatus> withGameEvents(final PendingGameEvents gameEvents) {
        return new MockGameStatus(getVariation(), getGamePlayers(), gameEvents, getGameChanges());
    }

    @Override
    public Set<String> calculateAllowedActionsForPlayer(GamePlayer player) {
        return Collections.emptySet();
    }
}
