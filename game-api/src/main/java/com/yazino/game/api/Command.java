package com.yazino.game.api;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

public final class Command implements Serializable {
    private static final long serialVersionUID = 5018674611283587169L;

    public enum CommandType {
        Game, Ack, GetStatus, InitialGetStatus, ObserverGetStatus;

        public static CommandType parse(final String code) {
            if (GetStatus.name().equalsIgnoreCase(code) || InitialGetStatus.name().equalsIgnoreCase(code)) {
                return InitialGetStatus;
            }
            if (Ack.name().equalsIgnoreCase(code)) {
                return Ack;
            }
            if (ObserverGetStatus.name().equalsIgnoreCase(code)) {
                return ObserverGetStatus;
            }
            return Game;
        }

        public String getCode() {
            return name().toUpperCase();
        }
    }

    private static final String[] EMPTY = new String[]{};

    private final BigDecimal tableId;
    private final Long gameId;
    private final String type;
    private final String[] args;
    private final GamePlayer player;
    private final String uuid;

    public Command(final Command command,
                   final String playerName) {
        this.tableId = command.tableId;
        this.gameId = command.gameId;
        this.type = command.type;
        this.args = command.args;
        this.uuid = command.uuid;

        this.player = new GamePlayer(command.getPlayer().getId(), command.getPlayer().getSessionId(), playerName);
    }

    public Command(final GamePlayer player,
                   final BigDecimal tableId,
                   final Long gameId,
                   final String uuid,
                   final String type) {
        this(player, tableId, gameId, uuid, type, EMPTY);
    }

    public Command(final GamePlayer player,
                   final BigDecimal tableId,
                   final Long gameId,
                   final String uuid,
                   final String type,
                   final String... args) {
        this.player = player;
        this.tableId = tableId;
        this.gameId = gameId;
        this.type = type;
        this.uuid = uuid;
        this.args = args;
    }

    public String[] getArgs() {
        return args;
    }

    public Long getGameId() {
        return gameId;
    }

    public GamePlayer getPlayer() {
        return player;
    }

    public BigDecimal getTableId() {
        return tableId;
    }

    public String getType() {
        return type;
    }

    public String getUuid() {
        return uuid;
    }

    public CommandType getCommandType() {
        return CommandType.parse(type);
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
                .append(args, rhs.args)
                .append(gameId, rhs.gameId)
                .append(player, rhs.player)
                .append(tableId, rhs.tableId)
                .append(type, rhs.type)
                .append(uuid, rhs.uuid)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(args)
                .append(gameId)
                .append(player)
                .append(tableId)
                .append(type)
                .append(uuid)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(args)
                .append(gameId)
                .append(player)
                .append(tableId)
                .append(type)
                .append(uuid)
                .toString();
    }

}
