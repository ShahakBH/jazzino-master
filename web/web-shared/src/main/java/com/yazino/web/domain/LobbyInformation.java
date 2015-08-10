package com.yazino.web.domain;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

public class LobbyInformation implements Serializable {
    private static final long serialVersionUID = 7241530510766766625L;

    private final String gameType;
    private final int onlinePlayers;
    private final int activeTables;
    private final boolean available;

    public LobbyInformation(final String gameType,
                            final int onlinePlayers,
                            final int activeTables,
                            final boolean available) {
        this.gameType = gameType;
        this.onlinePlayers = onlinePlayers;
        this.activeTables = activeTables;
        this.available = available;
    }

    public String getGameType() {
        return gameType;
    }

    public int getOnlinePlayers() {
        return onlinePlayers;
    }

    public int getActiveTables() {
        return activeTables;
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
        final LobbyInformation rhs = (LobbyInformation) obj;
        return new EqualsBuilder()
                .append(gameType, rhs.gameType)
                .append(onlinePlayers, rhs.onlinePlayers)
                .append(activeTables, rhs.activeTables)
                .append(available, rhs.available)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(gameType)
                .append(onlinePlayers)
                .append(activeTables)
                .append(available)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(gameType)
                .append(onlinePlayers)
                .append(activeTables)
                .append(available)
                .toString();
    }
}
