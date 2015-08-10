package com.yazino.platform.playerstatistic.service;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

public class LevelInfo implements Serializable {
    private static final long serialVersionUID = -3168261598887477082L;

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int ONE_HUNDRED_PERCENT = 100;

    private final int level;
    private final long points;
    private final long toNextLevel;

    public LevelInfo(final int level,
                     final long points, final
    long toNextLevel) {
        this.level = level;
        this.points = points;
        this.toNextLevel = toNextLevel;
    }

    public int getLevel() {
        return level;
    }

    public int getNextLevel() {
        return level + 1;
    }

    public long getPoints() {
        return points;
    }

    public long getToNextLevel() {
        return toNextLevel;
    }

    public float getPercentage() {
        if (toNextLevel == 0) {
            return 0;
        }
        if (points > toNextLevel) {
            return ONE_HUNDRED_PERCENT;
        }
        final BigDecimal pointsBD = BigDecimal.valueOf(points);
        final BigDecimal toNextLevelBD = BigDecimal.valueOf(toNextLevel);
        final BigDecimal factor = pointsBD.divide(toNextLevelBD, 2, BigDecimal.ROUND_DOWN);
        return factor.multiply(HUNDRED).setScale(2).floatValue();
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
        final LevelInfo rhs = (LevelInfo) obj;
        return new EqualsBuilder()
                .append(level, rhs.level)
                .append(points, rhs.points)
                .append(toNextLevel, rhs.toNextLevel)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(level)
                .append(points)
                .append(toNextLevel)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(level)
                .append(points)
                .append(toNextLevel)
                .toString();
    }
}
