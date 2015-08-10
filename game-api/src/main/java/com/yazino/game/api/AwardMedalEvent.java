package com.yazino.game.api;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigDecimal;

public class AwardMedalEvent {
    private BigDecimal trophyId;
    private String gameType;

    public AwardMedalEvent(final BigDecimal trophyId,
                           final String gameType) {
        this.trophyId = trophyId;
        this.gameType = gameType;
    }

    public BigDecimal getTrophyId() {
        return trophyId;
    }

    public String getGameType() {
        return gameType;
    }

    @Override
    public String toString() {
        return "AwardMedalEvent{"
                + "awardId=" + trophyId
                + ", gameType='" + gameType + '\''
                + '}';
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
        final AwardMedalEvent rhs = (AwardMedalEvent) obj;
        return new EqualsBuilder()
                .append(trophyId, rhs.trophyId)
                .append(gameType, rhs.gameType)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(trophyId)
                .append(gameType)
                .toHashCode();
    }

}
