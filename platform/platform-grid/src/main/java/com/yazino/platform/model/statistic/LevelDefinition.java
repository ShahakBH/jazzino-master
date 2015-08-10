package com.yazino.platform.model.statistic;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

public class LevelDefinition implements Serializable {
    private static final long serialVersionUID = 8850945433659257176L;
    private final int level;
    private final BigDecimal minimumPoints;
    private final BigDecimal maximumPoints;
    private final BigDecimal chips;

    public LevelDefinition(final int level,
                           final BigDecimal minimumPoints,
                           final BigDecimal maximumPoints,
                           final BigDecimal chips) {
        this.level = level;
        this.minimumPoints = minimumPoints;
        this.maximumPoints = maximumPoints;
        this.chips = chips;
    }

    public int getLevel() {
        return level;
    }

    public BigDecimal getMinimumPoints() {
        return minimumPoints;
    }

    public BigDecimal getMaximumPoints() {
        return maximumPoints;
    }

    public BigDecimal getChips() {
        return chips;
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
        final LevelDefinition rhs = (LevelDefinition) obj;
        return new EqualsBuilder()
                .append(level, rhs.level)
                .append(minimumPoints, rhs.minimumPoints)
                .append(maximumPoints, rhs.maximumPoints)
                .append(chips, rhs.chips)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(level)
                .append(minimumPoints)
                .append(maximumPoints)
                .append(chips)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(level)
                .append(minimumPoints)
                .append(maximumPoints)
                .append(chips)
                .toString();
    }
}
