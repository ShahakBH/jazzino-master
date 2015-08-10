package com.yazino.platform.playerstatistic.service;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

public class AchievementInfo implements Serializable {
    private static final long serialVersionUID = -2016756039662122787L;

    private final int achievements;
    private final int totalAchievements;

    public AchievementInfo(final int achievements,
                           final int totalAchievements) {
        this.achievements = achievements;
        this.totalAchievements = totalAchievements;
    }

    public int getAchievements() {
        return achievements;
    }

    public int getTotalAchievements() {
        return totalAchievements;
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
        final AchievementInfo rhs = (AchievementInfo) obj;
        return new EqualsBuilder()
                .append(achievements, rhs.achievements)
                .append(totalAchievements, rhs.totalAchievements)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(achievements)
                .append(totalAchievements)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(achievements)
                .append(totalAchievements)
                .toString();
    }
}
