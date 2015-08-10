package com.yazino.platform.table;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.yazino.game.api.GameType;

import java.io.Serializable;

import static org.apache.commons.lang3.Validate.notNull;

public class GameTypeInformation implements Serializable {
    private static final long serialVersionUID = -3380604631525280332L;

    private final GameType gameType;
    private final boolean available;

    public GameTypeInformation(final GameType gameType,
                               final boolean available) {
        notNull(gameType, "gameType may not be null");

        this.gameType = gameType;
        this.available = available;
    }

    public GameType getGameType() {
        return gameType;
    }

    public String getId() {
        return gameType.getId();
    }

    public boolean isAvailable() {
        return available;
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
        final GameTypeInformation rhs = (GameTypeInformation) obj;
        return new EqualsBuilder()
                .append(gameType, rhs.gameType)
                .append(available, rhs.available)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(gameType)
                .append(available)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(gameType)
                .append(available)
                .toString();
    }
}
