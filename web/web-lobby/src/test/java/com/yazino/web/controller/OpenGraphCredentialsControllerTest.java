package com.yazino.web.controller;

import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.bi.opengraph.OpenGraphCredentialsMessage;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.mockito.Mockito.*;

public class OpenGraphCredentialsControllerTest {

    private static final String GAME_TYPE = "SLOTS";
    private static final String ACCESS_TOKEN = "access_token";
    private static final BigInteger PLAYER_ID = BigInteger.TEN;

    private OpenGraphCredentialsController underTest;
    private QueuePublishingService<OpenGraphCredentialsMessage> openGraphCredentialsService = mock(QueuePublishingService.class);
    private HttpServletRequest request = mock(HttpServletRequest.class);
    private HttpServletResponse response = mock(HttpServletResponse.class);
    private PrintWriter writer = mock(PrintWriter.class);
    private HttpSession session = mock(HttpSession.class);

    @Before
    public void setUp() throws IOException {
        underTest = new OpenGraphCredentialsController(openGraphCredentialsService);
        when(response.getWriter()).thenReturn(writer);
        when(request.getSession()).thenReturn(session);
    }

    @Test
    public void shouldImmediatelyReturn400WhenPlayerIdIsMissing() throws IOException {
        underTest.updateCredentials(request, response, null, GAME_TYPE, ACCESS_TOKEN);
        verify(response).sendError(SC_BAD_REQUEST);
        verify(openGraphCredentialsService, never()).send(any(OpenGraphCredentialsMessage.class));
    }

    @Test
    public void shouldImmediatelyReturn400WhenGameTypeIsMissing() throws IOException {
        underTest.updateCredentials(request, response, PLAYER_ID, null, ACCESS_TOKEN);
        verify(response).sendError(SC_BAD_REQUEST);
        verify(openGraphCredentialsService, never()).send(any(OpenGraphCredentialsMessage.class));
    }

    @Test
    public void shouldImmediatelyReturn400WhenAccessTokenIsMissing() throws IOException {
        underTest.updateCredentials(request, response, PLAYER_ID, GAME_TYPE, null);
        verify(response).sendError(SC_BAD_REQUEST);
        verify(openGraphCredentialsService, never()).send(any(OpenGraphCredentialsMessage.class));
        System.out.println(MediaType.TEXT_HTML.getType());
        System.out.println(MediaType.TEXT_HTML.toString());
    }

    @Test
    public void shouldEnqueueToOpenGraphCredentials() throws IOException {
        underTest.updateCredentials(request, response, PLAYER_ID, GAME_TYPE, ACCESS_TOKEN);
        verify(openGraphCredentialsService).send(new OpenGraphCredentialsMessage(PLAYER_ID, GAME_TYPE, ACCESS_TOKEN));
        // TODO above verify should explicitly check access token as this is not part of equals
    }
}
