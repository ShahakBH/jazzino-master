package com.yazino.platform.model.table;

import com.yazino.game.api.Command;
import com.yazino.game.api.GamePlayer;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

public final class CommandWrapper implements Serializable, IdentifiedTableRequest {
    private static final long serialVersionUID = -3275644061512783561L;

    public static final String STATUS_NEW = "NEW";

    private final TableRequestType requestType = TableRequestType.COMMAND;

    private final BigDecimal tableId;
    private final Long gameId;
    private final BigDecimal playerId;
    private final BigDecimal sessionId;
    private final String type;
    private final String[] args;
    private final String status;

    private String requestId;
    private Date timestamp;   // TODO remove date, replace with long

    public CommandWrapper(final com.yazino.platform.table.Command command) {
        this(command.getTableId(), command.getGameId(), command.getPlayerId(), command.getSessionId(), command.getType(), command.getArgs());

        this.timestamp = command.getTimestamp();
    }

    public CommandWrapper(final BigDecimal tableId,
                          final Long gameId,
                          final BigDecimal playerId,
                          final BigDecimal sessionId,
                          final String type,
                          final String... args) {
        this.tableId = tableId;
        this.gameId = gameId;
        this.playerId = playerId;
        this.sessionId = sessionId;
        this.type = type;
        this.args = args;
        this.status = STATUS_NEW;
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

    public void setTimestamp(final Date timestamp) {
        this.timestamp = timestamp;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getTableId() {
        return tableId;
    }

    public void setRequestId(final String requestId) {
        this.requestId = requestId;
    }

    public String getRequestId() {
        return requestId;
    }

    public TableRequestType getRequestType() {
        return requestType;
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
        final CommandWrapper rhs = (CommandWrapper) obj;
        return new EqualsBuilder()
                .append(status, rhs.status)
                .append(gameId, rhs.gameId)
                .append(type, rhs.type)
                .append(args, rhs.args)
                .append(requestId, rhs.requestId)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId)
                && BigDecimals.equalByComparison(sessionId, rhs.sessionId)
                && BigDecimals.equalByComparison(tableId, rhs.tableId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(status)
                .append(BigDecimals.strip(tableId))
                .append(gameId)
                .append(BigDecimals.strip(playerId))
                .append(BigDecimals.strip(sessionId))
                .append(type)
                .append(args)
                .append(requestId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(status)
                .append(tableId)
                .append(gameId)
                .append(playerId)
                .append(sessionId)
                .append(type)
                .append(args)
                .append(requestId)
                .toString();
    }

    public Command toAnonymousCommand() {
        return new Command(null, getTableId(), getGameId(), getRequestId(), getType(), getArgs());
    }

    public Command toCommand(final String playerName) {
        return new Command(new GamePlayer(playerId, sessionId, playerName), getTableId(),
                getGameId(), getRequestId(), getType(), getArgs());
    }

    public Command toCommandMissingName() {
        return new Command(new GamePlayer(playerId, sessionId, null), getTableId(),
                getGameId(), getRequestId(), getType(), getArgs());
    }
}
