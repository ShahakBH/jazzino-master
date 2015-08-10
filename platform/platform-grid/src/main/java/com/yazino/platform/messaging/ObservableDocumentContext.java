package com.yazino.platform.messaging;


import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.yazino.game.api.Command;
import com.yazino.game.api.ObservableChange;
import com.yazino.game.api.ObservableStatus;
import com.yazino.game.api.ParameterisedMessage;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

public final class ObservableDocumentContext implements Serializable {
    private static final long serialVersionUID = -6210151685137315067L;

    public static class Builder {
        private BigDecimal tableId;
        private long gameId;
        private ObservableStatus status;
        private long increment;
        private List<ObservableChange> lastGameChanges = new ArrayList<ObservableChange>();
        private BigDecimal playerBalance = null;
        private ParameterisedMessage message = null;
        private Map<String, String> tableProperties = null;
        private String commandUUID = null;
        private boolean aPlayer = false;
        private long acknowlegedIncrement = 0;
        private long incrementOfGameStart = 0;
        private Collection<BigDecimal> playerIds = Collections.emptyList();

        public Builder(final BigDecimal tableId,
                       final long gameId,
                       final ObservableStatus status,
                       final long increment,
                       final long incrementOfGameStart) {
            this.tableId = tableId;
            this.gameId = gameId;
            this.status = status;
            this.increment = increment;
            this.incrementOfGameStart = incrementOfGameStart;
        }

        public Builder withPlayerBalance(final BigDecimal newPlayerBalance) {
            this.playerBalance = newPlayerBalance;
            return this;
        }

        public Builder withIsAPlayer(final boolean newAPlayer) {
            this.aPlayer = newAPlayer;
            return this;
        }

        public Builder withMessage(final ParameterisedMessage newMessage) {
            this.message = newMessage;
            return this;
        }

        public Builder withCommand(final Command command) {
            if (command != null) {
                this.commandUUID = command.getUuid();
            }
            return this;
        }

        public Builder withTableProperties(final Map<String, String> newTableProperties) {
            this.tableProperties = newTableProperties;
            return this;
        }

        public long getIncrementOfGameStart() {
            return incrementOfGameStart;
        }

        public Builder withLastGameChanges(final List<ObservableChange> newLastGameChanges) {
            this.lastGameChanges.clear();
            this.lastGameChanges.addAll(newLastGameChanges);
            return this;
        }

        public Builder withAcknowlegedIncrement(final long newAcknowlegedIncrement) {
            this.acknowlegedIncrement = newAcknowlegedIncrement;
            return this;
        }

        public Builder withPlayerIds(final Collection<BigDecimal> newPlayerIds) {
            if (newPlayerIds == null) {
                this.playerIds = Collections.emptyList();
            } else {
                this.playerIds = newPlayerIds;
            }
            return this;
        }

        private List<ObservableChange> getChangesSinceIncrement(final long newIncrement) {
            final List<ObservableChange> mergedAndFilteredChanges = new ArrayList<ObservableChange>();

            for (ObservableChange change : lastGameChanges) {
                if (change.getIncrement() > newIncrement) {
                    mergedAndFilteredChanges.add(change);
                }
            }
            if (status != null) {
                for (ObservableChange change : status.getObservableChanges()) {
                    if (change.getIncrement() > newIncrement) {
                        mergedAndFilteredChanges.add(change);
                    }
                }
            }
            return mergedAndFilteredChanges;
        }

        public ObservableDocumentContext build() {
            return new ObservableDocumentContext(
                    tableId,
                    gameId,
                    playerBalance,
                    status,
                    message,
                    tableProperties,
                    commandUUID,
                    aPlayer,
                    increment,
                    getChangesSinceIncrement(acknowlegedIncrement),
                    incrementOfGameStart,
                    playerIds
            );
        }
    }

    private final BigDecimal tableId;
    private final long gameId;
    private final BigDecimal playerBalance;
    private final ObservableStatus status;
    private final ParameterisedMessage message;
    private final Map<String, String> tableProperties;
    private final String commandUUID;
    private final boolean aPlayer;
    private final long increment;
    private final List<ObservableChange> mergedGameChanges;
    private final long incrementOfGameStart;
    private final Collection<BigDecimal> playerIds;

    private ObservableDocumentContext(final BigDecimal tableId,
                                      final long gameId,
                                      final BigDecimal playerBalance,
                                      final ObservableStatus status,
                                      final ParameterisedMessage message,
                                      final Map<String, String> tableProperties,
                                      final String commandUUID,
                                      final boolean isAPlayer,
                                      final long increment,
                                      final List<ObservableChange> mergedGameChanges,
                                      final long incrementOfGameStart,
                                      final Collection<BigDecimal> playerIds) {
        this.tableId = tableId;
        this.gameId = gameId;
        this.playerBalance = playerBalance;
        this.status = status;
        this.message = message;
        this.tableProperties = tableProperties;
        this.commandUUID = commandUUID;
        this.aPlayer = isAPlayer;
        this.increment = increment;
        this.mergedGameChanges = mergedGameChanges;
        this.incrementOfGameStart = incrementOfGameStart;
        this.playerIds = playerIds;
    }

    public BigDecimal getTableId() {
        return tableId;
    }

    public Collection<BigDecimal> getPlayerIds() {
        return playerIds;
    }

    public long getGameId() {
        return gameId;
    }

    public BigDecimal getPlayerBalance() {
        return playerBalance;
    }

    public ObservableStatus getStatus() {
        return status;
    }

    public boolean isaPlayer() {
        return aPlayer;
    }

    public String getCommandUUID() {
        return commandUUID;
    }

    public ParameterisedMessage getMessage() {
        return message;
    }

    public Map<String, String> getTableProperties() {
        return tableProperties;
    }

    public long getIncrement() {
        return increment;
    }

    public List<ObservableChange> getMergedGameChanges() {
        return mergedGameChanges;
    }

    public long getIncrementOfGameStart() {
        return incrementOfGameStart;
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
        final ObservableDocumentContext rhs = (ObservableDocumentContext) obj;
        return new EqualsBuilder()
                .append(aPlayer, rhs.aPlayer)
                .append(gameId, rhs.gameId)
                .append(increment, rhs.increment)
                .append(incrementOfGameStart, rhs.incrementOfGameStart)
                .append(commandUUID, rhs.commandUUID)
                .append(mergedGameChanges, rhs.mergedGameChanges)
                .append(message, rhs.message)
                .append(playerBalance, rhs.playerBalance)
                .append(playerIds, rhs.playerIds)
                .append(status, rhs.status)
                .append(tableId, rhs.tableId)
                .append(tableProperties, rhs.tableProperties)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(aPlayer)
                .append(gameId)
                .append(increment)
                .append(incrementOfGameStart)
                .append(commandUUID)
                .append(mergedGameChanges)
                .append(message)
                .append(playerBalance)
                .append(playerIds)
                .append(status)
                .append(tableId)
                .append(tableProperties)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(aPlayer)
                .append(gameId)
                .append(increment)
                .append(incrementOfGameStart)
                .append(commandUUID)
                .append(mergedGameChanges)
                .append(message)
                .append(playerBalance)
                .append(playerIds)
                .append(status)
                .append(tableId)
                .append(tableProperties)
                .toString();
    }
}
