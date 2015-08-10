package com.yazino.web.session;

import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.session.Session;
import com.yazino.platform.session.SessionService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Collections;

import static com.yazino.platform.Partner.YAZINO;
import static com.yazino.platform.Platform.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class LobbySessionCacheTest {
    private static final String SESSION_KEY = "SessionKEY";
    private static final AuthProvider PROVIDER = AuthProvider.YAZINO;
    private static final String IP_ADDRESS = "10.9.34.23";
    private static final String SESSION_KEY2 = "103XXXXX";
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(3141592);

    private BigDecimal playerId = new BigDecimal("102.2");
    private BigDecimal playerId2 = new BigDecimal("103.3");

    private HttpServletRequest request;

    @Before
    public void setUp() {
        request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(IP_ADDRESS);
    }

    @Test
    public void testGetActiveSession_returns_local_session_if_defined() {
        LobbySessionCache lsc = prepareBackingSession();
        LobbySession l = prepopulateSession();
        lsc.setLobbySession(l);
        when(request.getCookies()).thenReturn(new Cookie[0]);

        assertSameSession(l, lsc.getActiveSession(request));
    }

    private void assertSameSession(LobbySession expected, LobbySession actual) {
        assertEquals(expected.getPlayerId(), actual.getPlayerId());
        assertEquals(expected.getLocalSessionKey(), actual.getLocalSessionKey());
    }

    @Test
    public void testGetActiveSession_returns_local_session_from_cookies_if_local_undefined() {
        LobbySessionCache lsc = prepareBackingSession();
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie(LobbySessionCache.COOKIE_KEY,
                referenceWith(playerId, SESSION_KEY, null).encode())});

        LobbySession lobbySession = lsc.getActiveSession(request);
        assertEquals(SESSION_KEY, lobbySession.getLocalSessionKey());
        assertEquals(playerId, lobbySession.getPlayerId());
        assertEquals(WEB, lobbySession.getPlatform());
    }

    @Test
    public void testGetActiveSession_returns_local_session_from_cookies_with_platform_if_local_undefined() {
        LobbySessionCache lsc = prepareBackingSession();
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie(LobbySessionCache.COOKIE_KEY,
                referenceWith(playerId, SESSION_KEY, WEB).encode())});

        LobbySession lobbySession = lsc.getActiveSession(request);
        assertEquals(SESSION_KEY, lobbySession.getLocalSessionKey());
        assertEquals(playerId, lobbySession.getPlayerId());
        assertEquals(Platform.WEB, lobbySession.getPlatform());
    }

    @Test
    public void testGetActiveSession_returns_session_from_cookies_if_local_does_not_match_cookies() {
        LobbySessionCache lsc = prepareBackingSession();
        lsc.setLobbySession(new LobbySession(SESSION_ID, playerId, "name", SESSION_KEY, null, null, null, null, false,
                WEB, PROVIDER));
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie(LobbySessionCache.COOKIE_KEY,
                referenceWith(playerId2, SESSION_KEY2, null).encode())});

        LobbySession lobbySession = lsc.getActiveSession(request);
        assertEquals(SESSION_KEY2, lobbySession.getLocalSessionKey());
        assertEquals(playerId2, lobbySession.getPlayerId());
    }

    /**
     * initialises lobby session cache so that account detail service has a session
     */
    private LobbySessionCache prepareBackingSession() {
        final SessionService sessionService = mock(SessionService.class);
        when(sessionService.authenticateAndExtendSession(playerId, SESSION_KEY)).thenReturn(
                new Session(SESSION_ID, playerId, YAZINO, Platform.WEB, "ipAddress", SESSION_KEY, "name", null, null, null, null, null, Collections.<String>emptySet()));
        when(sessionService.authenticateAndExtendSession(playerId2, SESSION_KEY2)).thenReturn(
                new Session(SESSION_ID, playerId2, YAZINO, Platform.WEB, "ipAddress", SESSION_KEY2, "name", null, null, null, null, null, Collections.<String>emptySet()));

        return new LobbySessionCache(sessionService);
    }

    /**
     * initialises lobby session cache so that account detail service has a session
     */
    private LobbySessionCache prepareNoBackingSession() {
        final SessionService sessionService = mock(SessionService.class);
        when(sessionService.authenticateAndExtendSession(playerId, SESSION_KEY)).thenReturn(null);

        return new LobbySessionCache(sessionService);
    }

    @Test
    public void testGetActiveSession_sets_local_session_from_cookies_if_local_undefined() {
        LobbySessionCache lsc = prepareBackingSession();
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie(LobbySessionCache.COOKIE_KEY,
                referenceWith(playerId, SESSION_KEY, null).encode())
        }).thenReturn(new Cookie[0]);

        LobbySession lobbySession = lsc.getActiveSession(request);
        LobbySession cached = lsc.getActiveSession(request);
        assertSameSession(lobbySession, cached);
    }

    @Test
    public void testGetActiveSession_returns_null_if_AccountDetailService_rejects_cookie_session() {
        LobbySessionCache lsc = prepareNoBackingSession();
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie(LobbySessionCache.COOKIE_KEY,
                referenceWith(playerId, SESSION_KEY, null).encode())
        });

        LobbySession lobbySession = lsc.getActiveSession(request);
        Assert.assertNull(lobbySession);
    }

    @Test
    public void testGetActiveSession_returns_null_after_invalidate_any_session() {
        LobbySessionCache lsc = prepareBackingSession();
        LobbySessionCache lsc2 = prepareBackingSession();
        lsc.setLobbySession(new LobbySession(SESSION_ID, playerId, "name", SESSION_KEY, null, null, null, null, false, WEB, PROVIDER));
        lsc2.setLobbySession(new LobbySession(SESSION_ID, playerId, "name", SESSION_KEY, null, null, null, null, false, WEB, PROVIDER));
        final Cookie cookie = lsc.invalidateLocalSession();
        when(request.getCookies()).thenReturn(new Cookie[]{cookie});

        Assert.assertNull(lsc.getActiveSession(request));
        Assert.assertNull(lsc2.getActiveSession(request));
    }

    @Test
    public void testGetActiveSession_returns_null_if_AccountDetailService_rejects_cookie_session_even_when_local_defined() {
        LobbySessionCache lsc = prepareNoBackingSession();
        lsc.setLobbySession(prepopulateSession());
        lsc.setLastSessionCacheRenewal(-1);
        when(request.getCookies()).thenReturn(new Cookie[0]);

        Assert.assertNull(lsc.getActiveSession(request));
    }

    private LobbySession prepopulateSession(Platform platform) {
        return new LobbySession(SESSION_ID, playerId, "name", SESSION_KEY, null, null, null, null, false, platform, PROVIDER);
    }

    private LobbySession prepopulateSession() {
        return new LobbySession(SESSION_ID, playerId, "name", SESSION_KEY, null, null, null, null, false, WEB, PROVIDER);
    }

    @Test
    public void testGetActiveSession_leaves_local_session_null_if_AccountDetailService_rejects_cookie_session() {
        LobbySessionCache lsc = prepareNoBackingSession();
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie(LobbySessionCache.COOKIE_KEY,
                referenceWith(playerId, SESSION_KEY, null).encode())
        });

        LobbySession lobbySession = lsc.getActiveSession(request);
        Assert.assertNull(lobbySession);
    }

    @Test
    public void testAuthenticateAndExtendSession_is_still_called_even_if_local_session_cache_exists_but_last_verified_was_before_expiry() {
        LobbySession l = prepopulateSession();

        final SessionService sessionService = mock(SessionService.class);
        when(sessionService.authenticateAndExtendSession(playerId, SESSION_KEY)).thenReturn(
                new Session(SESSION_ID, playerId, YAZINO, Platform.WEB, "ipAddress", SESSION_KEY, "name", null, null, null, null, null, Collections.<String>emptySet()));

        LobbySessionCache lsc = new LobbySessionCache(sessionService);
        lsc.setLobbySession(l);
        lsc.setLastSessionCacheRenewal(System.currentTimeMillis() - 1000 * lsc.getSessionCacheExpiryInSeconds() - 100);
        when(request.getCookies()).thenReturn(null);

        lsc.getActiveSession(request);

        verify(sessionService).authenticateAndExtendSession(eq(playerId), eq(SESSION_KEY));
    }

    @Test
    public void whenSessionIsRenewedThenPlatfomIsCopiedFromOldSession() {
        LobbySession l = prepopulateSession(IOS);

        final SessionService sessionService = mock(SessionService.class);
        when(sessionService.authenticateAndExtendSession(playerId, SESSION_KEY)).thenReturn(
                new Session(SESSION_ID, playerId, YAZINO, Platform.WEB, "ipAddress", SESSION_KEY, "name", null, null, null, null, null, Collections.<String>emptySet()));

        LobbySessionCache lsc = new LobbySessionCache(sessionService);
        lsc.setLobbySession(l);
        lsc.setLastSessionCacheRenewal(System.currentTimeMillis() - 1000 * lsc.getSessionCacheExpiryInSeconds() - 100);
        when(request.getCookies()).thenReturn(null);

        final LobbySession activeSession = lsc.getActiveSession(request);
        assertThat(activeSession.getPlatform(), is(IOS));
    }

    @Test
    public void testAuthenticateAndExtendSession_is_not_called_if_local_session_cache_exists_but_last_verified_was_not_before_expiry() {
        LobbySession l = prepopulateSession();
        final SessionService sessionService = mock(SessionService.class);

        LobbySessionCache lsc = new LobbySessionCache(sessionService);
        lsc.setLobbySession(l);
        lsc.setLastSessionCacheRenewal(System.currentTimeMillis());
        when(request.getCookies()).thenReturn(null);

        lsc.getActiveSession(request);
    }

    @Test
    public void testGetSessionFromCookies_unpacks_cookies_and_sets_default_platform() {
        Cookie[] cookies = new Cookie[1];
        cookies[0] = new Cookie(LobbySessionCache.COOKIE_KEY,
                referenceWith(playerId, "SESSION", null).encode());
        LobbySessionCache lsc = new LobbySessionCache(mock(SessionService.class));
        when(request.getCookies()).thenReturn(cookies);
        LobbySessionReference sessionReference = ReflectionTestUtils.invokeMethod(lsc, "getSessionRequestFromHeaderOrCookie", request);
        assertEquals(new BigDecimal("102.2"), sessionReference.getPlayerId());
        assertEquals("SESSION", sessionReference.getSessionKey());
        assertEquals(WEB, sessionReference.getPlatform());
    }

    @Test
    public void testGetSessionFromCookies_unpacks_cookies() {
        Cookie[] cookies = new Cookie[1];
        cookies[0] = new Cookie(LobbySessionCache.COOKIE_KEY,
                referenceWith(playerId, "SESSION", IOS).encode());
        LobbySessionCache lsc = new LobbySessionCache(mock(SessionService.class));
        when(request.getCookies()).thenReturn(cookies);
        LobbySessionReference sessionReference = ReflectionTestUtils.invokeMethod(lsc, "getSessionRequestFromHeaderOrCookie", request);
        assertEquals(new BigDecimal("102.2"), sessionReference.getPlayerId());
        assertEquals("SESSION", sessionReference.getSessionKey());
        assertEquals(IOS, sessionReference.getPlatform());
    }

    @Test
    public void testGetSessionFromCookies_returns_null_if_invalid_format() {
        LobbySessionCache lsc = new LobbySessionCache(mock(SessionService.class));

        Assert.assertNull("no plus", ReflectionTestUtils.invokeMethod(lsc, "getSessionRequestFromHeaderOrCookie", request));
        Assert.assertNull("invalid number", ReflectionTestUtils.invokeMethod(lsc, "getSessionRequestFromHeaderOrCookie", request));
        Assert.assertNull("no plus", ReflectionTestUtils.invokeMethod(lsc, "getSessionRequestFromHeaderOrCookie", request));
        Assert.assertNull("more than one plus", ReflectionTestUtils.invokeMethod(lsc, "getSessionRequestFromHeaderOrCookie", request));
        Assert.assertNull("empty value", ReflectionTestUtils.invokeMethod(lsc, "getSessionRequestFromHeaderOrCookie", request));
        Assert.assertNull("not encoded", ReflectionTestUtils.invokeMethod(lsc, "getSessionRequestFromHeaderOrCookie", request));
        Assert.assertNull("null", ReflectionTestUtils.invokeMethod(lsc, "getSessionRequestFromHeaderOrCookie", request));
    }

    @Test
    public void testGenerateLocalSessionCookie_packs_session() {
        LobbySessionCache lsc = new LobbySessionCache(mock(SessionService.class));
        lsc.setLobbySession(new LobbySession(SESSION_ID, new BigDecimal("102.2"), null, "1837201", null, null, null, null, false, ANDROID, PROVIDER));

        Cookie cookie = ReflectionTestUtils.invokeMethod(lsc, "generateLocalSessionCookie", request);

        assertEquals(referenceWith(playerId, "1837201", ANDROID).encode(), cookie.getValue());
        assertEquals("/", cookie.getPath());
        assertEquals(LobbySessionCache.COOKIE_KEY, cookie.getName());
    }

    @Test
    public void shouldVerifyIfSessionRequestIsPresent_UsingCookies() {
        LobbySessionCache lsc = new LobbySessionCache(mock(SessionService.class));
        final Cookie[] cookies = new Cookie[]{new Cookie(LobbySessionCache.COOKIE_KEY, "some value")};
        when(request.getCookies()).thenReturn(cookies);
        assertTrue(lsc.hasSessionRequestHeaderOrCookie(request));
    }

    @Test
    public void shouldVerifyIfSessionRequestIsPresent_EmptyCookie() {
        LobbySessionCache lsc = new LobbySessionCache(mock(SessionService.class));
        final Cookie[] cookies = new Cookie[]{new Cookie(LobbySessionCache.COOKIE_KEY, "")};
        when(request.getCookies()).thenReturn(cookies);
        assertFalse(lsc.hasSessionRequestHeaderOrCookie(request));
    }

    @Test
    public void shouldVerifyIfSessionRequestIsPresent_CookieNotPresent() {
        LobbySessionCache lsc = new LobbySessionCache(mock(SessionService.class));
        when(request.getCookies()).thenReturn(new Cookie[]{});
        assertFalse(lsc.hasSessionRequestHeaderOrCookie(request));
    }

    @Test
    public void shouldVerifyIfSessionRequestIsPresent_NoCookies() {
        LobbySessionCache lsc = new LobbySessionCache(mock(SessionService.class));
        when(request.getCookies()).thenReturn(null);
        assertFalse(lsc.hasSessionRequestHeaderOrCookie(request));
    }

    @Test
    public void shouldVerifyIfSessionRequestIsPresent_UsingHeader() {
        LobbySessionCache lsc = new LobbySessionCache(mock(SessionService.class));
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader(LobbySessionCache.HEADER_KEY)).thenReturn("some value");
        assertTrue(lsc.hasSessionRequestHeaderOrCookie(request));
    }

    @Test
    public void shouldVerifyIfSessionRequestIsPresent_HeaderEmpty() {
        LobbySessionCache lsc = new LobbySessionCache(mock(SessionService.class));
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader(LobbySessionCache.HEADER_KEY)).thenReturn("");
        assertFalse(lsc.hasSessionRequestHeaderOrCookie(request));
    }

    @Test
    public void shouldVerifyIfSessionRequestIsPresent_HeaderNotPresent() {
        LobbySessionCache lsc = new LobbySessionCache(mock(SessionService.class));
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader(LobbySessionCache.HEADER_KEY)).thenReturn(null);
        assertFalse(lsc.hasSessionRequestHeaderOrCookie(request));
    }

    @Test
    public void shouldVerifyIfSessionRequestIsPresent_NoRequest() {
        LobbySessionCache lsc = new LobbySessionCache(mock(SessionService.class));
        assertFalse(lsc.hasSessionRequestHeaderOrCookie(null));
    }

    @Test
    public void shouldAllowInvalidatingSessionByCreatingCookieWithMaxAgeZero() {
        LobbySessionCache lsc = new LobbySessionCache(mock(SessionService.class));
        final Cookie cookie = lsc.invalidateLocalSession();
        assertEquals(0, cookie.getMaxAge());
    }

    private LobbySessionReference referenceWith(final BigDecimal playerId,
                                                final String sessionKey,
                                                final Platform platform) {
        return new LobbySessionReference(new LobbySession(SESSION_ID, playerId, "aPlayerName", sessionKey, YAZINO, "aPictureUrl", "anEmail", null, false, platform, PROVIDER));
    }
}
