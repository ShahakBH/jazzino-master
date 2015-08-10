package com.yazino.web.service;

import com.google.common.collect.Sets;
import com.yazino.platform.session.SessionService;
import com.yazino.platform.table.TableService;
import com.yazino.test.ThreadLocalDateTimeUtils;
import com.yazino.web.service.CachingLobbyInformationService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.yazino.game.api.GameType;
import com.yazino.platform.table.GameTypeInformation;
import com.yazino.web.domain.LobbyInformation;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class CachingLobbyInformationServiceTest {
    private static final String GAME_TYPE = "aweshum";

    @Mock
    private SessionService sessionService;
    @Mock
    private TableService tableService;

    CachingLobbyInformationService underTest;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        underTest = new CachingLobbyInformationService(sessionService, tableService);
    }

    @Test
    public void shouldGetLobbyInformationIfNoValueInCache() {
        when(sessionService.countSessions(false)).thenReturn(100);
        when(tableService.countTablesWithPlayers(GAME_TYPE)).thenReturn(100);
        when(tableService.getGameTypes()).thenReturn(Sets.newHashSet(gameTypeInfo()));

        LobbyInformation expectedLobbyInformation = new LobbyInformation(GAME_TYPE, 100, 100, true);

        assertEquals(expectedLobbyInformation, underTest.getLobbyInformation(GAME_TYPE));
    }

    @Test
    public void shouldNotGetLobbyInformationWhenCached() {
        when(sessionService.countSessions(false)).thenReturn(100);
        when(tableService.countTablesWithPlayers(GAME_TYPE)).thenReturn(100);
        when(tableService.getGameTypes()).thenReturn(Sets.newHashSet(gameTypeInfo()));
        
        LobbyInformation expectedLobbyInformation = new LobbyInformation(GAME_TYPE, 100, 100, true);

        assertEquals(expectedLobbyInformation, underTest.getLobbyInformation(GAME_TYPE));

        assertEquals(expectedLobbyInformation, underTest.getLobbyInformation(GAME_TYPE));

        verify(sessionService, times(1)).countSessions(false);
        verify(tableService, times(1)).countTablesWithPlayers(GAME_TYPE);
        verify(tableService, times(1)).getGameTypes();
    }


    @Test
    public void shouldGetLobbyInformationWhenCachedCopyIsOld() {
        when(sessionService.countSessions(false))
                .thenReturn(100)
                .thenReturn(200);
        when(tableService.countTablesWithPlayers(GAME_TYPE))
                .thenReturn(100)
                .thenReturn(200);
        when(tableService.getGameTypes()).thenReturn(Sets.newHashSet(gameTypeInfo()));

        ThreadLocalDateTimeUtils.setCurrentMillisFixed(0);

        LobbyInformation expectedLobbyInformation = new LobbyInformation(GAME_TYPE, 100, 100, true);
        assertEquals(expectedLobbyInformation, underTest.getLobbyInformation(GAME_TYPE));

        ThreadLocalDateTimeUtils.setCurrentMillisFixed(30 * 1000 + 1);

        LobbyInformation newExpectedLobbyInformation = new LobbyInformation(GAME_TYPE, 200, 200, true);
        assertEquals(newExpectedLobbyInformation, underTest.getLobbyInformation(GAME_TYPE));

        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void shouldGetLobbyInformationAndCacheNullGameType() {
        when(sessionService.countSessions(false)).thenReturn(100);
        when(tableService.countTablesWithPlayers(null)).thenReturn(100);
        when(tableService.getGameTypes()).thenReturn(Sets.newHashSet(gameTypeInfo()));

        LobbyInformation expectedLobbyInformation = new LobbyInformation(null, 100, 100, false);

        assertEquals(expectedLobbyInformation, underTest.getLobbyInformation(null));
        assertEquals(expectedLobbyInformation, underTest.getLobbyInformation(null));

        verify(tableService, times(1)).countTablesWithPlayers(null);
    }

    private GameTypeInformation gameTypeInfo() {
        return new GameTypeInformation(new GameType(GAME_TYPE, GAME_TYPE, Collections.<String>emptySet()), true);
    }

}
