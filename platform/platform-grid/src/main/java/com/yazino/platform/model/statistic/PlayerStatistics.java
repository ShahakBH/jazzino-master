package com.yazino.platform.model.statistic;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.platform.playerstatistic.StatisticEvent;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;

@SpaceClass
public class PlayerStatistics implements Serializable {
    private static final long serialVersionUID = -3795458438141754376L;

    private BigDecimal playerId;
    private String gameType;
    private Collection<StatisticEvent> events;

    public PlayerStatistics() {
    }

    public PlayerStatistics(final BigDecimal playerId,
                            final String gameType,
                            final Collection<StatisticEvent> events) {
        this.playerId = playerId;
        this.gameType = gameType;
        this.events = new HashSet<StatisticEvent>(events);
    }

    @SpaceId
    @SpaceRouting
    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(final String gameType) {
        this.gameType = gameType;
    }

    public Collection<StatisticEvent> getEvents() {
        return events;
    }

    public void setEvents(final Collection<StatisticEvent> events) {
        this.events = events;
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
        final PlayerStatistics rhs = (PlayerStatistics) obj;
        return new EqualsBuilder()
                .append(gameType, rhs.gameType)
                .append(events, rhs.events)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(gameType)
                .append(events)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .append(gameType)
                .append(events)
                .toString();
    }
}
