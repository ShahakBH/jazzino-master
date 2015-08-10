package com.yazino.platform.tournament;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.Duration;
import org.joda.time.Interval;

import java.io.Serializable;
import java.util.Map;

public class TrophyLeaderboardDefinition implements Serializable {
    private static final long serialVersionUID = 7579392453989218898L;

	private final String name;
	private final Boolean active;
	private final String gameType;
	private final Long pointBonusPerPlayer;
	private final Interval validInterval;
	private final Duration cycle;
	private final Map<Integer, TrophyLeaderboardPosition> positionData;

	public TrophyLeaderboardDefinition(final String name,
                                       final String gameType,
                                       final Interval validInterval,
                                       final Duration cycle,
                                       final Long pointBonusForPlayer,
                                       final Map<Integer, TrophyLeaderboardPosition> positionData) {
		this.name = name;
		this.gameType = gameType;
        this.validInterval = validInterval;
		this.cycle = cycle;
		this.active = true;
        this.pointBonusPerPlayer = pointBonusForPlayer;
        this.positionData = positionData;
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

    public Interval getValidInterval() {
        return validInterval;
    }

    public Duration getCycle() {
        return cycle;
    }

    public Map<Integer, TrophyLeaderboardPosition> getPositionData() {
        return positionData;
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
        final TrophyLeaderboardDefinition rhs = (TrophyLeaderboardDefinition) obj;
        return new EqualsBuilder()
                .append(name, rhs.name)
                .append(active, rhs.active)
                .append(gameType, rhs.gameType)
                .append(pointBonusPerPlayer, rhs.pointBonusPerPlayer)
                .append(validInterval, rhs.validInterval)
                .append(cycle, rhs.cycle)
                .append(positionData, rhs.positionData)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 19)
                .append(name)
                .append(active)
                .append(gameType)
                .append(pointBonusPerPlayer)
                .append(validInterval)
                .append(cycle)
                .append(positionData)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(name)
                .append(active)
                .append(gameType)
                .append(pointBonusPerPlayer)
                .append(validInterval)
                .append(cycle)
                .append(positionData)
                .toString();
    }
}
