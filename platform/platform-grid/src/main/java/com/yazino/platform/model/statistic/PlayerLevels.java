package com.yazino.platform.model.statistic;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

@SpaceClass
public class PlayerLevels implements Serializable {
    private static final long serialVersionUID = -2046376860625936926L;

    private BigDecimal playerId;
    private Map<String, PlayerLevel> levels;

    public PlayerLevels() {
    }

    public PlayerLevels(final BigDecimal playerId) {
        notNull(playerId, "playerId is null");

        this.playerId = playerId;
    }

    public PlayerLevels(final BigDecimal playerId,
                        final Map<String, PlayerLevel> levels) {
        notNull(playerId, "playerId is null");
        notNull(levels, "levels is null");

        this.playerId = playerId;
        this.levels = new HashMap<String, PlayerLevel>(levels);
    }

    @SpaceId
    @SpaceRouting
    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public Map<String, PlayerLevel> getLevels() {
        return levels;
    }

    public void setLevels(final Map<String, PlayerLevel> levels) {
        this.levels = levels;
    }

    public PlayerLevel retrievePlayerLevel(final String gameType) {
        if (levels == null) {
            return PlayerLevel.STARTING_LEVEL;
        }
        final PlayerLevel playerLevel = levels.get(gameType);
        if (playerLevel == null) {
            return PlayerLevel.STARTING_LEVEL;
        }
        return playerLevel;
    }

    public void updateLevel(final String gameType, final PlayerLevel updatedPlayerLevel) {
        getLevels().put(gameType, updatedPlayerLevel);
    }

    public int retrieveLevel(final String gameType) {
        return retrievePlayerLevel(gameType).getLevel();
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
        final PlayerLevels rhs = (PlayerLevels) obj;
        return new EqualsBuilder()
                .append(levels, rhs.levels)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(levels)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .append(levels)
                .toString();
    }
}
