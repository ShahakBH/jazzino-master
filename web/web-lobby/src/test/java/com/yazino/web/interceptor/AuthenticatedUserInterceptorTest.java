package com.yazino.web.interceptor;

import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.session.Session;
import com.yazino.platform.session.SessionService;
import com.yazino.web.domain.GameTypeResolver;
import com.yazino.web.security.ProtectedResourceClassifier;
import com.yazino.web.service.RememberMeHandler;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.session.LobbySessionReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;

import static com.yazino.platform.Platform.WEB;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AuthenticatedUserInterceptorTest {
    private static final String NO_COOKIES_MAPPING = "/noCookies";
    private static final String REMOTE_ADDRESS = "10.45.3.3";
    private static final String ANY_HTTP_METHOD = null;
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(3141592);
    private static final String LOGIN_ACTION = "loginAction";
    private static final String REQUEST_URI = "foo";
    private static final String QUERY_STRING = "bar";

    @Mock
    private ProtectedResourceClassifier protectedResourceClassifier;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private SessionService sessionService;
    @Mock
    private RememberMeHandler rememberMeHandler;

    private final Object handler = new Object();

    private LobbySessionCache lobbySessionCache;
    private LobbySession lobbySession;

    private AuthenticatedUserInterceptor underTest;
    @Mock private GameTypeResolver gameTypeResolver;

    @Before
    public void init() {
        lobbySessionCache = new LobbySessionCache(sessionService);

        when(gameTypeResolver.resolveGameType(request, response)).thenReturn("SLOTS");

        underTest = new AuthenticatedUserInterceptor(lobbySessionCache, rememberMeHandler, LOGIN_ACTION, protectedResourceClassifier,
                                                     gameTypeResolver);

        lobbySession = new LobbySession(SESSION_ID, BigDecimal.ONE, "playerName", "sessionKey", Partner.YAZINO, null, null, null, false, WEB, AuthProvider.YAZINO);

        when(request.getRequestURI()).thenReturn(REQUEST_URI);
        when(request.getQueryString()).thenReturn(QUERY_STRING);
        when(request.getRemoteAddr()).thenReturn(REMOTE_ADDRESS);
        when(request.getHeader("Accept")).thenReturn("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

        when(protectedResourceClassifier.requiresAuthorisation(anyString())).thenReturn(true);
    }

    @Test
    public void shouldRedirectToLoginWhenNoCookiesAndNotLoggedInAndClientAcceptsHTML() throws Exception {
        whenIsNotLoggedIn();
        when(request.getHeader("Accept")).thenReturn("application/xml;q=0.9,*/*;q=0.8");

        underTest.preHandle(request, response, handler);

        verify(response).sendRedirect(String.format("/%s?from=%s&%s", LOGIN_ACTION, REQUEST_URI, QUERY_STRING));
    }

    @Test
    public void shouldRedirectToLoginWhenNoCookiesAndNotLoggedInAndClientAcceptsWildcardTypes() throws Exception {
        whenIsNotLoggedIn();

        underTest.preHandle(request, response, handler);

        verify(response).sendRedirect(String.format("/%s?from=%s&%s", LOGIN_ACTION, REQUEST_URI, QUERY_STRING));
    }

    @Test
    public void shouldReturnUnauthorisedWhenNoCookiesAndNotLoggedInAndClientDoesNotAcceptHTMLOrAWildcard() throws Exception {
        whenIsNotLoggedIn();
        when(request.getHeader("Accept")).thenReturn("application/json,application/xml;q=0.9");

        underTest.preHandle(request, response, handler);

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
        verifyNoMoreInteractions(response);
    }

    @Test
    public void shouldRedirectToNoCookiesWhenLoggedInButStillNoCookie() throws Exception {
        whenIsLoggedInWithNoCookies();

        underTest.preHandle(request, response, handler);

        verify(response).sendRedirect(NO_COOKIES_MAPPING);
    }

    @Test
    public void shouldAllowAccessToUnprotectedPages_notLoggedInAndNoCookie() throws Exception {
        when(protectedResourceClassifier.requiresAuthorisation("/whitelisted")).thenReturn(false);
        whenIsLoggedInWithNoCookies();
        MockHttpServletRequest req = new MockHttpServletRequest(ANY_HTTP_METHOD, "/whitelisted");

        underTest.preHandle(req, response, handler);

        verifyZeroInteractions(response);
    }

    @Test
    public void shouldAllowAccessToUnprotectedPages_loggedInAndNoCookie() throws Exception {
        when(protectedResourceClassifier.requiresAuthorisation("/whitelisted")).thenReturn(false);
        whenIsLoggedInWithNoCookies();
        MockHttpServletRequest req = new MockHttpServletRequest(ANY_HTTP_METHOD, "/whitelisted");

        underTest.preHandle(req, response, handler);

        verifyZeroInteractions(response);
    }

    @Test
    public void shouldAllowAccessToUnprotectedPages_loggedInAndCookie() throws Exception {
        when(protectedResourceClassifier.requiresAuthorisation("/whitelisted")).thenReturn(false);
        whenIsLoggedInWithCookies(createSessionCookie());
        MockHttpServletRequest req = new MockHttpServletRequest(ANY_HTTP_METHOD, "/whitelisted");

        underTest.preHandle(req, response, handler);

        verifyZeroInteractions(response);
    }

    @Test
    public void testNoRedirectIfThereIsASessionCookieAndIsLoggedIn() throws Exception {
        whenIsLoggedInWithCookies(createSessionCookie());

        underTest.preHandle(request, response, handler);

        verifyZeroInteractions(response);
    }

    @Test
    public void whenHandlingAFacebookLoginRequestThenTrueIsReturned() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest(ANY_HTTP_METHOD, "/public/fbLogin/HIGH_STAKES");
        when(protectedResourceClassifier.requiresAuthorisation("/public/fbLogin/HIGH_STAKES")).thenReturn(false);
        whenIsNotLoggedIn();

        boolean proceedWithChain = underTest.preHandle(req, response, handler);

        assertTrue(proceedWithChain);
    }

    @Test
    public void whenHandlingAFacebookLoginRequestThenAutoLoginIsNotAttempted() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest(ANY_HTTP_METHOD, "/public/fbLogin/HIGH_STAKES");
        whenIsNotLoggedIn();
        when(protectedResourceClassifier.requiresAuthorisation("/public/fbLogin/HIGH_STAKES")).thenReturn(false);

        underTest.preHandle(req, response, handler);

        verifyZeroInteractions(rememberMeHandler);
    }

    @Test
    public void whenHandlingANonFacebookLoginRequestThenAutoLoginIsAttempted() throws Exception {
        whenIsNotLoggedIn();


        underTest.preHandle(request, response, handler);

        verify(rememberMeHandler).attemptAutoLogin(request, response, new HashMap<String, Object>(), "SLOTS");
    }

    private Cookie createSessionCookie() {
        return new Cookie(LobbySessionCache.COOKIE_KEY, new LobbySessionReference(lobbySession).encode());
    }

    private void whenIsLoggedInWithNoCookies() {
        isLoggedInExpectations(true);
    }

    private void whenIsLoggedInWithCookies(final Cookie... cookies) {
        isLoggedInExpectations(true, cookies);
    }

    private void whenIsNotLoggedIn() {
        isLoggedInExpectations(false);
    }

    private void isLoggedInExpectations(boolean loggedIn, Cookie... cookies) {
        if (loggedIn) {
            lobbySessionCache.setLobbySession(lobbySession);
        }
        final Session session = new Session(SESSION_ID, lobbySession.getPlayerId(), lobbySession.getPartnerId(),
                Platform.WEB, "anIpAddress", lobbySession.getLocalSessionKey(),
                lobbySession.getPlayerName(), lobbySession.getEmail(), lobbySession.getPictureUrl(), null, null, null, Collections.<String>emptySet());
        when(sessionService.authenticateAndExtendSession(lobbySession.getPlayerId(), lobbySession.getLocalSessionKey())).thenReturn(session);
        when(request.getCookies()).thenReturn(cookies);
    }
}
