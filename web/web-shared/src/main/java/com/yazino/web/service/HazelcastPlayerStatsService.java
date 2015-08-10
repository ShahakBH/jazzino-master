package com.yazino.web.service;

import com.yazino.platform.playerstatistic.service.AchievementDetails;
import com.yazino.platform.playerstatistic.service.AchievementInfo;
import com.yazino.platform.playerstatistic.service.LevelInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
@Service
public class HazelcastPlayerStatsService implements PlayerStatsService {
    private static final Logger LOG = LoggerFactory.getLogger(HazelcastPlayerStatsService.class);
    public static final LevelInfo DEFAULT_LEVEL_INFO = new LevelInfo(1, 0, 0);
    private static final int DEFAULT_LEVEL = 1;
    private static final AchievementInfo DEFAULT_ACHIEVEMENT_INFO = new AchievementInfo(0, 0);
    private static final int MAX_ITERATION_COUNT = 50;

    private final com.yazino.platform.playerstatistic.service.PlayerStatsService playerStatsService;

    @Autowired
    public HazelcastPlayerStatsService(final com.yazino.platform.playerstatistic.service.PlayerStatsService playerStatsService) {
        notNull(playerStatsService, "playerStatsService is null");
        this.playerStatsService = playerStatsService;
    }

    @Override
    public int getLevel(final BigDecimal playerId, final String gameType) {
        try {
            return playerStatsService.getLevel(playerId, gameType);
        } catch (Exception e) {
            if (rootCauseOf(e) instanceof IllegalArgumentException) {
                LOG.info("Received level request for a non-existent player: {}", playerId);
            } else {
                LOG.warn("Error retrieving level for player {}, gameType {}: ", playerId, gameType, e);
            }
            return DEFAULT_LEVEL;
        }
    }

    @Override
    public LevelInfo getLevelInfo(final BigDecimal playerId, final String gameType) {
        try {
            return playerStatsService.getLevelInfo(playerId, gameType);
        } catch (Exception e) {
            if (rootCauseOf(e) instanceof IllegalArgumentException) {
                LOG.info("Received levelInfo request for a non-existent player: {}", playerId);
            } else {
                LOG.warn("Error retrieving levelInfo for player {}, gameType {}: ", playerId, gameType, e);
            }
            return DEFAULT_LEVEL_INFO;
        }
    }

    @Override
    public AchievementInfo getAchievementInfo(final BigDecimal playerId, final String gameType) {
        try {
            return playerStatsService.getAchievementInfo(playerId, gameType);
        } catch (Exception e) {
            LOG.warn("Error retrieving achievementInfo for player {}, gameType {}: ", playerId, gameType, e);
            return DEFAULT_ACHIEVEMENT_INFO;
        }
    }

    @Override
    public Set<String> getPlayerAchievements(final BigDecimal playerId) {
        try {
            return playerStatsService.getPlayerAchievements(playerId);
        } catch (Exception e) {
            if (rootCauseOf(e) instanceof IllegalArgumentException) {
                LOG.info("Received playerAchievements request for a non-existent player: {}", playerId);
            } else {
                LOG.warn("Error retrieving playerAchievements for player {}: ", playerId, e);
            }
            return Collections.emptySet();
        }

    }

    @Override
    public Set<AchievementDetails> getAchievementDetails(final String gameType) {
        try {
            return playerStatsService.getAchievementDetails(gameType);
        } catch (Exception e) {
            LOG.warn("Error retrieving achievementDetails for gameType {}: ", gameType, e);
            return Collections.emptySet();
        }
    }

    private Throwable rootCauseOf(final Throwable t) {
        if (t == null) {
            return t;
        }
        return rootCauseOf(t, 1);
    }

    private Throwable rootCauseOf(final Throwable t, final int iteration) {
        if (t.getCause() == null || t.getCause() == t || iteration > MAX_ITERATION_COUNT) {
            return t;
        }
        return rootCauseOf(t.getCause(), iteration + 1);
    }
}
