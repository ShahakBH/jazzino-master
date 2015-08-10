package com.yazino.platform.model.statistic;

import com.yazino.platform.playerstatistic.StatisticEvent;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LevelingSystemTest {
    private static final String XP_EVENT = "XP_EVENT";
    private Collection<StatisticEvent> events;
    private LevelingSystem underTest;
    private ExperienceFactor experienceFactor1;
    private BigDecimal minimumPoints = BigDecimal.ZERO;
    private List<LevelDefinition> levelDefinitions = new ArrayList<LevelDefinition>();

    @Before
    public void setUp() throws Exception {
        experienceFactor1 = mock(ExperienceFactor.class);
        events = Arrays.asList(new StatisticEvent(XP_EVENT));
        underTest = new LevelingSystem("gameType", Arrays.asList(experienceFactor1), levelDefinitions);
    }

    @Test
    public void shouldCalculateExperience() {
        levelDefinitions.addAll(Arrays.asList(new LevelDefinition(1, minimumPoints, BigDecimal.valueOf(5), BigDecimal.ZERO)));
        when(experienceFactor1.calculateExperiencePoints(events)).thenReturn(BigDecimal.ONE);
        final PlayerLevel updatedLevel = underTest.calculateNewPlayerLevel(new PlayerLevel(1, BigDecimal.ZERO), events);
        assertEquals(new PlayerLevel(1, BigDecimal.ONE), updatedLevel);
    }

    @Test
    public void shouldReturnSameLevelIfNoExperienceIsGiven() {
        when(experienceFactor1.calculateExperiencePoints(events)).thenReturn(BigDecimal.ZERO);
        PlayerLevel playerLevel = new PlayerLevel(1, BigDecimal.ZERO);
        final PlayerLevel updatedLevel = underTest.calculateNewPlayerLevel(playerLevel, events);
        assertEquals(playerLevel, updatedLevel);
    }

    @Test
    public void shouldCalculateExperienceForNullLevel() {
        levelDefinitions.addAll(Arrays.asList(new LevelDefinition(1, minimumPoints, BigDecimal.valueOf(5), BigDecimal.ZERO)));
        when(experienceFactor1.calculateExperiencePoints(events)).thenReturn(BigDecimal.valueOf(3));
        final PlayerLevel updatedLevel = underTest.calculateNewPlayerLevel(null, events);
        assertEquals(new PlayerLevel(1, BigDecimal.valueOf(3)), updatedLevel);
    }

    @Test
    public void shouldIncreaseLevel() {
        levelDefinitions.addAll(Arrays.asList(new LevelDefinition(1, minimumPoints, BigDecimal.valueOf(5), BigDecimal.ZERO), new LevelDefinition(2, minimumPoints, BigDecimal.valueOf(50), BigDecimal.ZERO)));
        when(experienceFactor1.calculateExperiencePoints(events)).thenReturn(BigDecimal.TEN);
        final PlayerLevel updatedLevel = underTest.calculateNewPlayerLevel(new PlayerLevel(1, BigDecimal.TEN), events);
        assertEquals(new PlayerLevel(2, BigDecimal.valueOf(20)), updatedLevel);
    }

    @Test
    public void shouldLimitExperienceToHighestLevel() {
        levelDefinitions.addAll(Arrays.asList(new LevelDefinition(1, BigDecimal.ZERO, BigDecimal.valueOf(5), BigDecimal.ZERO),
                new LevelDefinition(2, BigDecimal.valueOf(5), BigDecimal.valueOf(50), BigDecimal.ZERO),
                new LevelDefinition(3, BigDecimal.valueOf(50), BigDecimal.valueOf(100), BigDecimal.ZERO)));
        when(experienceFactor1.calculateExperiencePoints(events)).thenReturn(BigDecimal.valueOf(200));
        final PlayerLevel updatedLevel = underTest.calculateNewPlayerLevel(new PlayerLevel(1, BigDecimal.TEN), events);
        assertEquals(new PlayerLevel(3, BigDecimal.valueOf(100)), updatedLevel);
    }

    @Test
    public void shouldRetrieveLevelDefinition() {
        final LevelDefinition level1 = new LevelDefinition(1, minimumPoints, BigDecimal.valueOf(5), BigDecimal.ZERO);
        final LevelDefinition level2 = new LevelDefinition(2, minimumPoints, BigDecimal.valueOf(50), BigDecimal.ZERO);
        final LevelDefinition level3 = new LevelDefinition(3, minimumPoints, BigDecimal.valueOf(100), BigDecimal.ZERO);
        levelDefinitions.addAll(Arrays.asList(level1, level2, level3));
        assertEquals(null, underTest.retrieveLevelDefinition(0));
        assertEquals(level1, underTest.retrieveLevelDefinition(1));
        assertEquals(level2, underTest.retrieveLevelDefinition(2));
        assertEquals(level3, underTest.retrieveLevelDefinition(3));
        assertEquals(null, underTest.retrieveLevelDefinition(4));
    }

    @Test
    public void shouldRetrieveChipsForLevel() {
        final LevelDefinition level1 = new LevelDefinition(1, minimumPoints, BigDecimal.valueOf(5), BigDecimal.TEN);
        levelDefinitions.addAll(Arrays.asList(level1));
        assertEquals(BigDecimal.ZERO, underTest.retrieveChipAmount(0));
        assertEquals(BigDecimal.TEN, underTest.retrieveChipAmount(1));
        assertEquals(BigDecimal.ZERO, underTest.retrieveChipAmount(0));
    }

}
