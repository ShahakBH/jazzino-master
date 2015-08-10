package com.yazino.platform.messaging;

import com.yazino.platform.table.PlayerInformation;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.yazino.game.api.Command;
import com.yazino.game.api.ExecutionResult;
import com.yazino.game.api.GameStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

public class DocumentContext implements Serializable {
    private static final long serialVersionUID = 6960485560820488114L;

    private final DocumentType documentType;
    private final BigDecimal tableId;
    private final Long gameId;
    private final String gameType;
    private final long increment;
    private final long incrementOfGameStart;
    private final Command command;
    private final Collection<PlayerInformation> players;
    private final GameStatus previousGame;
    private final GameStatus game;
    private final Set<BigDecimal> playerIdsInPreviousGame;
    private final ExecutionResult executionResult;

    DocumentContext(final DocumentType documentType,
                    final BigDecimal tableId,
                    final Long gameId,
                    final String gameType,
                    final long increment,
                    final long incrementOfGameStart,
                    final Command command,
                    final Collection<PlayerInformation> players,
                    final GameStatus previousGame,
                    final GameStatus game,
                    final Set<BigDecimal> playerIdsInPreviousGame,
                    final ExecutionResult executionResult) {
        notNull(documentType, "documentType may not be null");

        this.documentType = documentType;
        this.tableId = tableId;
        this.gameId = gameId;
        this.gameType = gameType;
        this.increment = increment;
        this.incrementOfGameStart = incrementOfGameStart;
        this.command = command;
        this.players = players;
        this.previousGame = previousGame;
        this.game = game;
        this.playerIdsInPreviousGame = playerIdsInPreviousGame;
        this.executionResult = executionResult;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public BigDecimal getTableId() {
        return tableId;
    }

    public Long getGameId() {
        return gameId;
    }

    public String getGameType() {
        return gameType;
    }

    public long getIncrement() {
        return increment;
    }

    public long getIncrementOfGameStart() {
        return incrementOfGameStart;
    }

    public Command getCommand() {
        return command;
    }

    public Collection<PlayerInformation> getPlayers() {
        return players;
    }

    public GameStatus getPreviousGame() {
        return previousGame;
    }

    public GameStatus getGame() {
        return game;
    }

    public Set<BigDecimal> getPlayerIdsInPreviousGame() {
        return playerIdsInPreviousGame;
    }

    public ExecutionResult getExecutionResult() {
        return executionResult;
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
        final DocumentContext rhs = (DocumentContext) obj;
        return new EqualsBuilder()
                .append(documentType, rhs.documentType)
                .append(tableId, rhs.tableId)
                .append(gameId, rhs.gameId)
                .append(gameType, rhs.gameType)
                .append(increment, rhs.increment)
                .append(incrementOfGameStart, rhs.incrementOfGameStart)
                .append(command, rhs.command)
                .append(players, rhs.players)
                .append(previousGame, rhs.previousGame)
                .append(game, rhs.game)
                .append(playerIdsInPreviousGame, rhs.playerIdsInPreviousGame)
                .append(executionResult, rhs.executionResult)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(documentType)
                .append(tableId)
                .append(gameId)
                .append(gameType)
                .append(increment)
                .append(incrementOfGameStart)
                .append(command)
                .append(players)
                .append(previousGame)
                .append(game)
                .append(playerIdsInPreviousGame)
                .append(executionResult)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(documentType)
                .append(tableId)
                .append(gameId)
                .append(gameType)
                .append(increment)
                .append(incrementOfGameStart)
                .append(command)
                .append(players)
                .append(previousGame)
                .append(game)
                .append(playerIdsInPreviousGame)
                .append(executionResult)
                .toString();
    }

    public static class Builder {
        private final DocumentType documentType;

        private BigDecimal tableId;
        private Long gameId;
        private String gameType;
        private long increment;
        private long incrementOfGameStart;
        private Command command;
        private Collection<PlayerInformation> players;
        private GameStatus previousGame;
        private Set<BigDecimal> playerIdsInPreviousGame;
        private ExecutionResult executionResult;

        public Builder(final DocumentType documentType) {
            notNull(documentType, "Document Type may not be null");

            this.documentType = documentType;
        }

        public DocumentContext build() {
            return new DocumentContext(documentType,
                    tableId,
                    gameId,
                    gameType,
                    increment,
                    incrementOfGameStart,
                    command,
                    players,
                    previousGame,
                    previousGame,
                    playerIdsInPreviousGame,
                    executionResult);
        }

        public Builder withTableId(final BigDecimal newTableId) {
            this.tableId = newTableId;
            return this;
        }

        public Builder withGameId(final Long newGameId) {
            this.gameId = newGameId;
            return this;
        }

        public Builder withGameType(final String newGameType) {
            this.gameType = newGameType;
            return this;
        }

        public Builder withIncrement(final long newIncrement) {
            this.increment = newIncrement;
            return this;
        }

        public Builder withIncrementOfGameStart(final long newIncrementOfGameStart) {
            this.incrementOfGameStart = newIncrementOfGameStart;
            return this;
        }

        public Builder withCommand(final Command newCommand) {
            this.command = newCommand;
            return this;
        }

        public Builder withPlayerInformation(final Collection<PlayerInformation> newPlayers) {
            this.players = newPlayers;
            return this;
        }

        public Builder withPreviousGame(final GameStatus newPreviousGame) {
            this.previousGame = newPreviousGame;
            return this;
        }

        public Builder withPlayersInPreviousGame(final Set<BigDecimal> newPlayerIdsInPreviousGame) {
            this.playerIdsInPreviousGame = newPlayerIdsInPreviousGame;
            return this;
        }

        public Builder withExecutionResult(final ExecutionResult newExecutionResult) {
            this.executionResult = newExecutionResult;
            return this;
        }
    }
}
