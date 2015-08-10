package com.yazino.platform.table;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

public final class Command implements Serializable {
    private static final long serialVersionUID = -3275644061512783561L;

    private final BigDecimal tableId;
    private final Long gameId;
    private final BigDecimal playerId;
    private final BigDecimal sessionId;
    private final String type;
    private final String[] args;
    private final Date timestamp;

    public Command(final BigDecimal tableId,
                   final Long gameId,
                   final BigDecimal playerId,
                   final BigDecimal sessionId,
                   final String type,
                   final String... args) {
        this(tableId, gameId, playerId, sessionId, type, null, args);
    }

    public Command(final BigDecimal tableId,
                   final Long gameId,
                   final BigDecimal playerId,
                   final BigDecimal sessionId,
                   final String type,
                   final Date timestamp,
                   final String... args) {
        this.tableId = tableId;
        this.gameId = gameId;
        this.playerId = playerId;
        this.sessionId = sessionId;
        this.type = type;
        this.timestamp = timestamp;
        this.args = args;
    }

    public Command withTimestamp(final Date newTimestamp) {
        return new Command(tableId, gameId, playerId, sessionId, type, newTimestamp, args);
    }

    public Long getGameId() {
        return gameId;
    }

    public String getType() {
        return type;
    }

    public String[] getArgs() {
        return args;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public BigDecimal getSessionId() {
        return sessionId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public BigDecimal getTableId() {
        return tableId;
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
        final Command rhs = (Command) obj;
        return new EqualsBuilder()
                .append(gameId, rhs.gameId)
                .append(type, rhs.type)
                .append(args, rhs.args)
                .append(timestamp, rhs.timestamp)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId)
                && BigDecimals.equalByComparison(sessionId, rhs.sessionId)
                && BigDecimals.equalByComparison(tableId, rhs.tableId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(tableId))
                .append(gameId)
                .append(BigDecimals.strip(playerId))
                .append(BigDecimals.strip(sessionId))
                .append(type)
                .append(args)
                .append(timestamp)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(tableId)
                .append(gameId)
                .append(playerId)
                .append(sessionId)
                .append(type)
                .append(args)
                .append(timestamp)
                .toString();
    }
}
