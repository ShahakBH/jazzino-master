package com.yazino.web.api;

import com.yazino.platform.player.service.AuthenticationService;
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
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ExternalFriendControllerTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private LobbySessionCache lobbySessionCache;
    @Mock
    private WebApiResponses webApiResponses;

    private ExternalFriendController underTest;

    @Before
    public void setUp() {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(new LobbySession(null, BigDecimal.ONE, null, null, null, null, null, null, false, null, null));
        underTest = new ExternalFriendController(authenticationService, lobbySessionCache, webApiResponses);
    }

    @Test
    public void shouldSyncBuddies() throws IOException {
        underTest.registerExternalFriends(request, response, "TANGO", "a,b,c");
        verify(authenticationService).syncBuddies(BigDecimal.ONE, "TANGO", new HashSet<String>(Arrays.asList("a", "b", "c")));
        verify(webApiResponses).writeNoContent(response, HttpServletResponse.SC_OK);
    }

    @Test
    public void shouldSyncBuddies_SomeEmptyIds() throws IOException {
        underTest.registerExternalFriends(request, response, "TANGO", "a,,c");
        verify(authenticationService).syncBuddies(BigDecimal.ONE, "TANGO", new HashSet<String>(Arrays.asList("a", "c")));
        verify(webApiResponses).writeNoContent(response, HttpServletResponse.SC_OK);
    }

    @Test
    public void shouldSyncBuddies_IdsWithSpaces() throws IOException {
        underTest.registerExternalFriends(request, response, "TANGO", "a,  b,  c");
        verify(authenticationService).syncBuddies(BigDecimal.ONE, "TANGO", new HashSet<String>(Arrays.asList("a", "b", "c")));
        verify(webApiResponses).writeNoContent(response, HttpServletResponse.SC_OK);
    }

    @Test
    public void shouldNotSyncBuddies_NoSession() throws IOException {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(null);
        underTest.registerExternalFriends(request, response, "TANGO", "a,  b,  c");
        verifyZeroInteractions(authenticationService);
        verify(webApiResponses).writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "no session");
    }

    @Test
    public void shouldNotSyncBuddies_NullExternalIds() throws IOException {
        underTest.registerExternalFriends(request, response, "TANGO", null);
        verifyZeroInteractions(authenticationService);
        verify(webApiResponses).writeError(response, HttpServletResponse.SC_BAD_REQUEST, "empty externalIds");
    }

    @Test
    public void shouldNotSyncBuddies_EmptyExternalIds() throws IOException {
        underTest.registerExternalFriends(request, response, "TANGO", "");
        verifyZeroInteractions(authenticationService);
        verify(webApiResponses).writeError(response, HttpServletResponse.SC_BAD_REQUEST, "empty externalIds");
    }

    @Test
    public void shouldNotSyncBuddies_CollectionOfEmptyExternalIds() throws IOException {
        underTest.registerExternalFriends(request, response, "TANGO", ",,,  , , , ");
        verifyZeroInteractions(authenticationService);
        verify(webApiResponses).writeError(response, HttpServletResponse.SC_BAD_REQUEST, "empty externalIds");
    }
}
