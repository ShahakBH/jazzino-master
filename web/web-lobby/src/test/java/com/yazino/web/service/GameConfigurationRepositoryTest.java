package com.yazino.web.service;

import com.yazino.platform.table.GameConfiguration;
import com.yazino.platform.table.TableService;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GameConfigurationRepositoryTest {

    public static final GameConfiguration BLACKJACK = gc("BLACKJACK", "bj", 2);
    public static final GameConfiguration TEXAS_HOLDEM = gc("TEXAS_HOLDEM", "th", 3);
    public static final GameConfiguration SLOTS = gc("SLOTS", "sl", 1, "wheelDeal");

    private TableService tableService;
    private GameConfigurationRepository underTest;
    private Collection<GameConfiguration> gameConfigurations;

    @Before
    public void setUp() throws Exception {
        gameConfigurations = new HashSet<GameConfiguration>(asList(BLACKJACK, TEXAS_HOLDEM, SLOTS));
        tableService = mock(TableService.class);
        underTest = new GameConfigurationRepository(tableService);
    }

    @Test
    public void shouldFindByGameType() {
        when(tableService.getGameConfigurations()).thenReturn(gameConfigurations);
        assertEquals(TEXAS_HOLDEM, underTest.find("TEXAS_HOLDEM"));
        assertEquals(TEXAS_HOLDEM, underTest.find("texas_holdem"));
    }

    @Test
    public void shouldThrowExceptionIfGameConfigurationNotFound() {
        assertNull(underTest.find("SOME_OTHER_GAME_TYPE"));
    }

    @Test
    public void shouldFindByAlias() {
        gameConfigurations.add(SLOTS);
        when(tableService.getGameConfigurations()).thenReturn(gameConfigurations);
        assertEquals(SLOTS, underTest.find("wheelDeal"));
        assertEquals(SLOTS, underTest.find("wHeeLdeal"));
    }

    @Test
    public void shouldFindByShortName() {
        gameConfigurations.add(SLOTS);
        when(tableService.getGameConfigurations()).thenReturn(gameConfigurations);
        assertEquals(SLOTS, underTest.find("sl"));
        assertEquals(SLOTS, underTest.find("SL"));

    }

    @Test
    public void shouldFindAllOrdered() {
        final List<GameConfiguration> expected = new ArrayList<GameConfiguration>();
        expected.add(gc("SLOTS", "sl", 1, "wheelDeal"));
        expected.add(gc("BLACKJACK", "bj", 2));
        expected.add(gc("TEXAS_HOLDEM", "th", 3));
        when(tableService.getGameConfigurations()).thenReturn(gameConfigurations);
        assertEquals(expected, underTest.findAll());
    }

    private static GameConfiguration gc(final String gameType, final String shortName, final int order, final String... aliases) {
        return new GameConfiguration(gameType, shortName, "dn", new HashSet<String>(asList(aliases)), order);
    }
}
