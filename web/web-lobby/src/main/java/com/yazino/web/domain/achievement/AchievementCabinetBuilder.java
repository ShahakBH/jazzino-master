package com.yazino.web.domain.achievement;

import com.yazino.platform.playerstatistic.service.AchievementDetails;
import com.yazino.web.domain.PlayerAchievement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import com.yazino.game.api.ParameterisedMessage;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

import static org.apache.commons.lang3.Validate.notNull;

@Service("achievementCabinetBuilder")
public class AchievementCabinetBuilder {

    private static final int NUMBER_OF_LEVELS = 3;

    private final AchievementDetailsRepository achievementDetailsRepository;
    private final PlayerAchievementRepository playerAchievementRepository;

    @Autowired
    public AchievementCabinetBuilder(
            @Qualifier("achievementDetailsRepository") final AchievementDetailsRepository achievementDetailsRepository,
            @Qualifier("playerAchievementRepository") final PlayerAchievementRepository playerAchievementRepository) {
        notNull(achievementDetailsRepository, "achievementDetailsRepository is null");
        notNull(playerAchievementRepository, "playerAchievementRepository is null");
        this.achievementDetailsRepository = achievementDetailsRepository;
        this.playerAchievementRepository = playerAchievementRepository;
    }

    public AchievementCabinet buildAchievementCabinet(final BigDecimal playerId,
                                                      final String playerName,
                                                      final String gameType) {

        final Set<AchievementDetails> availableAchievements
                = achievementDetailsRepository.getAchievementDetails(gameType);
        final Set<String> playerAchievements = playerAchievementRepository.getPlayerAchievements(playerId);
        final List<PlayerAchievement> playerAchievementDetails = new ArrayList<PlayerAchievement>();
        for (final AchievementDetails achievement : availableAchievements) {
            final boolean hasAchievement = playerAchievements.contains(achievement.getAchievementId());
            final PlayerAchievement playerAchievement = buildPlayerAchievement("${name}", achievement, hasAchievement);
            playerAchievementDetails.add(playerAchievement);
        }

        final int[] numberOfAchievementsPerLevel = new int[NUMBER_OF_LEVELS];
        final int[] numberOfTotalAchievementsPerLevel = new int[NUMBER_OF_LEVELS];
        final List<SortedSet<PlayerAchievement>> playerAchievementsForLevel
                = new ArrayList<SortedSet<PlayerAchievement>>(NUMBER_OF_LEVELS);

        for (int i = 0; i < NUMBER_OF_LEVELS; i++) {
            playerAchievementsForLevel.add(i, new TreeSet<PlayerAchievement>());
        }

        for (PlayerAchievement achievement : playerAchievementDetails) {
            if (achievement.getLevel() > NUMBER_OF_LEVELS) {
                continue;
            }

            final int levelIndex = achievement.getLevel() - 1;
            numberOfTotalAchievementsPerLevel[levelIndex]++;
            if (achievement.isHasAchievement()) {
                numberOfAchievementsPerLevel[levelIndex]++;
            }

            playerAchievementsForLevel.get(levelIndex).add(achievement);
        }

        return new AchievementCabinet(numberOfAchievementsPerLevel,
                numberOfTotalAchievementsPerLevel, playerAchievementsForLevel);
    }

    private PlayerAchievement buildPlayerAchievement(final String playerName,
                                                     final AchievementDetails achievement,
                                                     final boolean hasAchievements) {
        PlayerAchievement playerAchievement;
        try {
            playerAchievement = new PlayerAchievement(achievement.getAchievementId(),
                    achievement.getTitle(),
                    achievement.getLevel(),
                    new ParameterisedMessage(achievement.getMessage(), playerName).toString(),
                    achievement.getHowToGet(), hasAchievements);

        } catch (Exception e) {
            // temporary mesaure while they rewrite the achievement text
            playerAchievement = new PlayerAchievement(achievement.getAchievementId(),
                    achievement.getTitle(),
                    achievement.getLevel(),
                    achievement.getMessage(),
                    achievement.getHowToGet(),
                    hasAchievements);
        }
        return playerAchievement;
    }
}
