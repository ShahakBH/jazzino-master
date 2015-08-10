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
import java.util.*;

import static org.apache.commons.lang3.Validate.notNull;

@SpaceClass
public class PlayerAchievements implements Serializable {
    private static final long serialVersionUID = -5731133738178059915L;

    private BigDecimal playerId;
    private Set<String> achievements;
    private Map<String, String> achievementProgress;

    public PlayerAchievements() {
    }

    public PlayerAchievements(final BigDecimal playerId) {
        notNull(playerId, "playerId is null");

        this.playerId = playerId;
    }

    public PlayerAchievements(final BigDecimal playerId,
                              final Set<String> achievements,
                              final Map<String, String> achievementProgress) {
        notNull(playerId, "playerId is null");
        notNull(achievements, "achievements is null");
        notNull(achievementProgress, "achievementProgress is null");

        this.playerId = playerId;
        this.achievements = new HashSet<String>(achievements);
        this.achievementProgress = new HashMap<String, String>(achievementProgress);
    }

    @SpaceId
    @SpaceRouting
    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public Set<String> getAchievements() {
        return achievements;
    }

    public void setAchievements(final Set<String> achievements) {
        this.achievements = achievements;
    }

    public Map<String, String> getAchievementProgress() {
        return achievementProgress;
    }

    public void setAchievementProgress(final Map<String, String> achievementProgress) {
        this.achievementProgress = achievementProgress;
    }

    public int countAchievements(final Collection<Achievement> allAchievements) {
        int count = 0;
        for (Achievement definition : allAchievements) {
            if (achievements.contains(definition.getId())) {
                count++;
            }
        }
        return count;
    }

    public void awardAchievement(final String id) {
        achievements.add(id);
    }

    public boolean hasAchievement(final String id) {
        return achievements.contains(id);
    }

    public String progressForAchievement(final String id) {
        if (!achievementProgress.containsKey(id)) {
            return "";
        }
        return achievementProgress.get(id);
    }

    public void updateProgressForAchievement(final String id, final String progress) {
        achievementProgress.put(id, progress);
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
        final PlayerAchievements rhs = (PlayerAchievements) obj;
        return new EqualsBuilder()
                .append(achievements, rhs.achievements)
                .append(achievementProgress, rhs.achievementProgress)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(achievements)
                .append(achievementProgress)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .append(achievements)
                .append(achievementProgress)
                .toString();
    }

}
