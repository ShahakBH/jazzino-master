package com.yazino.web.controller.social;

import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.community.PlayerService;
import com.yazino.web.domain.social.PlayerInformation;
import com.yazino.web.domain.social.PlayersInformationService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.JsonHelper;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static com.yazino.platform.AuthProvider.YAZINO;
import static com.yazino.platform.Platform.WEB;
import static com.yazino.web.domain.social.PlayerInformationType.*;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.valueOf;
import static org.mockito.Mockito.*;

public class SocialControllerTest {

    private SocialController underTest;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private LobbySessionCache lobbySessionCache;
    private PlayerService playerService;
    private PrintWriter writer;
    private PlayersInformationService playersInformationService;
    private final JsonHelper jsonHelper = new JsonHelper();
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(3141592);
    private static final LobbySession A_SESSION = new LobbySession(SESSION_ID, ONE, "", "", Partner.YAZINO, "", "", null, false, WEB, AuthProvider.YAZINO);

    @Before
    public void setUp() throws Exception {
        request = mock(HttpServletRequest.class);
        playerService = mock(PlayerService.class);
        playersInformationService = mock(PlayersInformationService.class);
        lobbySessionCache = mock(LobbySessionCache.class);
        response = mock(HttpServletResponse.class);
        writer = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(writer);
        underTest = new SocialController(lobbySessionCache, playerService, playersInformationService);
    }

    @Test
    public void shouldPublishSocialSummary() {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(lobbySession(BigDecimal.ONE));
        underTest.publishSummary(request, response);
        verify(playerService).asyncPublishFriendsSummary(BigDecimal.ONE);
        verify(writer).write("{\"result\":\"ok\"}");
    }

    @Test
    public void shouldHandleNoSession(){
        underTest.publishSummary(request, response);
        verifyZeroInteractions(playerService);
        verify(writer).write("{\"result\":\"no session\"}");
    }
    @Test
    public void shouldRefuseRequestIfNoSessionAvailable(){
        underTest.players(request, response);
        verify(writer).write(String.format("{\"result\":\"no session\"}"));
    }

    @Test
    public void shouldRetrieveMinimalInformation() {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(A_SESSION);
        when(request.getParameter("playerIds")).thenReturn("1,2, 3");
        final List<BigDecimal> playerIds = Arrays.asList(valueOf(1), valueOf(2), valueOf(3));
        final List<PlayerInformation> playerInfos = Arrays.asList(info(1), info(2), info(3));
        when(playersInformationService.retrieve(playerIds, null)).thenReturn(playerInfos);
        underTest.players(request, response);
        verify(writer).write(String.format("{\"result\":\"ok\",\"players\":%s}", jsonHelper.serialize(playerInfos)));
    }

    @Test
    public void shouldRefuseInvalidPlayerId() {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(A_SESSION);
        when(request.getParameter("playerIds")).thenReturn("1,2,a");
        underTest.players(request, response);
        verify(writer).write("{\"result\":\"invalid player ids\"}");
    }

    @Test
    public void shouldIgnoreInvalidInformationTypes() {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(A_SESSION);
        when(request.getParameter("playerIds")).thenReturn("1");
        when(request.getParameter("details")).thenReturn("a");
        final List<BigDecimal> playerIds = Arrays.asList(valueOf(1));
        final List<PlayerInformation> playerInfos = Arrays.asList(info(1));
        when(playersInformationService.retrieve(playerIds, null)).thenReturn(playerInfos);
        underTest.players(request, response);
        verify(writer).write(String.format("{\"result\":\"ok\",\"players\":%s}", jsonHelper.serialize(playerInfos)));
    }

    @Test
    public void shouldUseGameTypeFromRequest(){
        when(lobbySessionCache.getActiveSession(request)).thenReturn(A_SESSION);
        when(request.getParameter("playerIds")).thenReturn("1");
        when(request.getParameter("gameType")).thenReturn("aGameType");
        final List<BigDecimal> playerIds = Arrays.asList(valueOf(1));
        final List<PlayerInformation> playerInfos = Arrays.asList(info(1));
        when(playersInformationService.retrieve(playerIds, "aGameType")).thenReturn(playerInfos);
        underTest.players(request, response);
        verify(writer).write(String.format("{\"result\":\"ok\",\"players\":%s}", jsonHelper.serialize(playerInfos)));
    }

    @Test
    public void shouldRetrieveOtherInformation() {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(A_SESSION);
        when(request.getParameter("playerIds")).thenReturn("1,2");
        when(request.getParameter("details")).thenReturn("balance,picture, name");
        final List<BigDecimal> playerIds = Arrays.asList(valueOf(1), valueOf(2));
        final List<PlayerInformation> playerInfos = Arrays.asList(info(1), info(2));
        when(playersInformationService.retrieve(playerIds, null, BALANCE, PICTURE, NAME)).thenReturn(playerInfos);
        underTest.players(request, response);
        verify(writer).write(String.format("{\"result\":\"ok\",\"players\":%s}", jsonHelper.serialize(playerInfos)));
    }

    private PlayerInformation info(int playerId) {
        return new PlayerInformation.Builder(BigDecimal.valueOf(playerId)).build();
    }

    private LobbySession lobbySession(BigDecimal playerId) {
        return new LobbySession(SESSION_ID, playerId, "player " + playerId, "", Partner.YAZINO, "", "", null, false, WEB, YAZINO);
    }
}
