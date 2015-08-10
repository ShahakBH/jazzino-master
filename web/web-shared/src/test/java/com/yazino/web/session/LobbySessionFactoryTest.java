package com.yazino.web.session;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.player.LoginResult;
import com.yazino.platform.session.SessionClientContextKey;
import com.yazino.test.ThreadLocalDateTimeUtils;
import com.yazino.web.service.TopUpResultService;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import strata.server.lobby.api.promotion.message.TopUpRequest;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.yazino.platform.Partner.YAZINO;
import static com.yazino.platform.Platform.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class LobbySessionFactoryTest {
    private static final Partner PARTNER_ID = YAZINO;
    private static final AuthProvider PROVIDER = AuthProvider.YAZINO;
    private static final String REMOTE_ADDRESS = "aRemoteAddress";
    private static final BigDecimal PLAYER_PROFILE_ID = BigDecimal.valueOf(10);
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(11);
    private static final String REFERRER = "aReferrer";
    public static final String LOGIN_URL = "http://a.login.url";
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(3141592);
    public static final HashMap<String, Object> CLIENT_CONTEXT_EMPTY = new HashMap<>();

    private final HashMap<String, Object> CLIENT_CONTEXT_MAP = new HashMap<>();


    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private SessionFactory sessionFactory;
    @Mock
    private LobbySessionCache lobbySessionCache;
    @Mock
    private ReferrerSessionCache referrerSessionCache;
    @Mock
    QueuePublishingService<TopUpRequest> queuePublishingService;
    @Mock
    TopUpResultService topUpResultService;

    private LobbySessionFactory underTest;

    @Mock
    private YazinoConfiguration yazinoConfiguration;
    private String gameType="SLOTS";

    @Before
    public void setUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime().getMillis());
        MockitoAnnotations.initMocks(this);

        underTest = new LobbySessionFactory(sessionFactory, lobbySessionCache, referrerSessionCache, queuePublishingService, topUpResultService, yazinoConfiguration);

        when(referrerSessionCache.getReferrer()).thenReturn(REFERRER);
        when(request.getRemoteAddr()).thenReturn(REMOTE_ADDRESS);
        CLIENT_CONTEXT_MAP.put(SessionClientContextKey.DEVICE_ID.name(), "unique identifier for device");
    }

    @After
    public void resetJoda() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void topupRequestShouldNotBePushedForDisabledGame() {
        expectASessionForPlatform(WEB);
        final List<Object> disabledGameTypes = new ArrayList<Object>();
        disabledGameTypes.add("SLOTS");
        disabledGameTypes.add("BLACKJACK");
        when(yazinoConfiguration.getList("strata.server.lobby.progressive.bonus.disabled.games", new ArrayList<Object>()))
                .thenReturn(disabledGameTypes);
        underTest.registerAuthenticatedSession(request, response, PARTNER_ID, PLAYER_ID,
                                               LoginResult.NEW_USER, true, WEB, CLIENT_CONTEXT_EMPTY, "SLOTS");
        verifyZeroInteractions(queuePublishingService);
    }

    @Test
    public void topupRequestShouldBePushedForGameThatIsntDisabled() {
        expectASessionForPlatform(WEB);
        final List<Object> disabledGameTypes = new ArrayList<Object>();
        when(yazinoConfiguration.getList("strata.server.lobby.progressive.bonus.disabled.games", new ArrayList<Object>()))
                .thenReturn(disabledGameTypes);
        underTest.registerAuthenticatedSession(request, response, PARTNER_ID, PLAYER_ID,
                                               LoginResult.NEW_USER, true, WEB, CLIENT_CONTEXT_EMPTY, "SLOTS");
        verify(queuePublishingService).send(any(TopUpRequest.class));
    }

    @Test
    public void topupRequestShouldBePushedForGameThatIsntSpecified() {
        expectASessionForPlatform(WEB);
        final List<Object> disabledGameTypes = new ArrayList<Object>();
        when(yazinoConfiguration.getList("strata.server.lobby.progressive.bonus.disabled.games", new ArrayList<Object>()))
                .thenReturn(disabledGameTypes);
        underTest.registerAuthenticatedSession(request, response, PARTNER_ID, PLAYER_ID,
                                               LoginResult.NEW_USER, true, WEB, CLIENT_CONTEXT_EMPTY, "");
        verify(queuePublishingService).send(any(TopUpRequest.class));
    }

    @Test
    public void theIpAddressIsTakenFromTheXForwardedByHeaderIfAvailable() {
        expectASessionWithIP("anIpAddress");
        when(request.getHeader("X-Forwarded-For")).thenReturn("anIpAddress");

        underTest.registerAuthenticatedSession(request, response, PARTNER_ID, PLAYER_PROFILE_ID,
                                               LoginResult.NEW_USER, true, ANDROID, CLIENT_CONTEXT_EMPTY, gameType);

        final PartnerSession expectedSession =
                new PartnerSession(REFERRER, "anIpAddress", PARTNER_ID, ANDROID, LOGIN_URL);
        verify(sessionFactory).registerNewSession(BigDecimal.TEN, expectedSession, ANDROID, LoginResult.NEW_USER, CLIENT_CONTEXT_EMPTY);
    }

    @Test
    public void onlyTheFirstIpAddressIsTakenFromTheForwardedHeader() {
        expectASessionWithIP("10.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 10.0.0.2, 10.0.0.3");

        underTest.registerAuthenticatedSession(request, response, PARTNER_ID, PLAYER_PROFILE_ID,
                                               LoginResult.NEW_USER, true, WEB, CLIENT_CONTEXT_EMPTY, gameType);

        final PartnerSession expectedSession =
                new PartnerSession(REFERRER, "10.0.0.1", PARTNER_ID, WEB, LOGIN_URL);
        verify(sessionFactory).registerNewSession(BigDecimal.TEN, expectedSession, WEB, LoginResult.NEW_USER, CLIENT_CONTEXT_EMPTY);
    }

    @Test
    public void theIpAddressIsTakenFromRequestIfNoXForwardedByHeaderIsAvailable() {
        expectASessionWithIP(REMOTE_ADDRESS);

        underTest.registerAuthenticatedSession(request, response, PARTNER_ID, PLAYER_PROFILE_ID,
                                               LoginResult.NEW_USER, true, WEB, CLIENT_CONTEXT_EMPTY, gameType);

        final PartnerSession expectedSession =
                new PartnerSession(REFERRER, REMOTE_ADDRESS, PARTNER_ID, WEB, LOGIN_URL);
        verify(sessionFactory).registerNewSession(BigDecimal.TEN, expectedSession, WEB, LoginResult.NEW_USER, CLIENT_CONTEXT_EMPTY);
    }

    @Test
    public void theSessionCookieIsAddedToTheRequest() {
        final Cookie sessionCookie = new Cookie("aCookie", "aValue");
        when(lobbySessionCache.generateLocalSessionCookie(null)).thenReturn(sessionCookie);
        expectASessionForPlatform(WEB);

        underTest.registerAuthenticatedSession(request, response, PARTNER_ID, PLAYER_ID,
                                               LoginResult.NEW_USER, true, WEB, CLIENT_CONTEXT_EMPTY, gameType);

        verify(response).addCookie(sessionCookie);
    }

    @Test
    public void theIsKnowCookieIsAddedToTheRequest() {
        expectASessionForPlatform(WEB);

        underTest.registerAuthenticatedSession(request, response, PARTNER_ID, PLAYER_ID,
                                               LoginResult.NEW_USER, true, WEB, CLIENT_CONTEXT_EMPTY, gameType);

        final ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response, atLeast(1)).addCookie(cookieCaptor.capture());
        boolean cookieExists = false;
        for (Cookie cookie : cookieCaptor.getAllValues()) {
            if (cookie != null && "knownUser".equals(cookie.getName())) {
                cookieExists = true;
                assertThat(cookie.getValue(), is(equalTo("true")));
                assertThat(cookie.getMaxAge(), is(equalTo(2419200)));
            }
        }
        assertThat(cookieExists, is(true));
    }

    @Test
    public void aNullSessionCookieIsNotAddedToTheRequest() {
        when(lobbySessionCache.generateLocalSessionCookie(null)).thenReturn(null);
        expectASessionForPlatform(WEB);

        underTest.registerAuthenticatedSession(request, response, PARTNER_ID, PLAYER_ID,
                                               LoginResult.NEW_USER, true, WEB, CLIENT_CONTEXT_EMPTY, gameType);

        final ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response, atLeast(1)).addCookie(cookieCaptor.capture());
        for (Cookie cookie : cookieCaptor.getAllValues()) {
            assertThat(cookie, is(not(nullValue())));
        }
    }

    @Test
    public void whenRegisteringANewSessionFailsThenATopUpRequestIsNotSentToThePromotionProcessor() {
        underTest.registerAuthenticatedSession(request, response, PARTNER_ID, PLAYER_ID,
                                               LoginResult.NEW_USER, true, WEB, CLIENT_CONTEXT_EMPTY, gameType);
        verify(queuePublishingService, never()).send(any(TopUpRequest.class));
    }

    @Test
    public void whenANewWEBSessionIsRegisteredThenATopUpRequestIsPublished() {
        expectASessionForPlatform(WEB);
        underTest.registerAuthenticatedSession(request, response, PARTNER_ID, PLAYER_ID,
                                               LoginResult.NEW_USER, true, WEB, CLIENT_CONTEXT_EMPTY, gameType);
        verify(queuePublishingService).send(new TopUpRequest(PLAYER_ID, WEB, new DateTime(), SESSION_ID));
    }

    @Test
    public void whenANewFACEBOOK_CANVASSessionIsRegisteredThenATopUpRequestIsPublished() {
        expectASessionForPlatform(FACEBOOK_CANVAS);
        underTest.registerAuthenticatedSession(request, response, PARTNER_ID, PLAYER_ID,
                                               LoginResult.NEW_USER, true, FACEBOOK_CANVAS, CLIENT_CONTEXT_EMPTY,
                                               gameType);
        verify(queuePublishingService).send(new TopUpRequest(PLAYER_ID, FACEBOOK_CANVAS, new DateTime(), SESSION_ID));
    }

    @Test
    public void whenANewIOSSessionIsRegisteredThenATopUpRequestIsPublished() {
        expectASessionForPlatform(IOS);
        underTest.registerAuthenticatedSession(request, response, PARTNER_ID, PLAYER_ID,
                                               LoginResult.NEW_USER, true, IOS, CLIENT_CONTEXT_EMPTY, gameType);
        verify(queuePublishingService).send(new TopUpRequest(PLAYER_ID, IOS, new DateTime(), SESSION_ID));
    }

    @Test
    public void whenANewAndroidSessionIsRegisteredThenATopUpRequestIsPublished() {
        expectASessionForPlatform(ANDROID);
        underTest.registerAuthenticatedSession(request, response, PARTNER_ID, PLAYER_ID,
                                               LoginResult.NEW_USER, true, ANDROID, CLIENT_CONTEXT_EMPTY, gameType);
        verify(queuePublishingService).send(new TopUpRequest(PLAYER_ID, ANDROID, new DateTime(), SESSION_ID));
    }

    @Test
    public void whenRegisteringANewSessionFailsThenThePlayersTopUpStatusRemainsCached() {
        underTest.registerAuthenticatedSession(request, response, PARTNER_ID, PLAYER_ID,
                                               LoginResult.NEW_USER, true, WEB, CLIENT_CONTEXT_EMPTY, gameType);
        verify(topUpResultService, never()).clearTopUpStatus(any(BigDecimal.class));
    }

    @Test
    public void whenANewSessionIsRegisteredThenThePlayersTopUpStatusIsRemovedFromCache() {
        expectASessionForPlatform(WEB);
        underTest.registerAuthenticatedSession(request, response, PARTNER_ID, PLAYER_ID,
                                               LoginResult.NEW_USER, true, WEB, CLIENT_CONTEXT_EMPTY, gameType);
        verify(topUpResultService).clearTopUpStatus(PLAYER_ID);
    }

    @Test
    public void lobbySessionFactoryShouldPassClientContextToSessionFactory() {
        underTest.registerAuthenticatedSession(request, response, PARTNER_ID, PLAYER_ID,
                                               LoginResult.NEW_USER, true, WEB, CLIENT_CONTEXT_MAP, gameType);

        verify(sessionFactory).registerNewSession(any(BigDecimal.class), any(PartnerSession.class), any(Platform.class), any(LoginResult.class), eq(CLIENT_CONTEXT_MAP));
    }

    private void expectASessionWithIP(final String ipAddress) {
        when(request.getRequestURL()).thenReturn(new StringBuffer(LOGIN_URL));
        final LobbySession lobbySession = new LobbySession(SESSION_ID, PLAYER_ID, "playerName", "sessionKey", YAZINO, "pictureUrl", "email", null, false,
                                                           WEB, PROVIDER);
        final PartnerSession partnerSession = new PartnerSession(REFERRER, ipAddress, PARTNER_ID, WEB, LOGIN_URL);
        when(sessionFactory.registerNewSession(PLAYER_ID, partnerSession, WEB, LoginResult.NEW_USER, CLIENT_CONTEXT_EMPTY))
                .thenReturn(new LobbySessionCreationResponse(lobbySession, false, new ReferralResult(PLAYER_ID, BigDecimal.ZERO)));
    }

    private void expectASessionForPlatform(Platform platform) {
        when(request.getRequestURL()).thenReturn(new StringBuffer(LOGIN_URL));
        final LobbySession lobbySession = new LobbySession(SESSION_ID, PLAYER_ID, "playerName", "sessionKey", YAZINO, "pictureUrl", "email", null, false, platform, PROVIDER);
        final PartnerSession partnerSession = new PartnerSession(REFERRER, REMOTE_ADDRESS, PARTNER_ID, platform, LOGIN_URL);
        when(sessionFactory.registerNewSession(PLAYER_ID, partnerSession, platform, LoginResult.NEW_USER, CLIENT_CONTEXT_EMPTY))
                .thenReturn(new LobbySessionCreationResponse(lobbySession, false, new ReferralResult(PLAYER_ID, BigDecimal.ZERO)));
    }

}
