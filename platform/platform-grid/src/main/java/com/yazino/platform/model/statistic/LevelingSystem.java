package com.yazino.platform.model.statistic;

import com.yazino.platform.playerstatistic.StatisticEvent;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import static org.apache.commons.lang3.Validate.notEmpty;

public class LevelingSystem implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(LevelingSystem.class);
    private static final long serialVersionUID = -6717331087276318782L;

    private String gameType;
    private Collection<ExperienceFactor> experienceFactors;
    private List<LevelDefinition> levelDefinitions;

    public LevelingSystem(final String gameType,
                          final Collection<ExperienceFactor> experienceFactors,
                          final List<LevelDefinition> levelDefinitions) {
        this.gameType = gameType;
        this.experienceFactors = experienceFactors;
        this.levelDefinitions = levelDefinitions;
    }

    public String getGameType() {
        return gameType;
    }

    public PlayerLevel calculateNewPlayerLevel(final PlayerLevel originalPlayerLevel,
                                               final Collection<StatisticEvent> events) {
        PlayerLevel playerLevel = originalPlayerLevel;
        if (playerLevel == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No previous level defined. Starting from scratch.");
            }
            playerLevel = new PlayerLevel(1, BigDecimal.ZERO);
        }
        BigDecimal points = calculateNewExperience(playerLevel, events);
        if (points.compareTo(playerLevel.getExperience()) == 0) {
            return playerLevel;
        }
        final int level = calculateLevel(points, playerLevel.getLevel());
        final LevelDefinition currentLevel = currentLevel(level);
        if (reachedMaxLevel(points, currentLevel)) {
            points = currentLevel.getMaximumPoints();
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Reached max level. Fixing to %s", points));
            }
        }
        return new PlayerLevel(level, points);
    }

    private boolean reachedMaxLevel(final BigDecimal points,
                                    final LevelDefinition currentLevel) {
        return currentLevel.equals(currentLevel(levelDefinitions.size()))
                && points.compareTo(currentLevel.getMaximumPoints()) > 0;
    }

    private LevelDefinition currentLevel(final int level) {
        return levelDefinitions.get(level - 1);
    }

    private int calculateLevel(final BigDecimal points,
                               final int originalLevel) {
        int level = originalLevel;
        checkInitialisation();
        LevelDefinition currentLevel = currentLevel(level);
        while (points.compareTo(currentLevel.getMaximumPoints()) >= 0 && level < levelDefinitions.size()) {
            level++;
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Advancing to level %s", level));
            }
            currentLevel = currentLevel(level);
        }
        return level;
    }

    private BigDecimal calculateNewExperience(final PlayerLevel playerLevel,
                                              final Collection<StatisticEvent> events) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Calculating experience points"));
        }
        BigDecimal points = BigDecimal.ZERO;
        for (ExperienceFactor experienceFactor : experienceFactors) {
            points = points.add(experienceFactor.calculateExperiencePoints(events));
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Gained xp = %s", points));
        }
        final BigDecimal result = points.add(playerLevel.getExperience());
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Updated xp = %s", result));
        }
        return result;
    }

    private void checkInitialisation() {
        notEmpty(experienceFactors, "experienceFactor is empty");
        notEmpty(levelDefinitions, "levelDefinitions is empty");
    }

    public LevelDefinition retrieveLevelDefinition(final int level) {
        if (level <= 0 || level > levelDefinitions.size()) {
            return null;
        }
        return levelDefinitions.get(level - 1);
    }

    public BigDecimal retrieveChipAmount(final int level) {
        final LevelDefinition levelDef = retrieveLevelDefinition(level);
        if (levelDef == null) {
            return BigDecimal.ZERO;
        }
        return levelDef.getChips();
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
        final LevelingSystem rhs = (LevelingSystem) obj;
        return new EqualsBuilder()
                .append(gameType, rhs.gameType)
                .append(experienceFactors, rhs.experienceFactors)
                .append(levelDefinitions, rhs.levelDefinitions)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(gameType)
                .append(experienceFactors)
                .append(levelDefinitions)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(gameType)
                .append(experienceFactors)
                .append(levelDefinitions)
                .toString();
    }
}
