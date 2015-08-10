package com.yazino.web.service;

import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.player.LoginResult;
import com.yazino.platform.player.PlayerInformationHolder;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.PlayerProfileLoginResponse;
import com.yazino.platform.player.service.AuthenticationService;
import com.yazino.test.ThreadLocalDateTimeUtils;
import com.yazino.web.domain.LoginResponse;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionFactory;
import com.yazino.web.session.ReferrerSessionCache;
import com.yazino.web.util.CookieHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static com.yazino.platform.Platform.FACEBOOK_CANVAS;
import static com.yazino.platform.Platform.WEB;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class ExternalWebLoginServiceTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(200);
    private static final String REMOTE_ADDRESS = "aRemoteAddress";
    public static final String SLOTS = "SLOTS";
    public static final HashMap<String, Object> EMPTY_CLIENT_CONTEXT = new HashMap<String, Object>();

    @Mock
    private CookieHelper cookieHelper;
    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private PlayerInformationHolder playerInformationHolder;
    @Mock
    private LobbySessionFactory lobbySessionFactory;
    @Mock
    private ReferrerSessionCache referrerSessionCache;

    private ExternalWebLoginService underTest;
    private LobbySession session;

    @Before
    public void setUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(2345);
        MockitoAnnotations.initMocks(this);

        when(request.getRemoteAddr()).thenReturn(REMOTE_ADDRESS);
        session = mock(LobbySession.class);
        when(lobbySessionFactory.registerAuthenticatedSession(
                eq(request),
                eq(response),
                any(Partner.class),
                any(BigDecimal.class),
                any(LoginResult.class),
                anyBoolean(),
                any(Platform.class), anyMap(), eq(SLOTS)))
                .thenReturn(session);

        underTest = new ExternalWebLoginService(authenticationService, cookieHelper, referrerSessionCache, lobbySessionFactory);
    }

    @After
    public void cleanUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test(expected = NullPointerException.class)
    public void theServiceCannotBeCreatedWithANullAuthenticationService() {
        new ExternalWebLoginService(null, cookieHelper, referrerSessionCache, lobbySessionFactory);
    }

    @Test(expected = NullPointerException.class)
    public void theServiceCannotBeCreatedWithANullCookieHelper() {
        new ExternalWebLoginService(authenticationService, null, referrerSessionCache, lobbySessionFactory);
    }

    @Test(expected = NullPointerException.class)
    public void theServiceCannotBeCreatedWithANullLobbySessionFactory() {
        new ExternalWebLoginService(authenticationService, cookieHelper, referrerSessionCache, null);
    }

    @Test
    public void aBlockedLoginReturnsWithNoFurtherAction() {
        when(authenticationService.loginExternalUser(REMOTE_ADDRESS, Partner.YAZINO, playerInformationHolder, null,
                WEB, SLOTS))
                .thenReturn(new PlayerProfileLoginResponse(LoginResult.BLOCKED));

        final LoginResponse result = underTest.login(request, response, Partner.YAZINO, SLOTS,
                playerInformationHolder, true, WEB, EMPTY_CLIENT_CONTEXT);

        assertThat(result.getResult(), is(equalTo(LoginResult.BLOCKED)));
        verifyZeroInteractions(cookieHelper);
    }

    @Test
    public void aFailedLoginReturnsWithNoFurtherAction() {
        when(authenticationService.loginExternalUser(REMOTE_ADDRESS, Partner.YAZINO, playerInformationHolder, null,
                WEB, SLOTS))
                .thenReturn(new PlayerProfileLoginResponse(LoginResult.FAILURE));

        final LoginResponse result = underTest.login(request, response, Partner.YAZINO, SLOTS,
                playerInformationHolder, true, WEB, EMPTY_CLIENT_CONTEXT);

        assertThat(result.getResult(), is(equalTo(LoginResult.FAILURE)));
        verifyZeroInteractions(cookieHelper);
        verifyZeroInteractions(lobbySessionFactory);
    }

    @Test
    public void aSuccessfulLoginForAnExistingUserSetsTheCanvasCookie() {
        when(authenticationService.loginExternalUser(REMOTE_ADDRESS, Partner.YAZINO, playerInformationHolder, null,
                WEB, SLOTS))
                .thenReturn(new PlayerProfileLoginResponse(PLAYER_ID, LoginResult.EXISTING_USER));

        underTest.login(request, response, Partner.YAZINO, SLOTS,
                playerInformationHolder, true, WEB, EMPTY_CLIENT_CONTEXT);

        verify(cookieHelper).setOnCanvas(response, false);
        verifyNoMoreInteractions(cookieHelper);
    }

    @Test
    public void aSuccessfulLoginForANewUSerSetsTheCanvasCookie() {
        when(authenticationService.loginExternalUser(REMOTE_ADDRESS, Partner.YAZINO, playerInformationHolder, null,
                FACEBOOK_CANVAS, SLOTS))
                .thenReturn(new PlayerProfileLoginResponse(PLAYER_ID, LoginResult.NEW_USER));

        underTest.login(request, response, Partner.YAZINO, SLOTS,
                playerInformationHolder, true, FACEBOOK_CANVAS, EMPTY_CLIENT_CONTEXT);

        verify(cookieHelper).setOnCanvas(response, true);
    }

    @Test
    public void aSuccessfulLoginForANewUserSetsTheNewPlayerCookie() {
        when(authenticationService.loginExternalUser(REMOTE_ADDRESS, Partner.YAZINO, playerInformationHolder, null,
                WEB, SLOTS))
                .thenReturn(new PlayerProfileLoginResponse(PLAYER_ID, LoginResult.NEW_USER));

        underTest.login(request, response, Partner.YAZINO, SLOTS,
                playerInformationHolder, true, WEB, EMPTY_CLIENT_CONTEXT);

        verify(cookieHelper).setIsNewPlayer(response);
    }

    @Test
    public void aSuccessfulLoginUpdateTheProviderWithThePlayerID() {
        when(playerInformationHolder.getPlayerProfile()).thenReturn(
                PlayerProfile.withPlayerId(PLAYER_ID).asProfile());
        when(authenticationService.loginExternalUser(REMOTE_ADDRESS, Partner.YAZINO, playerInformationHolder, null,
                WEB, SLOTS))
                .thenReturn(new PlayerProfileLoginResponse(PLAYER_ID, LoginResult.NEW_USER));

        underTest.login(request, response, Partner.YAZINO, SLOTS,
                playerInformationHolder, true, WEB, EMPTY_CLIENT_CONTEXT);

        assertThat(playerInformationHolder.getPlayerProfile().getPlayerId(), is(equalTo(PLAYER_ID)));
    }

    @Test
    public void aSuccessfulLoginForAnExistingUserCreatesANewSession() {
        when(authenticationService.loginExternalUser(REMOTE_ADDRESS, Partner.YAZINO, playerInformationHolder, null,
                WEB, SLOTS))
                .thenReturn(new PlayerProfileLoginResponse(PLAYER_ID, LoginResult.EXISTING_USER));

        underTest.login(request, response, Partner.YAZINO, SLOTS,
                playerInformationHolder, true, WEB, EMPTY_CLIENT_CONTEXT);

        verify(lobbySessionFactory).registerAuthenticatedSession(request, response, Partner.YAZINO,
                PLAYER_ID, LoginResult.EXISTING_USER, true, WEB, EMPTY_CLIENT_CONTEXT, SLOTS);
    }

    @Test
    public void aSuccessfulLoginForANewUserCreatesANewSession() {
        when(authenticationService.loginExternalUser(REMOTE_ADDRESS, Partner.YAZINO, playerInformationHolder, null,
                WEB, SLOTS))
                .thenReturn(new PlayerProfileLoginResponse(PLAYER_ID, LoginResult.NEW_USER));

        underTest.login(request, response, Partner.YAZINO, SLOTS,
                playerInformationHolder, true, WEB, EMPTY_CLIENT_CONTEXT);

        verify(lobbySessionFactory).registerAuthenticatedSession(request, response, Partner.YAZINO,
                PLAYER_ID, LoginResult.NEW_USER, true, WEB, EMPTY_CLIENT_CONTEXT, SLOTS);
    }

    @Test
    public void aSuccessfulLoginForANewUserReturnsTheSessionWithTheResponse() {
        when(authenticationService.loginExternalUser(REMOTE_ADDRESS, Partner.YAZINO, playerInformationHolder, null, WEB, SLOTS))
                .thenReturn(new PlayerProfileLoginResponse(PLAYER_ID, LoginResult.NEW_USER));

        final LoginResponse response = underTest.login(request, this.response, Partner.YAZINO, SLOTS,
                playerInformationHolder, true, WEB, EMPTY_CLIENT_CONTEXT);

        assertThat(response.getSession().isPresent(), is(true));
        assertThat(response.getSession().get(), is(equalTo(session)));
    }

    @Test
    public void shouldReturnAFailedResultWhenSessionHasNotBeenCreated() throws Exception {
        when(authenticationService.loginExternalUser(REMOTE_ADDRESS, Partner.YAZINO, playerInformationHolder, null, WEB, SLOTS))
                .thenReturn(new PlayerProfileLoginResponse(PLAYER_ID, LoginResult.EXISTING_USER));

        when(lobbySessionFactory.registerAuthenticatedSession(eq(request), eq(response), any(Partner.class),
                any(BigDecimal.class), any(LoginResult.class), anyBoolean(), any(Platform.class), eq(EMPTY_CLIENT_CONTEXT),
                eq(SLOTS)))
                .thenReturn(null);

        LoginResponse loginResult = underTest.login(request, response, Partner.YAZINO, SLOTS, playerInformationHolder,
                true, WEB, EMPTY_CLIENT_CONTEXT);
        assertEquals(LoginResult.FAILURE, loginResult.getResult());
    }

    @Test
    public void aSuccessfulLoginForAUserPassesTheClientContextToTheLobbySessionFactory() {
        Map<String, Object> expectedClientContext = new HashMap<>();
        expectedClientContext.put("stuff", "in here");

        when(authenticationService.loginExternalUser(REMOTE_ADDRESS, Partner.YAZINO, playerInformationHolder, null,
                WEB, SLOTS))
                .thenReturn(new PlayerProfileLoginResponse(PLAYER_ID, LoginResult.NEW_USER));

        underTest.login(request, response, Partner.YAZINO, SLOTS,
                playerInformationHolder, true, WEB, expectedClientContext);

        verify(lobbySessionFactory).registerAuthenticatedSession(eq(request), eq(response), any(Partner.class),
                any(BigDecimal.class), any(LoginResult.class), anyBoolean(), any(Platform.class), eq(expectedClientContext),
                eq(SLOTS));
    }
}
