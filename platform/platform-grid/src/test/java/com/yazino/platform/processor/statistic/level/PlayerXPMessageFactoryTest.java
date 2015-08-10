package com.yazino.platform.processor.statistic.level;

import com.yazino.platform.model.statistic.LevelDefinition;
import com.yazino.platform.model.statistic.PlayerLevel;
import org.junit.Before;
import org.junit.Test;

import static java.math.BigDecimal.valueOf;
import static org.junit.Assert.assertEquals;

public class PlayerXPMessageFactoryTest {

    public static final String GAME_TYPE = "myGame";
    private PlayerXPMessageFactory underTest;
    private PlayerLevel playerLevel;
    private LevelDefinition definition;

    @Before
    public void setUp() throws Exception {
        underTest = new PlayerXPMessageFactory();
        playerLevel = new PlayerLevel(2, valueOf(23));
        definition = new LevelDefinition(2, valueOf(20), valueOf(25), valueOf(0));
    }

    @Test
    public void shouldBuildMessage() {
        final String json = underTest.create(GAME_TYPE, playerLevel, definition);
        assertEquals("{\"gameType\":\"myGame\",\"level\":2,\"points\":3,\"toNextLevel\":5}", json);
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotBuildMessageForMissingGameType() {
        underTest.create(null, playerLevel, definition);
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotBuildMessageForMissingPlayerLevel() {
        underTest.create(GAME_TYPE, null, definition);
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotBuildMessageForMissingLevelDefinition() {
        underTest.create(GAME_TYPE, playerLevel, null);
    }
}
