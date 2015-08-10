package com.yazino.platform.model.statistic;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;

public class PlayerAchievementsTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(10);
    private PlayerAchievements underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new PlayerAchievements(PLAYER_ID,
                new HashSet<String>(),
                new HashMap<String, String>());
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotCreateWithoutPlayerId() {
        new PlayerAchievements(null, new HashSet<String>(), new HashMap<String, String>());
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotCreateWithoutAchievements() {
        new PlayerAchievements(PLAYER_ID, null, new HashMap<String, String>());
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotCreateWithoutProgress() {
        new PlayerAchievements(PLAYER_ID, new HashSet<String>(), null);
    }

    @Test
    public void shouldRetrieveProgressForAchievement() {
        underTest.updateProgressForAchievement("achievementId", "abc");
        assertEquals("abc", underTest.progressForAchievement("achievementId"));
    }

    @Test
    public void shouldRetrieveDefaultProgressForAchievement() {
        assertEquals("", underTest.progressForAchievement("achievementId"));
    }
}
