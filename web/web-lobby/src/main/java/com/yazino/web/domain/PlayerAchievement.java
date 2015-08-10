package com.yazino.web.domain;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.Comparator;

import static org.apache.commons.lang3.Validate.notNull;

public class PlayerAchievement implements Serializable, Comparable<PlayerAchievement>, Comparator<PlayerAchievement> {
    private static final long serialVersionUID = -962713174041167610L;

    private final String achievementId;
    private final String title;
    private final String message;
    private final String howToGet;
    private final int level;
    private final boolean hasAchievement;

    public PlayerAchievement(final String achievementId,
                             final String title,
                             final int level,
                             final String message,
                             final String howToGet,
                             final boolean hasAchievement) {
        notNull(achievementId, "achievementId may not be null");

        this.achievementId = achievementId;
        this.title = title;
        this.level = level;
        this.message = message;
        this.howToGet = howToGet;
        this.hasAchievement = hasAchievement;
    }

    public String getHowToGet() {
        return howToGet;
    }

    public String getTitle() {
        return title;
    }

    public boolean isHasAchievement() {
        return hasAchievement;
    }

    public String getAchievementId() {
        return achievementId;
    }

    public int getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public int compareTo(final PlayerAchievement other) {
        if (other == null) {
            return 1;
        }

        return achievementId.compareTo(other.achievementId);
    }

    @Override
    public int compare(final PlayerAchievement achievement1,
                       final PlayerAchievement achievement2) {
        if (achievement1 == null && achievement2 == null) {
            return 0;
        }
        if (achievement1 == null) {
            return -1;
        }
        return achievement1.compareTo(achievement2);
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
        final PlayerAchievement rhs = (PlayerAchievement) obj;
        return new EqualsBuilder()
                .append(achievementId, rhs.achievementId)
                .append(title, rhs.title)
                .append(message, rhs.message)
                .append(howToGet, rhs.howToGet)
                .append(level, rhs.level)
                .append(hasAchievement, rhs.hasAchievement)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(achievementId)
                .append(title)
                .append(message)
                .append(howToGet)
                .append(level)
                .append(hasAchievement)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(achievementId)
                .append(title)
                .append(message)
                .append(howToGet)
                .append(level)
                .append(hasAchievement)
                .toString();
    }
}
