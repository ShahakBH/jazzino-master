package com.yazino.web.service;

import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.player.LoginResult;
import com.yazino.platform.player.PlayerProfileLoginResponse;
import com.yazino.platform.player.service.AuthenticationService;
import com.yazino.test.ThreadLocalDateTimeUtils;
import com.yazino.web.domain.GameTypeResolver;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionFactory;
import com.yazino.web.util.CookieHelper;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.HashMap;

import static com.yazino.platform.Platform.WEB;
import static com.yazino.platform.player.LoginResult.EXISTING_USER;
import static com.yazino.platform.player.LoginResult.NEW_USER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class YazinoWebLoginServiceTest {
    private static final BigDecimal PLAYER_PROFILE_ID = BigDecimal.valueOf(10);
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(20);
    public static final HashMap<String,Object> EMPTY_CLIENT_CONTEXT = new HashMap<String, Object>();

    @Mock
    private CookieHelper cookieHelper;
    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private RememberMeHandler rememberMeHandler;
    @Mock
    private LobbySessionFactory lobbySessionFactory;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private YazinoWebLoginService underTest;

    @Mock private GameTypeResolver gameTypeResolver;

    @Before
    public void setUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime().getMillis());

        underTest = new YazinoWebLoginService(authenticationService, cookieHelper, rememberMeHandler, lobbySessionFactory,
                                              gameTypeResolver);
        when(gameTypeResolver.resolveGameType(request,response)).thenReturn("SLOTS");
    }

    @After
    public void resetJoda() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test(expected = NullPointerException.class)
    public void theServiceCannotBeCreatedWithANullYazinoAuthenticationService() {
        new YazinoWebLoginService(null, cookieHelper, rememberMeHandler, lobbySessionFactory, gameTypeResolver);
    }

    @Test(expected = NullPointerException.class)
    public void theServiceCannotBeCreatedWithANullCookieHelper() {
        new YazinoWebLoginService(authenticationService, null, rememberMeHandler, lobbySessionFactory, gameTypeResolver);
    }

    @Test(expected = NullPointerException.class)
    public void theServiceCannotBeCreatedWithANullRememberMeHandler() {
        new YazinoWebLoginService(authenticationService, cookieHelper, null, lobbySessionFactory, gameTypeResolver);
    }

    @Test(expected = NullPointerException.class)
    public void theServiceCannotBeCreatedWithANullLobbySessionFactory() {
        new YazinoWebLoginService(authenticationService, cookieHelper, rememberMeHandler, null, gameTypeResolver);
    }

    @Test
    public void aBlockedUserReturnsTheBlockedResult() {
        when(authenticationService.loginYazinoUser("anEmail", "aPassword"))
                .thenReturn(new PlayerProfileLoginResponse(LoginResult.BLOCKED));

        final LoginResult result = underTest.login(request, response, "anEmail", "aPassword", WEB, null);

        assertThat(result, is(equalTo(LoginResult.BLOCKED)));
    }

    @Test
    public void aLoginFailureReturnsTheFailureResult() {
        when(authenticationService.loginYazinoUser("anEmail", "aPassword"))
                .thenReturn(new PlayerProfileLoginResponse(LoginResult.FAILURE));

        final LoginResult result = underTest.login(request, response, "anEmail", "aPassword", WEB, null);

        assertThat(result, is(equalTo(LoginResult.FAILURE)));
    }

    @Test
    public void aLoginUpdateSuccessReturnsTheExistingUserResult() {
        when(authenticationService.loginYazinoUser("anEmail", "aPassword"))
                .thenReturn(new PlayerProfileLoginResponse(PLAYER_ID, EXISTING_USER));

        final LoginResult result = underTest.login(request, response, "anEmail", "aPassword", WEB, null);

        assertThat(result, is(equalTo(LoginResult.EXISTING_USER)));
    }

    @Test
    public void aLoginCreatedSuccessReturnsTheNewUserResult() {
        when(authenticationService.loginYazinoUser("anEmail", "aPassword"))
                .thenReturn(new PlayerProfileLoginResponse(PLAYER_ID, NEW_USER));

        final LoginResult result = underTest.login(request, response, "anEmail", "aPassword", WEB, null);

        assertThat(result, is(equalTo(LoginResult.NEW_USER)));
    }

    @Test
    public void aLoginUpdateSuccessCreatesALobbySession() {
        when(authenticationService.loginYazinoUser("anEmail", "aPassword"))
                .thenReturn(new PlayerProfileLoginResponse(PLAYER_ID, EXISTING_USER));

        underTest.login(request, response, "anEmail", "aPassword", WEB, Partner.YAZINO);

        verify(lobbySessionFactory).registerAuthenticatedSession(request, response, Partner.YAZINO, PLAYER_ID,
                EXISTING_USER, true, WEB, EMPTY_CLIENT_CONTEXT, "SLOTS");
    }

    @Test
    public void aLoginCreatedSuccessCreatesALobbySession() {
        when(authenticationService.loginYazinoUser("anEmail", "aPassword"))
                .thenReturn(new PlayerProfileLoginResponse(PLAYER_ID, NEW_USER));

        underTest.login(request, response, "anEmail", "aPassword", WEB, Partner.YAZINO);

        verify(lobbySessionFactory).registerAuthenticatedSession(request, response, Partner.YAZINO, PLAYER_ID,
                NEW_USER, true, WEB, EMPTY_CLIENT_CONTEXT, "SLOTS");
    }

    @Test
    public void aLoginSuccessClearTheRedirectionCookie() {
        when(authenticationService.loginYazinoUser("anEmail", "aPassword"))
                .thenReturn(new PlayerProfileLoginResponse(PLAYER_ID, NEW_USER));

        underTest.login(request, response, "anEmail", "aPassword", WEB, Partner.YAZINO);

        verify(cookieHelper).setRedirectTo(response, null);
    }

    @Test
    public void aLoginSuccessSetsTheRememberMeCookie() {
        when(authenticationService.loginYazinoUser("anEmail", "aPassword"))
                .thenReturn(new PlayerProfileLoginResponse(PLAYER_ID, NEW_USER));
        when(
                lobbySessionFactory.registerAuthenticatedSession(
                        request, response, Partner.YAZINO, PLAYER_ID,
                        NEW_USER, true, WEB, EMPTY_CLIENT_CONTEXT, "SLOTS")).thenReturn(
                new LobbySession(
                        BigDecimal.valueOf(3141592), PLAYER_ID, "playerName",
                        "sessionkey", Partner.YAZINO, "aPictureUrl", "anEmail", null, true, WEB, AuthProvider.YAZINO));


        underTest.login(request, response, "anEmail", "aPassword", WEB, Partner.YAZINO);

        verify(rememberMeHandler).storeRememberMeCookie(Partner.YAZINO, WEB, PLAYER_ID, "anEmail", request, response);
    }
}
