package com.yazino.web.domain;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

public class TournamentDetailViewPlayer implements Serializable {
    private static final long serialVersionUID = -1502271958796165417L;

    private final BigDecimal playerId;
    private final String name;
    private final boolean friend;

    public TournamentDetailViewPlayer(final BigDecimal playerId,
                                      final String name,
                                      final boolean friend) {
        notNull(playerId, "playerId may not be null");

        this.playerId = playerId;
        this.name = name;
        this.friend = friend;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public String getName() {
        return name;
    }

    public boolean isFriend() {
        return friend;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        TournamentDetailViewPlayer rhs = (TournamentDetailViewPlayer) obj;
        return new EqualsBuilder()
                .append(this.playerId, rhs.playerId)
                .append(this.name, rhs.name)
                .append(this.friend, rhs.friend)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(playerId)
                .append(name)
                .append(friend)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("playerId", playerId)
                .append("name", name)
                .append("friend", friend)
                .toString();
    }
}
