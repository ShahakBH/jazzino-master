package com.yazino.platform.model.community;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceIndex;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * This class represents a trophy that has been awarded at a particular time.
 */
@SpaceClass
public class PlayerTrophy implements Serializable {
    private static final long serialVersionUID = -3912234893798572331L;

    private BigDecimal playerId;
    private BigDecimal trophyId;
    private DateTime awardTime;

    public PlayerTrophy() {
    }

    public PlayerTrophy(final BigDecimal playerId,
                        final BigDecimal trophyId,
                        final DateTime awardTime) {
        notNull(playerId, "playerId is null");
        notNull(trophyId, "trophyId is null");
        notNull(awardTime, "awardTime is null");
        this.playerId = playerId;
        this.trophyId = trophyId;
        this.awardTime = awardTime;
    }

    @SpaceId
    public String getId() {
        if (playerId == null || trophyId == null || awardTime == null) {
            return null;
        }
        return String.valueOf(playerId) + "_" + String.valueOf(trophyId) + "_" + String.valueOf(awardTime);
    }

    public void setId(final String id) {
        // for gigaspace
    }

    @SpaceIndex
    public DateTime getAwardTime() {
        return awardTime;
    }

    public void setAwardTime(final DateTime awardTime) {
        notNull(awardTime, "awartTime must not be null");
        this.awardTime = awardTime;
    }

    @SpaceIndex
    public BigDecimal getTrophyId() {
        return trophyId;
    }

    public void setTrophyId(final BigDecimal trophyId) {
        notNull(trophyId, "trophyId must not be null");
        this.trophyId = trophyId;
    }

    @SpaceIndex
    @SpaceRouting
    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        notNull(playerId, "playerId must not be null");
        this.playerId = playerId;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj.getClass() != getClass()) {
            return false;
        }

        final PlayerTrophy rhs = (PlayerTrophy) obj;
        return new EqualsBuilder()
                .append(trophyId, rhs.trophyId)
                .append(awardTime, rhs.awardTime)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(trophyId)
                .append(awardTime)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
