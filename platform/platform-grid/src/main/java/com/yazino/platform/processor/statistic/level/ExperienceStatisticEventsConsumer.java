package com.yazino.platform.processor.statistic.level;

import com.yazino.platform.model.statistic.LevelingSystem;
import com.yazino.platform.model.statistic.PlayerLevel;
import com.yazino.platform.model.statistic.PlayerLevels;
import com.yazino.platform.playerstatistic.StatisticEvent;
import com.yazino.platform.processor.statistic.PlayerStatisticEventConsumer;
import com.yazino.platform.repository.statistic.LevelingSystemRepository;
import com.yazino.platform.repository.statistic.PlayerLevelsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;

import static org.apache.commons.lang3.Validate.notNull;

@Component("experienceStatisticEventsConsumer")
@Qualifier("playerStatisticEventConsumers")
public class ExperienceStatisticEventsConsumer implements PlayerStatisticEventConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(ExperienceStatisticEventsConsumer.class);

    private final LevelingSystemRepository levelingSystemRepository;
    private final PlayerLevelsRepository playerLevelsRepository;
    private final PlayerXPPublisher playerXPPublisher;
    private final PlayerNewLevelPublisher playerNewLevelPublisher;


    @Autowired
    public ExperienceStatisticEventsConsumer(
            @Qualifier("levelingSystemRepository") final LevelingSystemRepository levelingSystemRepository,
            final PlayerLevelsRepository playerLevelsRepository,
            @Qualifier("playerXPPublisher") final PlayerXPPublisher playerXPPublisher,
            @Qualifier("playerNewLevelPublisher") final PlayerNewLevelPublisher playerNewLevelPublisher) {
        notNull(levelingSystemRepository, "levelingSystemRepository is null");
        notNull(playerLevelsRepository, "playerLevelsRepository is null");
        notNull(playerXPPublisher, "playerXPPublisher is null");
        notNull(playerNewLevelPublisher, "playerLevelChangePublisher is null");
        this.levelingSystemRepository = levelingSystemRepository;
        this.playerLevelsRepository = playerLevelsRepository;
        this.playerXPPublisher = playerXPPublisher;
        this.playerNewLevelPublisher = playerNewLevelPublisher;
    }

    @Override
    public void processEvents(final BigDecimal playerId,
                              final String gameType,
                              final Collection<StatisticEvent> events) {
        final LevelingSystem levelSystem = levelingSystemRepository.findByGameType(gameType);
        if (levelSystem == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Game Type %s has no leveling system defined. Ignoring...", gameType));
            }
            return;
        }
        processExperienceEvents(playerId, gameType, events, levelSystem);
    }

    private void processExperienceEvents(final BigDecimal playerId,
                                         final String gameType,
                                         final Collection<StatisticEvent> events,
                                         final LevelingSystem levelSystem) {
        final PlayerLevels levels = playerLevelsRepository.forPlayer(playerId);
        final PlayerLevel currentPlayerLevel = levels.retrievePlayerLevel(gameType);
        final PlayerLevel updatedPlayerLevel = levelSystem.calculateNewPlayerLevel(currentPlayerLevel, events);
        if (updatedPlayerLevel.equals(currentPlayerLevel)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Experience for player %s has not changed. Ignoring...", playerId));
            }
            return;
        }
        updateAndPublish(levels, gameType, currentPlayerLevel, updatedPlayerLevel, levelSystem);
    }

    private void updateAndPublish(final PlayerLevels levels,
                                  final String gameType,
                                  final PlayerLevel currentPlayerLevel,
                                  final PlayerLevel updatedPlayerLevel,
                                  final LevelingSystem levelSystem) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Player %s - Updated experience is %s", levels.getPlayerId(), updatedPlayerLevel));
        }
        levels.updateLevel(gameType, updatedPlayerLevel);
        playerLevelsRepository.save(levels);
        if (currentPlayerLevel != null && updatedPlayerLevel.getLevel() > currentPlayerLevel.getLevel()) {
            publishNewLevel(levels, gameType, updatedPlayerLevel, levelSystem);
        }
        playerXPPublisher.publish(levels.getPlayerId(), gameType, updatedPlayerLevel, levelSystem);
    }

    private void publishNewLevel(final PlayerLevels levels,
                                 final String gameType,
                                 final PlayerLevel updatedPlayerLevel,
                                 final LevelingSystem levelSystem) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Player %s - New Level! %s", levels.getPlayerId(), updatedPlayerLevel));
        }
        final BigDecimal bonusChips = levelSystem.retrieveChipAmount(updatedPlayerLevel.getLevel());
        playerNewLevelPublisher.publishNewLevel(levels,
                gameType,
                bonusChips);
    }

}
