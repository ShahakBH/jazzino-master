package com.yazino.platform.model.statistic;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class PlayerLevelsTest {

    private PlayerLevels underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new PlayerLevels(BigDecimal.ONE, new HashMap<String, PlayerLevel>());
    }

    @Test(expected = NullPointerException.class)
    public void shuoldNotCreateWithoutPlayerId() {
        new PlayerLevels(null, new HashMap<String, PlayerLevel>());
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotCreateWithoutLevelsMap() {
        new PlayerLevels(BigDecimal.ONE, null);
    }

    @Test
    public void shouldRetrieveLevelForPlayer() {
        final PlayerLevel expectedLevel = new PlayerLevel(23, BigDecimal.valueOf(332131));
        underTest.updateLevel("gameType", expectedLevel);
        assertEquals(expectedLevel, underTest.retrievePlayerLevel("gameType"));
        assertEquals(expectedLevel.getLevel(), underTest.retrieveLevel("gameType"));
    }

    @Test
    public void shouldRetrieveDefaultLevel() {
        assertEquals(PlayerLevel.STARTING_LEVEL, underTest.retrievePlayerLevel("gameType"));
        assertEquals(PlayerLevel.STARTING_LEVEL.getLevel(), underTest.retrieveLevel("gameType"));
    }
}
