package com.yazino.web.controller.gameserver;

import com.yazino.platform.community.CommunityService;
import com.yazino.platform.community.RelationshipAction;
import com.yazino.platform.playerstatistic.service.PlayerStatsService;
import com.yazino.web.service.LobbyInformationService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.WebApiResponses;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.math.BigDecimal;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CommunityControllerTest {
    @Mock
    private CommunityService communityService;
    @Mock
    private PlayerStatsService playerStatsService;
    @Mock
    private LobbySessionCache lobbySessionCache;
    @Mock
    private LobbyInformationService lobbyInformationService;
    @Mock
    private WebApiResponses webApiResponses;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private BufferedReader reader;
    @Mock
    private LobbySession lobbySession;
    @Mock
    private PrintWriter writer;

    private CommunityController communityController;

    @Before
    public void setUp() throws Exception {
        communityController = new CommunityController(lobbyInformationService, lobbySessionCache, playerStatsService, communityService, webApiResponses);
    }

    @Test
    public void handleCommunityShouldHandleUnbuddyRequests() throws Exception {
        when(request.getReader()).thenReturn(reader);
        when(response.getWriter()).thenReturn(writer);
        when(lobbySessionCache.getActiveSession(request)).thenReturn(lobbySession);
        when(reader.readLine()).thenReturn("request|123|REMOVE_FRIEND");
        when(lobbySession.getPlayerId()).thenReturn(new BigDecimal(999));

        communityController.handleCommunity(request, response);

        verify(communityService).asyncRequestRelationshipChange(new BigDecimal(999), new BigDecimal(123), RelationshipAction.REMOVE_FRIEND);
        verify(response).setContentType("text/html");
        verify(writer).write("OK");
    }
}
