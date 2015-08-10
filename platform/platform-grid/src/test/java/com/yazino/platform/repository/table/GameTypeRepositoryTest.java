package com.yazino.platform.repository.table;

import com.google.common.collect.Sets;
import com.yazino.platform.table.GameTypeInformation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import com.yazino.game.api.GameMetaData;
import com.yazino.game.api.GameMetaDataBuilder;
import com.yazino.game.api.GameMetaDataKey;
import com.yazino.game.api.GameType;

import java.util.Collections;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GameTypeRepositoryTest {

    @Mock
    private GameRepository gameRepository;

    private GameTypeRepository underTest;

    @Before
    public void setUp() {
        underTest = new GameTypeRepository(gameRepository);
    }

    @Test(expected = NullPointerException.class)
    public void aNullTableServiceCausesANullPointerException() {
        new GameTypeRepository(null);
    }

    @Test
    public void aGameTypeIsFetchedFromTheTableService() {
        when(gameRepository.getAvailableGameTypes()).thenReturn(availableGameTypes());

        final GameType gameType = underTest.getGameType("TEST2");

        verify(gameRepository).getAvailableGameTypes();
        assertThat(gameType, is(equalTo(aGameTypeCalled("TEST2"))));
    }

    @Test
    public void gameTypeIsNullWhenTheTableServiceReturnsNull() {
        when(gameRepository.getAvailableGameTypes()).thenReturn(null);

        final GameType gameType = underTest.getGameType("TEST3");

        verify(gameRepository).getAvailableGameTypes();
        assertThat(gameType, is(nullValue()));
    }

    @Test(expected = NullPointerException.class)
    public void aNullGameTypeThrowsANullPointerExceptionWhenFetchingAGameType() {
        underTest.getGameType(null);
    }

    @Test
    public void aGamesMetaDataIsFetchedFromTheTableService() {
        when(gameRepository.getMetaDataFor("TEST1")).thenReturn(metaDataFor("TEST1"));

        final GameMetaData gameMetaData = underTest.getMetaDataFor("TEST1");

        verify(gameRepository).getMetaDataFor("TEST1");
        assertThat(gameMetaData, is(equalTo(metaDataFor("TEST1"))));
    }

    @Test
    public void gameMetaDataIsNullWhenTheTableServiceReturnsNull() {
        final GameMetaData gameMetaData = underTest.getMetaDataFor("TEST3");

        verify(gameRepository).getMetaDataFor("TEST3");
        assertThat(gameMetaData, is(nullValue()));
    }

    @Test(expected = NullPointerException.class)
    public void aNullGameTypeThrowsANullPointerExceptionWhenFetchingGameMetaData() {
        underTest.getMetaDataFor(null);
    }

    private Set<GameTypeInformation> availableGameTypes() {
        return Sets.newHashSet(new GameTypeInformation(aGameTypeCalled("TEST1"), true),
                new GameTypeInformation(aGameTypeCalled("TEST2"), false));
    }

    private GameMetaData metaDataFor(final String gameType) {
        return new GameMetaDataBuilder().with(GameMetaDataKey.TOURNAMENT_RANKING_MESSAGE, gameType).build();
    }

    private GameType aGameTypeCalled(final String gameType) {
        return new GameType(gameType, gameType.toLowerCase(), Collections.<String>emptySet());
    }

}
