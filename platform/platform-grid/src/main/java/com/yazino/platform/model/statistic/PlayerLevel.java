package com.yazino.platform.model.statistic;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

public class PlayerLevel implements Serializable {
    private static final long serialVersionUID = 5686689367035746231L;

    public static final PlayerLevel STARTING_LEVEL = new PlayerLevel(1, BigDecimal.ZERO);
    private final int level;
    private final BigDecimal experience;

    public PlayerLevel(final int level,
                       final BigDecimal experience) {
        this.level = level;
        this.experience = experience;
    }

    public int getLevel() {
        return level;
    }

    public BigDecimal getExperience() {
        return experience;
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
        final PlayerLevel rhs = (PlayerLevel) obj;
        return new EqualsBuilder()
                .append(level, rhs.level)
                .append(experience, rhs.experience)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(level)
                .append(experience)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(level)
                .append(experience)
                .toString();
    }

}
