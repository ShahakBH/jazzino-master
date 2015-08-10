package com.yazino.web.controller.gameserver;

import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.tournament.TournamentService;
import com.yazino.test.ThreadLocalDateTimeUtils;
import com.yazino.web.util.WebApiResponses;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.domain.LaunchConfiguration;
import com.yazino.web.domain.TournamentRequestType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigDecimal;

import static com.yazino.platform.Platform.WEB;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class TournamentControllerTest {
    private BigDecimal TOURNAMENT_ID = BigDecimal.valueOf(17L);
    private BigDecimal PLAYER_ID = BigDecimal.valueOf(19L);

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private TournamentService tournamentService;
    @Mock
    private PlayerService playerService;
    @Mock
    private WebApiResponses webApiResponses;

    private TournamentController underTest;

    private ByteArrayOutputStream responseOut;
    private PrintWriter responseWriter;

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(10000L);

        responseOut = new ByteArrayOutputStream();
        responseWriter = new PrintWriter(responseOut);
        when(response.getWriter()).thenReturn(responseWriter);

        final LobbySessionCache lobbySessionCache = mock(LobbySessionCache.class);
        final LobbySession lobbySession = new LobbySession(BigDecimal.valueOf(3141592), PLAYER_ID, "Dummy", "0001", Partner.YAZINO, null, "email",
                null, false, WEB, AuthProvider.YAZINO);
        when(lobbySessionCache.getActiveSession(request)).thenReturn(lobbySession);

        final LaunchConfiguration launchConfiguration = new LaunchConfiguration("aHost", "aVirtualHost", "5672",
                "aContentUrl", "aClienturl",
                "aCommandBaseUrl",
                Partner.YAZINO, "aPermanentContentUrl");

        underTest = new TournamentController(launchConfiguration, lobbySessionCache, playerService,
                tournamentService, webApiResponses);
    }

    @After
    public void resetJodaTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void registrationShouldCallService() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        createRequest(TournamentRequestType.TOURNAMENT_PLAYER_REGISTER, TOURNAMENT_ID);

        when(tournamentService.register(TOURNAMENT_ID, PLAYER_ID, true)).thenReturn(null);

        underTest.tournamentCommand(request, response);

        responseWriter.flush();
        assertEquals("OK|Command sent", responseOut.toString());

        verify(tournamentService).register(TOURNAMENT_ID, PLAYER_ID, true);
    }

    @Test
    public void deregistrationShouldCallService() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        createRequest(TournamentRequestType.TOURNAMENT_PLAYER_UNREGISTER, TOURNAMENT_ID);

        when(tournamentService.deregister(TOURNAMENT_ID, PLAYER_ID, true)).thenReturn(null);

        underTest.tournamentCommand(request, response);

        responseWriter.flush();
        assertEquals("OK|Command sent", responseOut.toString());

        verify(tournamentService).deregister(TOURNAMENT_ID, PLAYER_ID, true);
    }

    @Test
    public void shouldRejectInvalidCommand() throws Exception {
        when(request.getMethod()).thenReturn("POST");

        final ByteArrayInputStream requestIn = new ByteArrayInputStream("bob|17".getBytes());
        when(request.getReader()).thenReturn(new BufferedReader(new InputStreamReader(requestIn)));

        underTest.tournamentCommand(request, response);

        verify(response).sendError(200, "ERROR|Command rejected");
    }

    private void createRequest(final TournamentRequestType type,
                               final BigDecimal tournamentId)
            throws IOException {
        final String requestValue = type + "|" + tournamentId;
        final ByteArrayInputStream requestIn = new ByteArrayInputStream(requestValue.getBytes());
        when(request.getReader()).thenReturn(new BufferedReader(new InputStreamReader(requestIn)));
    }

}
