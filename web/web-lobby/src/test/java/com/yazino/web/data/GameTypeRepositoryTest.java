package com.yazino.web.data;

import com.google.common.collect.Sets;
import com.yazino.platform.table.GameTypeInformation;
import com.yazino.platform.table.TableService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.yazino.game.api.GameType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GameTypeRepositoryTest {

    @Mock
    private TableService tableService;

    private GameTypeRepository underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        underTest = new GameTypeRepository(tableService);
    }

    @Test(expected = NullPointerException.class)
    public void aNullTableServiceCausesANullPointerException() {
        new GameTypeRepository(null);
    }

    @Test
    public void gameTypesAreFetchedFromTheTableService() {
        when(tableService.getGameTypes()).thenReturn(availableGameTypes());

        final Map<String, GameTypeInformation> gameTypes = underTest.getGameTypes();

        verify(tableService).getGameTypes();
        assertThat(gameTypes, is(equalTo(mappedGameTypes())));
    }

    @Test
    public void gameTypesIsAnEmptyMapWhenTheTableServiceReturnsNull() {
        when(tableService.getGameTypes()).thenReturn(null);

        final Map<String, GameTypeInformation>  gameTypes = underTest.getGameTypes();

        verify(tableService).getGameTypes();
        assertThat(gameTypes.size(), is(equalTo(0)));
    }

    private Set<GameTypeInformation> availableGameTypes() {
        return Sets.newHashSet(new GameTypeInformation(aGameTypeCalled("TEST1"), true), new GameTypeInformation(aGameTypeCalled("TEST2"), false));
    }

    private Map<String, GameTypeInformation> mappedGameTypes() {
        final Map<String, GameTypeInformation> typeMap = new HashMap<String, GameTypeInformation>();
        typeMap.put("TEST1", new GameTypeInformation(aGameTypeCalled("TEST1"), true));
        typeMap.put("TEST2", new GameTypeInformation(aGameTypeCalled("TEST2"), false));
        return typeMap;
    }

    private GameType aGameTypeCalled(final String gameType) {
        return new GameType(gameType, gameType.toLowerCase(), Collections.<String>emptySet());
    }

}
