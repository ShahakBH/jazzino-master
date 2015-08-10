package com.yazino.web.domain.achievement;

import com.yazino.web.domain.PlayerAchievement;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;
import java.util.SortedSet;

public class AchievementCabinet {
    private int[] numberOfAchievements;
    private int[] numberOfTotalAchievements;
    private List<SortedSet<PlayerAchievement>> playerAchievements;

    public AchievementCabinet(final int[] numberOfAchievements,
                              final int[] numberOfTotalAchievements,
                              final List<SortedSet<PlayerAchievement>> playerAchievements) {

        this.numberOfAchievements = numberOfAchievements;
        this.numberOfTotalAchievements = numberOfTotalAchievements;
        this.playerAchievements = playerAchievements;
    }

    /**
     * @param i The level (starting from 1) of which we want the number of achievements for
     * @return The total number of achievements for that level
     */
    public int getNumberOfAchievements(final int i) {
        return this.numberOfAchievements[i - 1];
    }

    /**
     * @param i The level (starting from 1) of which we want the number of <b>total</b> achievements for
     * @return the number of achievements.
     */
    public int getNumberOfTotalAchievements(final int i) {
        return this.numberOfTotalAchievements[i - 1];
    }

    /**
     * @return A two-dimensional List of Lists, where each List contains the PlayerAchievements for each level
     */
    public List<SortedSet<PlayerAchievement>> getPlayerAchievements() {
        return playerAchievements;
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
        final AchievementCabinet rhs = (AchievementCabinet) obj;
        return new EqualsBuilder()
                .append(numberOfAchievements, rhs.numberOfAchievements)
                .append(numberOfTotalAchievements, rhs.numberOfTotalAchievements)
                .append(playerAchievements, rhs.playerAchievements)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(numberOfAchievements)
                .append(numberOfTotalAchievements)
                .append(playerAchievements)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(numberOfAchievements)
                .append(numberOfTotalAchievements)
                .append(playerAchievements)
                .toString();
    }

}
