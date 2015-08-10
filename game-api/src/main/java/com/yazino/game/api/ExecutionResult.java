package com.yazino.game.api;

import com.yazino.game.api.statistic.GameStatistic;
import com.yazino.game.api.statistic.GameXPEvents;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.math.BigDecimal;
import java.util.*;

import static org.apache.commons.lang3.Validate.notNull;

public final class ExecutionResult {

    private final GameStatus gameStatus;
    private final GameXPEvents gameXPEvents;
    private final List<ScheduledEvent> scheduledEvents;
    private final Collection<GameStatistic> gameStatistics;
    private final Map<BigDecimal, BigDecimal> playerIdsToSessions;

    private ExecutionResult(final GameStatus gameStatus,
                            final GameXPEvents gameXPEvents,
                            final Collection<GameStatistic> gameStatistics,
                            final List<ScheduledEvent> scheduledEvents,
                            final Map<BigDecimal, BigDecimal> playerIdsToSessions) {
        this.gameStatus = gameStatus;
        this.gameXPEvents = gameXPEvents;
        this.gameStatistics = gameStatistics;
        this.scheduledEvents = scheduledEvents;
        this.playerIdsToSessions = playerIdsToSessions;
    }

    public Set<BigDecimal> playerIds() {
        if (playerIdsToSessions != null) {
            return Collections.unmodifiableSet(playerIdsToSessions.keySet());
        }
        return Collections.emptySet();
    }

    public Map<BigDecimal, BigDecimal> playerIdsToSessions() {
        return Collections.unmodifiableMap(playerIdsToSessions);
    }

    public GameStatus getGameStatus() {
        return gameStatus;
    }

    public List<ScheduledEvent> getScheduledEvents() {
        return scheduledEvents;
    }

    public GameXPEvents getGameXPEvents() {
        return gameXPEvents;
    }

    public Collection<GameStatistic> getGameStatistics() {
        return gameStatistics;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final ExecutionResult rhs = (ExecutionResult) obj;
        return new EqualsBuilder()
                .append(gameStatus, rhs.gameStatus)
                .append(scheduledEvents, rhs.scheduledEvents)
                .append(gameXPEvents, rhs.gameXPEvents)
                .append(gameStatistics, rhs.gameStatistics)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(gameStatus)
                .append(scheduledEvents)
                .append(gameXPEvents)
                .append(gameStatistics)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(gameStatus)
                .append(scheduledEvents)
                .append(gameXPEvents)
                .append(gameStatistics)
                .toString();
    }

    public static class Builder {
        private final GameStatus gameStatus;
        private final GameRules gameRules;
        private GameXPEvents gameXPEvents = new GameXPEvents();
        private List<ScheduledEvent> scheduledEvents = new ArrayList<>();
        private Collection<GameStatistic> gameStatistics = new ArrayList<>();

        public Builder(final GameRules gameRules,
                       final GameStatus gameStatus) {
            notNull(gameRules, "gameRules cannot be null");
            notNull(gameStatus, "gameStatus cannot be null");

            this.gameRules = gameRules;
            this.gameStatus = gameStatus;
        }

        public Builder gameXPEvents(final GameXPEvents newGameXPEvents) {
            this.gameXPEvents = newGameXPEvents;
            return this;
        }

        public Builder scheduledEvents(final Collection<ScheduledEvent> newScheduledEvents) {
            notNull(newScheduledEvents, "newScheduledEvents cannot be null");
            this.scheduledEvents.clear();
            this.scheduledEvents.addAll(newScheduledEvents);
            return this;
        }

        public Builder gameStatistics(final Collection<GameStatistic> newGameStatistics) {
            notNull(newGameStatistics, "newGameStatistics cannot be null");
            this.gameStatistics.clear();
            this.gameStatistics.addAll(newGameStatistics);
            return this;
        }

        private Map<BigDecimal, BigDecimal> playerIdsToSessions() {
            if (gameStatus == null) {
                return Collections.emptyMap();
            }

            final Map<BigDecimal, BigDecimal> playersToSessions = new HashMap<>();
            for (PlayerAtTableInformation playerAtTable : gameRules.getPlayerInformation(gameStatus)) {
                playersToSessions.put(playerAtTable.getPlayer().getId(), playerAtTable.getPlayer().getSessionId());
            }
            return playersToSessions;
        }

        public ExecutionResult build() {
            return new ExecutionResult(gameStatus, gameXPEvents, gameStatistics, scheduledEvents, playerIdsToSessions());
        }
    }
}
