package com.yazino.web.controller;

import com.yazino.platform.AuthProvider;
import com.yazino.platform.Platform;
import com.yazino.platform.session.InboxService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;

import static com.yazino.platform.Partner.YAZINO;
import static org.mockito.Mockito.*;

public class InboxControllerTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(10);

    @Mock
    private LobbySessionCache lobbySessionCache;
    @Mock
    private InboxService inboxService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private InboxController underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(lobbySessionCache.getActiveSession(request)).thenReturn(
                new LobbySession(BigDecimal.valueOf(3141592), PLAYER_ID, "aPlayer", "aSessionKey", YAZINO, "aPicture", "anEmail", null, false, Platform.WEB, AuthProvider.YAZINO));

        underTest = new InboxController(lobbySessionCache, inboxService);
    }

    @Test(expected = NullPointerException.class)
    public void controllerCannotBeCreatedWithANullLobbySessionCache() {
        new InboxController(null, inboxService);
    }

    @Test(expected = NullPointerException.class)
    public void controllerCannotBeCreatedWithANullInboxService() {
        new InboxController(lobbySessionCache, null);
    }

    @Test
    public void aForbiddenStatusIsReturnedIfNoLobbySessionCacheExists() throws IOException {
        reset(lobbySessionCache);
        when(lobbySessionCache.getActiveSession(request)).thenReturn(null);

        underTest.checkNewMessages(request, response);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    public void anOKStatusIsReturnedIfSuccessful() throws IOException {
        underTest.checkNewMessages(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
    }

    @Test
    public void theInboxServiceIsInvokedForTheCurrentPlayerIfSuccessful() throws IOException {
        underTest.checkNewMessages(request, response);

        verify(inboxService).checkNewMessages(PLAYER_ID);
    }

}
