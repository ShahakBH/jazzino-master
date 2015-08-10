package com.yazino.platform.service.statistic;

import com.yazino.platform.model.statistic.*;
import com.yazino.platform.playerstatistic.service.AchievementDetails;
import com.yazino.platform.playerstatistic.service.AchievementInfo;
import com.yazino.platform.playerstatistic.service.LevelInfo;
import com.yazino.platform.playerstatistic.service.PlayerStatsService;
import com.yazino.platform.processor.statistic.level.PlayerXPPublisher;
import com.yazino.platform.repository.statistic.AchievementRepository;
import com.yazino.platform.repository.statistic.LevelingSystemRepository;
import com.yazino.platform.repository.statistic.PlayerAchievementsRepository;
import com.yazino.platform.repository.statistic.PlayerLevelsRepository;
import org.openspaces.remoting.RemotingService;
import org.openspaces.remoting.Routing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

@RemotingService
public class GigaspaceRemotingPlayerStatsService implements PlayerStatsService {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceRemotingPlayerStatsService.class);

    private final PlayerLevelsRepository playerLevelsRepository;
    private final LevelingSystemRepository levelingSystemRepository;
    private final AchievementRepository achievementRepository;
    private final PlayerAchievementsRepository playerAchievementsRepository;
    private final PlayerXPPublisher playerXPPublisher;

    @Autowired
    public GigaspaceRemotingPlayerStatsService(final PlayerLevelsRepository playerLevelsRepository,
                                               final LevelingSystemRepository levelingSystemRepository,
                                               final AchievementRepository achievementRepository,
                                               final PlayerAchievementsRepository playerAchievementsRepository,
                                               final PlayerXPPublisher playerXPPublisher) {
        notNull(playerLevelsRepository, "playerLevelsRepository is null");
        notNull(levelingSystemRepository, "levelingSystemRepository is null");
        notNull(achievementRepository, "achievementRepository is null");
        notNull(playerAchievementsRepository, "playerAchievementsRepository is null");
        notNull(playerXPPublisher, "playerXPPublisher is null");

        this.playerLevelsRepository = playerLevelsRepository;
        this.levelingSystemRepository = levelingSystemRepository;
        this.achievementRepository = achievementRepository;
        this.playerAchievementsRepository = playerAchievementsRepository;
        this.playerXPPublisher = playerXPPublisher;
    }

    @Override
    public void publishExperience(@Routing final BigDecimal playerId,
                                  final String gameType) {
        notNull(playerId, "playerId is null");
        notBlank(gameType, "gameType is blank");

        final PlayerLevels playerLevels = playerLevelsRepository.forPlayer(playerId);
        final LevelingSystem levelingSystem = levelingSystemRepository.findByGameType(gameType);
        final PlayerLevel playerLevel = playerLevels.retrievePlayerLevel(gameType);

        LOG.debug("Publishing experience for {}", playerId);
        playerXPPublisher.publish(playerId, gameType, playerLevel, levelingSystem);
    }

    @Override
    public int getLevel(@Routing final BigDecimal playerId,
                        final String gameType) {
        notNull(playerId, "playerId may not be null");
        notNull(gameType, "gameType may not be null");

        return playerLevelsRepository.forPlayer(playerId).retrieveLevel(gameType);
    }

    @Override
    public LevelInfo getLevelInfo(@Routing final BigDecimal playerId,
                                  final String gameType) {
        notNull(playerId, "playerId may not be null");
        notNull(gameType, "gameType may not be null");

        final PlayerLevel level = playerLevelsRepository.forPlayer(playerId).retrievePlayerLevel(gameType);
        final LevelingSystem levelingSystem = levelingSystemRepository.findByGameType(gameType);
        if (levelingSystem == null) {
            return null;
        }

        final int playerLevel = level.getLevel();
        final LevelDefinition definition = levelingSystem.retrieveLevelDefinition(playerLevel);
        if (definition == null) {
            return null;
        }

        final long points = level.getExperience().subtract(definition.getMinimumPoints()).longValue();
        final long nextLevel = definition.getMaximumPoints().subtract(definition.getMinimumPoints()).longValue();
        return new LevelInfo(playerLevel, points, nextLevel);
    }

    @Override
    public AchievementInfo getAchievementInfo(@Routing final BigDecimal playerId,
                                              final String gameType) {
        final Collection<Achievement> achievementsForGameType = achievementRepository.findByGameType(gameType);
        final int playerAchievements = playerAchievementsRepository.forPlayer(playerId)
                .countAchievements(achievementsForGameType);
        final int totalAchievements = achievementsForGameType.size();
        return new AchievementInfo(playerAchievements, totalAchievements);
    }

    @Override
    public Set<String> getPlayerAchievements(@Routing final BigDecimal playerId) {
        notNull(playerId, "playerId may not be null");

        return new HashSet<String>(playerAchievementsRepository.forPlayer(playerId).getAchievements());
    }

    @Override
    public Set<AchievementDetails> getAchievementDetails(final String gameType) {
        notNull(gameType, "gameType may not be null");

        final Set<AchievementDetails> result = new HashSet<AchievementDetails>();
        final Collection<Achievement> achievements = achievementRepository.findByGameType(gameType);
        for (Achievement achievement : achievements) {
            result.add(new AchievementDetails(achievement.getId(), achievement.getTitle(),
                    achievement.getLevel(), achievement.getMessage(), achievement.getHowToGet()));
        }
        return result;
    }
}
