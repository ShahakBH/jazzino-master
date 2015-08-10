package com.yazino.platform.repository.table;

import com.yazino.platform.table.GameConfiguration;
import com.yazino.platform.table.GameTypeInformation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import com.yazino.game.api.GameMetaDataBuilder;
import com.yazino.game.api.GameMetaDataKey;
import com.yazino.game.api.GameRules;
import com.yazino.game.api.GameType;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultGameRepositoryTest {

    private static final String GAME_ID_1 = "game1";
    private static final String GAME_ID_2 = "game2";
    private static final String GAME_ID_3 = "game3";
    private static final String GAME_ONE = "Game One";
    private static final String GAME_TWO = "Game Two";
    private static final String GAME_THREE = "Game Three";
    @Mock
    private GameConfigurationRepository gameConfigurationRepository;
    private DefaultGameRepository underTest;
    private GameRules game1;

    @Before
    public void setUp() throws Exception {
        game1 = createGameRule(GAME_ID_1);
        final GameRules game2 = createGameRule(GAME_ID_2);
        final GameRules game3 = createGameRule(GAME_ID_3);

        final GameConfiguration configOne = new GameConfiguration(game1.getGameType(), GAME_ID_1, GAME_ONE, Collections.<String>emptyList(), 0);
        final GameConfiguration configTwo = new GameConfiguration(game2.getGameType(), GAME_ID_2, GAME_TWO, Collections.<String>emptyList(), 1);
        final GameConfiguration configThree = new GameConfiguration(game3.getGameType(), GAME_ID_3, GAME_THREE, Collections.<String>emptyList(), 2);

        when(gameConfigurationRepository.retrieveAll()).thenReturn(Arrays.asList(configOne, configTwo, configThree));
        when(gameConfigurationRepository.findById(GAME_ID_1)).thenReturn(configOne);
        when(gameConfigurationRepository.findById(GAME_ID_2)).thenReturn(configTwo);
        when(gameConfigurationRepository.findById(GAME_ID_3)).thenReturn(configThree);

        underTest = new DefaultGameRepository(gameConfigurationRepository);
        underTest.addGameRules(game1);
        underTest.addGameRules(game2);
        underTest.addGameRules(game3);
    }

    @Test
    public void shouldRetrieveByGameType() {
        assertEquals(game1, underTest.getGameRules(GAME_ID_1));
    }

    @Test
    public void shouldGetMetaDataByGameType() {
        assertEquals(new GameMetaDataBuilder().with(GameMetaDataKey.TOURNAMENT_RANKING_MESSAGE, "game1 ranking").build(),
                underTest.getMetaDataFor(GAME_ID_1));
    }

    @Test
    public void shouldUpdateAvailability() {
        assertTrue(underTest.isGameAvailable("game1"));
        underTest.setGameAvailable("game1", false);
        assertFalse(underTest.isGameAvailable("game1"));
    }

    @Test
    public void shouldGetCurrentGameTypeStatuses() {
        underTest.setGameAvailable(GAME_ID_3, false);
        final Set<GameTypeInformation> expected = new HashSet<GameTypeInformation>(Arrays.asList(
                new GameTypeInformation(new GameType(GAME_ID_1, GAME_ONE, Collections.<String>emptySet()), true),
                new GameTypeInformation(new GameType(GAME_ID_2, GAME_TWO, Collections.<String>emptySet()), true),
                new GameTypeInformation(new GameType(GAME_ID_3, GAME_THREE, Collections.<String>emptySet()), false)
        ));
        assertEquals(expected, underTest.getAvailableGameTypes());
    }

    private GameRules createGameRule(final String gameType) {
        final GameRules result = mock(GameRules.class);
        when(result.getGameType()).thenReturn(gameType);
        when(result.getMetaData()).thenReturn(
                new GameMetaDataBuilder().with(GameMetaDataKey.TOURNAMENT_RANKING_MESSAGE, gameType + " ranking").build());
        return result;
    }
}
