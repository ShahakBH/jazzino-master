package com.yazino.web.service;

import com.yazino.platform.playerstatistic.service.AchievementDetails;
import com.yazino.platform.playerstatistic.service.AchievementInfo;
import com.yazino.platform.playerstatistic.service.LevelInfo;
import com.yazino.platform.playerstatistic.service.PlayerStatsService;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("AssertEqualsBetweenInconvertibleTypes")
public class HazelcastPlayerStatsServiceTest {
    public static final RuntimeException EXCEPTION = new RuntimeException();
    public static final RuntimeException INVALID_PLAYER_EXCEPTION = new RuntimeException(new IllegalArgumentException());
    public static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    public static final String GAME_TYPE = "gameType";
    private com.yazino.platform.playerstatistic.service.PlayerStatsService delegate;
    private HazelcastPlayerStatsService underTest;

    @Before
    public void setUp() {
        delegate = mock(PlayerStatsService.class);
        underTest = new HazelcastPlayerStatsService(delegate);
    }

    @Test
    public void shouldReturnLevelInfo() {
        final LevelInfo expected = new LevelInfo(2, 3, 4);
        when(delegate.getLevelInfo(PLAYER_ID, GAME_TYPE)).thenReturn(expected);
        assertEquals(expected, underTest.getLevelInfo(PLAYER_ID, GAME_TYPE));
    }

    @Test
    public void shouldReturnDefaultValueForLevelInfoWhenAnExceptionIsThrown() {
        when(delegate.getLevelInfo(PLAYER_ID, GAME_TYPE)).thenThrow(EXCEPTION);
        assertEquals(new LevelInfo(1, 0, 0), underTest.getLevelInfo(PLAYER_ID, GAME_TYPE));
    }

    @Test
    public void shouldReturnDefaultValueForLevelInfoWhenThePlayerDoesNotExist() {
        when(delegate.getLevelInfo(PLAYER_ID, GAME_TYPE)).thenThrow(INVALID_PLAYER_EXCEPTION);
        assertEquals(new LevelInfo(1, 0, 0), underTest.getLevelInfo(PLAYER_ID, GAME_TYPE));
    }

    @Test
    public void shouldReturnLevel() {
        final int expected = 23;
        when(delegate.getLevel(PLAYER_ID, GAME_TYPE)).thenReturn(expected);
        assertEquals(expected, underTest.getLevel(PLAYER_ID, GAME_TYPE));
    }

    @Test
    public void shouldReturnDefaultValueForLevelWhenAnExceptionIsThrown() {
        when(delegate.getLevel(PLAYER_ID, GAME_TYPE)).thenThrow(EXCEPTION);
        assertEquals(1, underTest.getLevel(PLAYER_ID, GAME_TYPE));
    }

    @Test
    public void shouldReturnDefaultValueForLevelWhenThePlayerDoesNotExist() {
        when(delegate.getLevel(PLAYER_ID, GAME_TYPE)).thenThrow(INVALID_PLAYER_EXCEPTION);
        assertEquals(1, underTest.getLevel(PLAYER_ID, GAME_TYPE));
    }

    @Test
    public void shouldReturnAchievementInfo() {
        final AchievementInfo expected = new AchievementInfo(23, 400);
        when(delegate.getAchievementInfo(PLAYER_ID, GAME_TYPE)).thenReturn(expected);
        assertEquals(expected, underTest.getAchievementInfo(PLAYER_ID, GAME_TYPE));
    }

    @Test
    public void shouldReturnDefaultValueForAchievementInfoWhenAnExceptionIsThrown() {
        when(delegate.getAchievementInfo(PLAYER_ID, GAME_TYPE)).thenThrow(EXCEPTION);
        assertEquals(new AchievementInfo(0, 0), underTest.getAchievementInfo(PLAYER_ID, GAME_TYPE));
    }

    @Test
    public void shouldReturnDefaultValueForAchievementInfoWhenThePlayerDoesNotExist() {
        when(delegate.getAchievementInfo(PLAYER_ID, GAME_TYPE)).thenThrow(INVALID_PLAYER_EXCEPTION);
        assertEquals(new AchievementInfo(0, 0), underTest.getAchievementInfo(PLAYER_ID, GAME_TYPE));
    }

    @Test
    public void shouldReturnPlayerAchievements() {
        final Set<String> expected = new HashSet<String>(Arrays.asList("a", "b", "c"));
        when(delegate.getPlayerAchievements(PLAYER_ID)).thenReturn(expected);
        assertEquals(expected, underTest.getPlayerAchievements(PLAYER_ID));
    }

    @Test
    public void shouldReturnDefaultValueForPlayerAchievementsWhenAnExceptionIsThrown() {
        when(delegate.getPlayerAchievements(PLAYER_ID)).thenThrow(EXCEPTION);
        assertEquals(Collections.emptySet(), underTest.getPlayerAchievements(PLAYER_ID));
    }

    @Test
    public void shouldReturnDefaultValueForPlayerAchievementsWhenThePlayerDoesNotExist() {
        when(delegate.getPlayerAchievements(PLAYER_ID)).thenThrow(INVALID_PLAYER_EXCEPTION);
        assertEquals(Collections.emptySet(), underTest.getPlayerAchievements(PLAYER_ID));
    }

    @Test
    public void shouldReturnAchievements() {
        final List<AchievementDetails> achievements = Arrays.asList(achievement(1), achievement(2));
        final Set<AchievementDetails> expected = new HashSet<AchievementDetails>(achievements);
        when(delegate.getAchievementDetails(GAME_TYPE)).thenReturn(expected);
        assertEquals(expected, underTest.getAchievementDetails(GAME_TYPE));
    }

    @Test
    public void shouldReturnDefaultValueForAchievements() {
        when(delegate.getAchievementDetails(GAME_TYPE)).thenThrow(EXCEPTION);
        assertEquals(Collections.emptySet(), underTest.getAchievementDetails(GAME_TYPE));
    }

    private AchievementDetails achievement(final Integer id) {
        return new AchievementDetails("ac" + id, "t" + id, id, "mess" + id, "htg" + id);
    }
}
