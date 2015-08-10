package com.yazino.platform.processor.statistic.level;

import com.yazino.platform.model.statistic.LevelingSystem;
import com.yazino.platform.model.statistic.PlayerLevel;
import com.yazino.platform.model.statistic.PlayerLevels;
import com.yazino.platform.playerstatistic.StatisticEvent;
import com.yazino.platform.repository.statistic.LevelingSystemRepository;
import com.yazino.platform.repository.statistic.PlayerLevelsRepository;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import static org.mockito.Mockito.*;

public class ExperienceStatisticEventsConsumerTest {

    public static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    public static final String GAME_TYPE = "aGameType";
    private PlayerLevelsRepository playerLevelsRepository;
    private PlayerXPPublisher playerXPPublisher;
    private NotificationPlayerNewLevelPublisher playerNewLevelPublisher;
    private ExperienceStatisticEventsConsumer underTest;
    private PlayerLevels playerLevels;
    private LevelingSystem levelingSystem;
    private Collection<StatisticEvent> events;

    @Before
    public void setUp() {
        final LevelingSystemRepository levelingSystemRepository = mock(LevelingSystemRepository.class);
        playerLevelsRepository = mock(PlayerLevelsRepository.class);
        playerXPPublisher = mock(PlayerXPPublisher.class);
        playerNewLevelPublisher = mock(NotificationPlayerNewLevelPublisher.class);
        underTest = new ExperienceStatisticEventsConsumer(levelingSystemRepository
                , playerLevelsRepository
                , playerXPPublisher
                , playerNewLevelPublisher);
        playerLevels = new PlayerLevels(PLAYER_ID, new HashMap<String, PlayerLevel>());
        levelingSystem = mock(LevelingSystem.class);
        when(levelingSystemRepository.findByGameType(GAME_TYPE)).thenReturn(levelingSystem);
        when(playerLevelsRepository.forPlayer(PLAYER_ID)).thenReturn(playerLevels);
        events = new HashSet<StatisticEvent>();
    }

    @Test
    public void shouldIgnoreEventsIfGameTypeHasNoLevelingSystem() {
        underTest.processEvents(PLAYER_ID, "unknownGameType", Collections.<StatisticEvent>emptyList());
        verifyZeroInteractions(playerLevelsRepository);
    }

    @Test
    public void shouldProcessEventsDoesNotSaveIfLevelHasNotChanged() {
        final PlayerLevel playerLevel = playerLevels.retrievePlayerLevel(GAME_TYPE);
        when(levelingSystem.calculateNewPlayerLevel(playerLevel, events)).thenReturn(playerLevel);
        underTest.processEvents(PLAYER_ID, GAME_TYPE, events);
    }

    @Test
    public void shouldUpdatePlayerLevelsAndPublishExperience() {
        final PlayerLevel playerLevelBefore = playerLevels.retrievePlayerLevel(GAME_TYPE);
        final PlayerLevel playerLevelAfter = new PlayerLevel(1, BigDecimal.valueOf(2));
        when(levelingSystem.calculateNewPlayerLevel(playerLevelBefore, events)).thenReturn(playerLevelAfter);
        underTest.processEvents(PLAYER_ID, GAME_TYPE, events);
        playerLevels.updateLevel(GAME_TYPE, playerLevelAfter);
        verify(playerLevelsRepository).save(playerLevels);
        verify(playerXPPublisher).publish(PLAYER_ID, GAME_TYPE, playerLevelAfter, levelingSystem);
        verifyZeroInteractions(playerNewLevelPublisher);
    }

    @Test
    public void shouldPublishNewLevel() {
        final BigDecimal bonus = BigDecimal.valueOf(123);
        final PlayerLevel playerLevelBefore = playerLevels.retrievePlayerLevel(GAME_TYPE);
        final PlayerLevel playerLevelAfter = new PlayerLevel(2, BigDecimal.valueOf(2));
        when(levelingSystem.calculateNewPlayerLevel(playerLevelBefore, events)).thenReturn(playerLevelAfter);
        when(levelingSystem.retrieveChipAmount(2)).thenReturn(bonus);
        underTest.processEvents(PLAYER_ID, GAME_TYPE, events);
        playerLevels.updateLevel(GAME_TYPE, playerLevelAfter);
        verify(playerNewLevelPublisher).publishNewLevel(playerLevels, GAME_TYPE, bonus);
    }

}
