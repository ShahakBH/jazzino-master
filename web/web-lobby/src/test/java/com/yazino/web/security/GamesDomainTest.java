package com.yazino.web.security;

import com.yazino.platform.table.GameTypeInformation;
import com.yazino.web.data.GameTypeRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.yazino.game.api.GameType;

import java.util.*;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class GamesDomainTest {

    private Domain underTest;
    @Mock
    private GameTypeRepository gameTypeRepository;
    private List<String> gameIds;
    private List<String> gameNames;
    private List<Set<String>> pseudonyms;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        underTest = new GamesDomain(gameTypeRepository);
        final HashMap<String, GameTypeInformation> gameTypeInformationHashMap = new HashMap<String, GameTypeInformation>();
        gameIds = Arrays.asList("DARREN_GAME", "RICHARD_GAME", "Three");
        gameNames = Arrays.asList("darrenGame", "richardGame", "threeOneFive");
        pseudonyms = Arrays.asList((Set<String>) newHashSet("cool", "awsome", "rad", "sweet"),
                newHashSet("dathird", "Gear", "Branson", "Rich"),
                new HashSet<String>());
        gameTypeInformationHashMap.put(gameIds.get(0), createGameTypeInformation(0));
        gameTypeInformationHashMap.put(gameIds.get(1), createGameTypeInformation(1));
        gameTypeInformationHashMap.put(gameIds.get(2), createGameTypeInformation(2));

        when(gameTypeRepository.getGameTypes()).thenReturn(gameTypeInformationHashMap);
    }

    @Test
    public void ShouldReturnTrueIfIncludesUrlIsInGameTypeList() {

        assertTrue(underTest.includesUrl("/awsome"));
        assertTrue(underTest.includesUrl("/sweet"));
        assertTrue(underTest.includesUrl("/rad"));
        assertTrue(underTest.includesUrl("/dathird"));
        assertTrue(underTest.includesUrl("/Gear"));
        assertTrue(underTest.includesUrl("/Branson"));
        assertTrue(underTest.includesUrl("/Rich"));
    }

    @Test
    public void ShouldReturnFalseIfUrlIsNotInGameTypeList() {
        assertFalse(underTest.includesUrl("/terrible"));
        assertFalse(underTest.includesUrl("/fail"));
        assertFalse(underTest.includesUrl("/JamesGame"));
    }

    @Test
    public void ShouldReturnTrueIfGameTypeIdMatches()   {
        assertTrue(underTest.includesUrl("/Three"));
    }

    private GameTypeInformation createGameTypeInformation(final int index) {
        return new GameTypeInformation(new GameType(gameIds.get(index), gameNames.get(index), pseudonyms.get(index)), true);
    }
}
