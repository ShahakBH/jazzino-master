package com.yazino.web.controller.gameserver;

import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.chat.ChatService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigDecimal;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class ChatControllerTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(100);

    @Mock
    private ChatService chatService;
    @Mock
    private LobbySessionCache lobbySessionCache;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private ChatController underTest;

    private StringWriter responseWriter;

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);

        underTest = new ChatController(lobbySessionCache, chatService);

        when(request.getMethod()).thenReturn("POST");
        when(lobbySessionCache.getActiveSession(request)).thenReturn(aPlayerSession());

        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    @Test
    public void theControllerOnlyAcceptsPostRequests() throws IOException {
        reset(request);
        when(request.getMethod()).thenReturn("GET");

        underTest.handleChat(request, response);

        verify(response).sendError(200, "ERROR|Only POST accepted");
    }

    @Test
    public void aValidSessionMustBePresentForTheCurrentPlayer() throws IOException {
        reset(lobbySessionCache);
        when(lobbySessionCache.getActiveSession(request)).thenReturn(null);

        underTest.handleChat(request, response);

        verify(response).sendError(200, "ERROR|Session Expired");
    }

    @Test
    public void anIOExceptionCausesMessageParsingToFail() throws IOException {
        when(request.getReader()).thenThrow(new IOException("anIOException"));

        underTest.handleChat(request, response);

        verify(response).sendError(200, "ERROR|Unable to parse message");
    }

    @Test
    public void aRequestWithoutPipesIsPassedToTheChatServiceAsASingleElement() throws IOException {
        requestContentsAre("aRequest");

        underTest.handleChat(request, response);

        verify(chatService).asyncProcessCommand(PLAYER_ID, "aRequest");
    }

    @Test
    public void aRequestWithPipesIsPassedToTheChatServiceHavingBeenSplit() throws IOException {
        requestContentsAre("aRequest|with|multiple|parts");

        underTest.handleChat(request, response);

        verify(chatService).asyncProcessCommand(PLAYER_ID, "aRequest", "with", "multiple", "parts");
    }

    @Test
    public void aSuccessfulRequestReturnsAnOkayMessage() throws IOException {
        requestContentsAre("aRequest");

        underTest.handleChat(request, response);

        assertThat(responseWriter.getBuffer().toString(), is(equalTo("OK|Command sent")));
    }

    @Test
    public void anExceptionFromTheChatServiceIsReturnedAsARejection() throws IOException {
        requestContentsAre("aRequest");
        doThrow(new RuntimeException("anExceptionFromTheChatService"))
                .when(chatService).asyncProcessCommand(PLAYER_ID, "aRequest");

        underTest.handleChat(request, response);

        verify(response).sendError(200, "ERROR|Command rejected");
    }

    @Test
    public void anEmptyRequestReturnsParseFailed() throws IOException {
        requestContentsAre(null);

        underTest.handleChat(request, response);

        verify(response).sendError(200, "ERROR|Unable to parse message");
    }

    @Test
    public void anIllegalArgumentExceptionFromTheChatServiceIsReturnedAsARejection() throws IOException {
        requestContentsAre("aRequest");
        doThrow(new IllegalArgumentException("anInvalidMessageExceptionFromTheChatService"))
                .when(chatService).asyncProcessCommand(PLAYER_ID, "aRequest");

        underTest.handleChat(request, response);

        verify(response).sendError(200, "ERROR|Command rejected");
    }

    private void requestContentsAre(final String content) throws IOException {
        final BufferedReader contentReader;
        if (content != null) {
            contentReader = new BufferedReader(new StringReader(content));
        } else {
            contentReader = mock(BufferedReader.class);
            when(contentReader.readLine()).thenReturn(null);
        }
        when(request.getReader()).thenReturn(contentReader);
    }

    private LobbySession aPlayerSession() {
        return new LobbySession(BigDecimal.valueOf(3141592), PLAYER_ID, "aPlayerName", "aSessionKey", Partner.YAZINO,
                "aPictureUrl", "anEmailAddress", null, false, Platform.WEB, AuthProvider.YAZINO);
    }

}
