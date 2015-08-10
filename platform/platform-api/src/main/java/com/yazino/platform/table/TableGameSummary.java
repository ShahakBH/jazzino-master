package com.yazino.platform.table;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.yazino.game.api.GameType;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

public class TableGameSummary implements Serializable {
    private static final long serialVersionUID = 4661260777653309996L;

    private final BigDecimal id;
    private final String name;
    private final Long gameId;
    private final GameType gameType;
    private final String currentGameStatus;
    private final long increment;

    public TableGameSummary(final BigDecimal id,
                            final String name,
                            final Long gameId,
                            final GameType gameType,
                            final String currentGameStatus,
                            final long increment) {
        notNull(id, "id may not be null");

        this.id = id;
        this.name = name;
        this.gameId = gameId;
        this.gameType = gameType;
        this.currentGameStatus = currentGameStatus;
        this.increment = increment;
    }

    public BigDecimal getId() {
        return id;
    }

    public Long getGameId() {
        return gameId;
    }

    public GameType getGameType() {
        return gameType;
    }

    public String getCurrentGameStatus() {
        return currentGameStatus;
    }

    public String getName() {
        return name;
    }

    public long getIncrement() {
        return increment;
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
        final TableGameSummary rhs = (TableGameSummary) obj;
        return new EqualsBuilder()
                .append(id, rhs.id)
                .append(name, rhs.name)
                .append(gameId, rhs.gameId)
                .append(gameType, rhs.gameType)
                .append(currentGameStatus, rhs.currentGameStatus)
                .append(increment, rhs.increment)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(id)
                .append(name)
                .append(gameId)
                .append(gameType)
                .append(currentGameStatus)
                .append(increment)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(id)
                .append(name)
                .append(gameId)
                .append(gameType)
                .append(currentGameStatus)
                .append(increment)
                .toString();
    }
}
