package com.yazino.web.api;

import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.web.domain.ApplicationInformation;
import com.yazino.web.domain.GameTypeResolver;
import com.yazino.web.service.InvitationLobbyService;
import com.yazino.web.service.InvitationSendingResult;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.WebApiResponses;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static com.yazino.platform.Platform.WEB;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InvitationByEmailControllerTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    private static final String GAME_TYPE = "gameType";
    private static final ApplicationInformation APP_INFO = new ApplicationInformation(GAME_TYPE, Partner.YAZINO, Platform.AMAZON);
    private static final String SOURCE = "source";
    private static final String USER_IP_ADDRESS = "192.168.1.1";

    @Mock
    private LobbySessionCache lobbySessionCache;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private InvitationLobbyService invitationLobbyService;
    @Mock
    private GameTypeResolver gameTypeResolver;
    @Mock
    private WebApiResponses webApiResponses;

    private InvitationByEmailController underTest;

    @Before
    public void setUp() {
        final LobbySession lobbySession = new LobbySession(BigDecimal.valueOf(3141592), PLAYER_ID, "p", "psk", Partner.YAZINO,
                "pic", "email", null, false, WEB, AuthProvider.YAZINO);

        when(request.getRemoteAddr()).thenReturn(USER_IP_ADDRESS);
        when(lobbySessionCache.getActiveSession(request)).thenReturn(lobbySession);
        when(gameTypeResolver.resolveGameType(request, response)).thenReturn(GAME_TYPE);
        when(gameTypeResolver.appInfoFor(request, response, lobbySession)).thenReturn(APP_INFO);

        underTest = new InvitationByEmailController(lobbySessionCache, invitationLobbyService, gameTypeResolver, webApiResponses);
    }

    @Test
    public void shouldDelegateSendingInvitesToInvitationLobbyService() throws IOException {
        InvitationSendingResult expected = new InvitationSendingResult(2, new HashSet<InvitationSendingResult.Rejection>());
        when(invitationLobbyService.sendInvitations(PLAYER_ID, APP_INFO, SOURCE, "", new String[]{"a@b.com", "c@d.com"}, false, USER_IP_ADDRESS))
                .thenReturn(expected);

        underTest.sendInvitationEmail("a@b.com,c@d.com", SOURCE, request, response);

        final Map<String, Object> expectedJson = new HashMap<>();
        expectedJson.put("sent", "2");
        expectedJson.put("rejected", new HashMap<String, Object>());
        assertThat(responseJson(), is(equalTo(expectedJson)));
    }

    @Test
    public void shouldAcceptSpacesBetweenEmailAddress() throws IOException {
        InvitationSendingResult expected = new InvitationSendingResult(2, new HashSet<InvitationSendingResult.Rejection>());
        when(invitationLobbyService.sendInvitations(PLAYER_ID, APP_INFO, SOURCE, "", new String[]{"a@b.com", "c@d.com"}, false, USER_IP_ADDRESS))
                .thenReturn(expected);

        underTest.sendInvitationEmail("a@b.com, c@d.com", SOURCE, request, response);

        final Map<String, Object> expectedJson = new HashMap<>();
        expectedJson.put("sent", "2");
        expectedJson.put("rejected", new HashMap<String, Object>());
        assertThat(responseJson(), is(equalTo(expectedJson)));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> responseJson() throws IOException {
        final ArgumentCaptor<Map> jsonCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webApiResponses).writeOk(eq(response), jsonCaptor.capture());
        return (Map<String, Object>) jsonCaptor.getValue();
    }

}
