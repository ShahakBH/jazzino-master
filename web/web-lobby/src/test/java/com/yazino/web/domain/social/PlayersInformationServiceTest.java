package com.yazino.web.domain.social;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.yazino.web.domain.social.PlayerInformationType.NAME;
import static com.yazino.web.domain.social.PlayerInformationType.PICTURE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PlayersInformationServiceTest {

    private Set<PlayerInformationRetriever> retrievers;

    private PlayersInformationService underTest;

    @Before
    public void setUp() throws Exception {
        retrievers = new HashSet<PlayerInformationRetriever>();
    }

    @Test
    public void shouldRetrieveJustPlayerIds() {
        underTest = new PlayersInformationService(retrievers);
        final List<PlayerInformation> info = underTest.retrieve(Arrays.<BigDecimal>asList(BigDecimal.ONE), "aGameType");
        assertEquals(1, info.size());
        assertEquals(BigDecimal.ONE, info.get(0).get("playerId"));
        assertNull(info.get(0).get(PlayerInformationType.NAME.getDisplayName()));
    }

    @Test
    public void shouldRetrievePlayersNames() {
        final PlayerInformationRetriever nameRetriever = mock(PlayerInformationRetriever.class);
        when(nameRetriever.getType()).thenReturn(NAME);
        when(nameRetriever.retrieveInformation(BigDecimal.ONE, "aGameType")).thenReturn("a name");
        retrievers.add(nameRetriever);
        underTest = new PlayersInformationService(retrievers);
        final List<PlayerInformation> result = underTest.retrieve(Arrays.<BigDecimal>asList(BigDecimal.ONE), "aGameType", NAME);
        assertEquals(1, result.size());
        assertEquals("a name", result.get(0).get(NAME.getDisplayName()));
    }

    @Test
    public void shouldRetrieveMultipleInformationForPlayer() {
        final PlayerInformationRetriever nameRetriever = mock(PlayerInformationRetriever.class);
        when(nameRetriever.getType()).thenReturn(NAME);
        when(nameRetriever.retrieveInformation(BigDecimal.ONE, "aGameType")).thenReturn("a name");
        final PlayerInformationRetriever picRetriever = mock(PlayerInformationRetriever.class);
        when(picRetriever.getType()).thenReturn(PlayerInformationType.PICTURE);
        when(picRetriever.retrieveInformation(BigDecimal.ONE, "aGameType")).thenReturn("a pic");
        retrievers.add(nameRetriever);
        retrievers.add(picRetriever);
        underTest = new PlayersInformationService(retrievers);
        final List<PlayerInformation> result = underTest.retrieve(Arrays.<BigDecimal>asList(BigDecimal.ONE), "aGameType", PICTURE, NAME);
        assertEquals(1, result.size());
        assertEquals("a name", result.get(0).get(NAME.getDisplayName()));
        assertEquals("a pic", result.get(0).get(PICTURE.getDisplayName()));
    }
    
    @Test
    public void shouldRetrieveInformationForMultiplePlayers(){
        underTest = new PlayersInformationService(retrievers);
        final List<PlayerInformation> result = underTest.retrieve(Arrays.<BigDecimal>asList(BigDecimal.ONE, BigDecimal.TEN), "aGameType");
        assertEquals(2, result.size());
        assertEquals(BigDecimal.ONE, result.get(0).get("playerId"));
        assertEquals(BigDecimal.TEN, result.get(1).get("playerId"));
    }

}
