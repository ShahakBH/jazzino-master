package com.yazino.platform.playerstatistic.service;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

public class AchievementDetails implements Serializable {
    private static final long serialVersionUID = 1892037472074009463L;

    private final String achievementId;
    private final String title;
    private final Integer level;
    private final String message;
    private final String howToGet;

    public AchievementDetails(final String achievementId,
                              final String title,
                              final Integer level,
                              final String message,
                              final String howToGet) {
        this.achievementId = achievementId;
        this.title = title;
        this.level = level;
        this.message = message;
        this.howToGet = howToGet;
    }

    public String getAchievementId() {
        return achievementId;
    }

    public String getTitle() {
        return title;
    }

    public Integer getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public String getHowToGet() {
        return howToGet;
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
        final AchievementDetails rhs = (AchievementDetails) obj;
        return new EqualsBuilder()
                .append(achievementId, rhs.achievementId)
                .append(title, rhs.title)
                .append(level, rhs.level)
                .append(message, rhs.message)
                .append(howToGet, rhs.howToGet)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(achievementId)
                .append(title)
                .append(level)
                .append(message)
                .append(howToGet)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(achievementId)
                .append(title)
                .append(level)
                .append(message)
                .append(howToGet)
                .toString();
    }
}
