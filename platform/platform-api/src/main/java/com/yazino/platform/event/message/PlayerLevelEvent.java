package com.yazino.platform.event.message;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

import static org.apache.commons.lang3.Validate.notNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerLevelEvent implements PlatformEvent, Serializable {
    private static final long serialVersionUID = -2567451651798086843L;
    @JsonProperty("id")
    private String playerId;
    @JsonProperty("lvl")
    private String level;
    @JsonProperty("typ")
    private String gameType;

    private PlayerLevelEvent() {
    }

    public PlayerLevelEvent(final String playerId,
                            final String gameType,
                            final String level) {
        notNull(playerId, "playerId may not be null");
        notNull(gameType, "gameType may not be null");
        notNull(level, "level may not be null");
        this.playerId = playerId;
        this.gameType = gameType;
        this.level = level;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public EventMessageType getMessageType() {
        return EventMessageType.PLAYER_NEW_LEVEL;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getGameType() {
        return gameType;
    }

    public String getLevel() {
        return level;
    }

    private void setPlayerId(final String playerId) {
        this.playerId = playerId;
    }

    private void setGameType(final String gameType) {
        this.gameType = gameType;
    }

    private void setLevel(final String level) {
        this.level = level;
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
        final PlayerLevelEvent rhs = (PlayerLevelEvent) obj;
        return new EqualsBuilder()
                .append(gameType, rhs.gameType)
                .append(level, rhs.level)
                .append(playerId, rhs.playerId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(gameType)
                .append(level)
                .append(playerId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "PlayerLevelEvent{"
                + "playerId='" + playerId + '\''
                + ", level='" + level + '\''
                + ", gameType='" + gameType + '\''
                + '}';
    }
}
