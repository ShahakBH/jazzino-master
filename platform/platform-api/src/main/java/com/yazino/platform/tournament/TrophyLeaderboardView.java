package com.yazino.platform.tournament;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

import static com.google.common.collect.Collections2.filter;

public class TrophyLeaderboardView implements Serializable {
    private static final long serialVersionUID = 7579392453989218898L;

    private final Map<Integer, TrophyLeaderboardPosition> positionData = new HashMap<>();

    private final BigDecimal id;
    private final String name;
    private final Boolean active;
    private final String gameType;
    private final Long pointBonusPerPlayer;
    private final DateTime startTime;
    private final DateTime endTime;
    private final DateTime currentCycleEnd;
    private final Duration cycle;
    private final TrophyLeaderboardPlayers players;

    public TrophyLeaderboardView(final BigDecimal id,
                                 final String name,
                                 final Boolean active,
                                 final String gameType,
                                 final Long pointBonusPerPlayer,
                                 final DateTime startTime,
                                 final DateTime endTime,
                                 final DateTime currentCycleEnd,
                                 final Duration cycle,
                                 final Map<Integer, TrophyLeaderboardPosition> positionData,
                                 final TrophyLeaderboardPlayers players) {
        this.id = id;
        this.name = name;
        this.active = active;
        this.gameType = gameType;
        this.pointBonusPerPlayer = pointBonusPerPlayer;
        this.startTime = startTime;
        this.endTime = endTime;
        this.currentCycleEnd = currentCycleEnd;
        this.cycle = cycle;
        this.players = players;

        if (positionData != null) {
            this.positionData.putAll(positionData);
        }
    }

    public List<TrophyLeaderboardPlayer> getOrderedByPosition() {
        if (players != null) {
            return players.getOrderedByPosition();
        }
        return Collections.emptyList();
    }

    public List<TrophyLeaderboardPlayer> getFilteredOrderedByPosition(final Set<BigDecimal> filterIds) {
        final List<TrophyLeaderboardPlayer> positions = new ArrayList<>(getOrderedByPosition());
        return Lists.newArrayList(filter(positions, new Predicate<TrophyLeaderboardPlayer>() {
            @Override
            public boolean apply(final TrophyLeaderboardPlayer trophyLeaderboardPlayer) {
                return filterIds.contains(trophyLeaderboardPlayer.getPlayerId());
            }
        }));
    }


    public BigDecimal getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Boolean getActive() {
        return active;
    }

    public String getGameType() {
        return gameType;
    }

    public Long getPointBonusPerPlayer() {
        return pointBonusPerPlayer;
    }

    public DateTime getStartTime() {
        return startTime;
    }

    public DateTime getEndTime() {
        return endTime;
    }

    public DateTime getCurrentCycleEnd() {
        return currentCycleEnd;
    }

    public Duration getCycle() {
        return cycle;
    }

    public Map<Integer, TrophyLeaderboardPosition> getPositionData() {
        return positionData;
    }

    public TrophyLeaderboardPlayers getPlayers() {
        return players;
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
        final TrophyLeaderboardView rhs = (TrophyLeaderboardView) obj;
        return new EqualsBuilder()
                .append(id, rhs.id)
                .append(name, rhs.name)
                .append(active, rhs.active)
                .append(gameType, rhs.gameType)
                .append(pointBonusPerPlayer, rhs.pointBonusPerPlayer)
                .append(startTime, rhs.startTime)
                .append(endTime, rhs.endTime)
                .append(currentCycleEnd, rhs.currentCycleEnd)
                .append(cycle, rhs.cycle)
                .append(positionData, rhs.positionData)
                .append(players, rhs.players)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 19)
                .append(id)
                .append(name)
                .append(active)
                .append(gameType)
                .append(pointBonusPerPlayer)
                .append(startTime)
                .append(endTime)
                .append(currentCycleEnd)
                .append(cycle)
                .append(positionData)
                .append(players)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(id)
                .append(name)
                .append(active)
                .append(gameType)
                .append(pointBonusPerPlayer)
                .append(startTime)
                .append(endTime)
                .append(currentCycleEnd)
                .append(cycle)
                .append(positionData)
                .append(players)
                .toString();
    }
}
