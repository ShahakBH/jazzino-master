package com.yazino.web.controller;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.bonus.BonusException;
import com.yazino.platform.bonus.BonusService;
import com.yazino.platform.bonus.BonusStatus;
import com.yazino.platform.community.CommunityService;
import com.yazino.test.ThreadLocalDateTimeUtils;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.WebApiResponses;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsEqual;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static org.mockito.Mockito.*;

public class BonusControllerTest {
    public static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    public static final BigDecimal SESSION_ID = BigDecimal.TEN;
    @Mock
    private WebApiResponses responseWriter;
    @Mock
    private BonusService bonusService;
    @Mock
    private LobbySessionCache lobbySessionCache;

    private BonusController underTest;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    @Mock
    LobbySession session;

    @Mock
    private YazinoConfiguration yazinoConfiguration;
    @Mock
    private CommunityService communityService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(yazinoConfiguration.getBoolean(anyString(), eq(false))).thenReturn(false);
        underTest = new BonusController(responseWriter, communityService, yazinoConfiguration, bonusService, lobbySessionCache);
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(System.currentTimeMillis());

    }

    @After
    public void tearDown() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();

    }

    @Test
    public void disabledLockoutShouldReturnErrorMessage() throws IOException {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(session);
        when(session.getPlayerId()).thenReturn(PLAYER_ID);
        when(yazinoConfiguration.getBoolean(anyString(), eq(false))).thenReturn(true);

        underTest.collectBonus(request,response);
        verifyZeroInteractions(bonusService);
        verify(responseWriter).writeError(response, SC_FORBIDDEN, "Bonus collection disabled");
    }

    @Test
    public void getBonusStatusShouldFailIfNoSession() throws IOException {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(null);
        underTest.getBonusStatus(request, response);
        verify(responseWriter).writeError(response, SC_BAD_REQUEST, "no active session");
    }

    @Test
    public void getBonusStatusShouldFailIfNoPlayerId() throws IOException {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(session);
        when(session.getPlayerId()).thenReturn(null);

        underTest.getBonusStatus(request, response);

        verify(responseWriter).writeError(response, SC_BAD_REQUEST, "no playerId in lobby session");
        verifyZeroInteractions(bonusService);
    }

    @Test
    public void getBonusStatusShouldGetStatusFromService() throws IOException {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(session);
        when(session.getPlayerId()).thenReturn(PLAYER_ID);
        final BonusStatus bonusStatus = new BonusStatus(100000L, 123L);
        when(bonusService.getBonusStatus(PLAYER_ID)).thenReturn(bonusStatus);

        underTest.getBonusStatus(request, response);

        verify(responseWriter).writeOk(response, bonusStatus);

    }


    @Test
    public void collectBonusShouldFailIfNoSession() throws IOException {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(null);

        underTest.collectBonus(request, response);

        verify(responseWriter).writeError(response, SC_BAD_REQUEST, "no active session");
        verifyZeroInteractions(bonusService);

    }

    @Test
    public void collectBonusShouldFailIfNoPlayerId() throws IOException {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(session);
        when(session.getPlayerId()).thenReturn(null);

        underTest.collectBonus(request, response);

        verifyZeroInteractions(bonusService);
        verify(responseWriter).writeError(response, SC_BAD_REQUEST, "no playerId in lobby session");
    }

    @Test
    public void collectBonusShouldFailIfNullBonusStatusReturned() throws BonusException, IOException {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(session);
        when(session.getPlayerId()).thenReturn(PLAYER_ID);
        when(session.getSessionId()).thenReturn(SESSION_ID);
        when(bonusService.collectBonus(PLAYER_ID, SESSION_ID))
                .thenThrow(new BonusException("Lockout has not expired:" + DateTime.now().plusMinutes(5)));

        underTest.collectBonus(request, response);

        verify(responseWriter).writeError(response, HttpServletResponse.SC_FORBIDDEN, "Lockout has not expired:" + DateTime.now().plusMinutes(5));
    }

    @Test
    public void collectBonusShouldReturnSuccessfulResultFromService() throws IOException, BonusException {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(session);
        when(session.getPlayerId()).thenReturn(PLAYER_ID);
        when(session.getSessionId()).thenReturn(SESSION_ID);
        final BonusStatus bonusStatus = new BonusStatus(1000000L, 1000l);
        when(bonusService.collectBonus(PLAYER_ID, SESSION_ID)).thenReturn(bonusStatus);

        underTest.collectBonus(request, response);

        verify(responseWriter).writeOk(response, bonusStatus);
    }
}
