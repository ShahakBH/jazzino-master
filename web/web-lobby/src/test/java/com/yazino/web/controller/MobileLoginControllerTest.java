package com.yazino.web.controller;

import com.yazino.bi.opengraph.OpenGraphCredentialsMessage;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.player.*;
import com.yazino.platform.player.service.AuthenticationService;
import com.yazino.platform.session.SessionClientContextKey;
import com.yazino.platform.table.TableException;
import com.yazino.web.controller.MobileLoginController.LoginInfo;
import com.yazino.web.domain.LoginResponse;
import com.yazino.web.domain.facebook.FacebookUserInformationProvider;
import com.yazino.web.form.LoginForm;
import com.yazino.web.form.WebLoginForm;
import com.yazino.web.security.LogoutHelper;
import com.yazino.web.service.ExternalWebLoginService;
import com.yazino.web.service.GameAvailability;
import com.yazino.web.service.GameAvailabilityService;
import com.yazino.web.service.RememberMeHandler;
import com.yazino.web.session.*;
import com.yazino.web.util.*;
import org.apache.http.HttpHeaders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletResponse;
import strata.server.lobby.api.facebook.FacebookAppConfiguration;
import strata.server.lobby.api.facebook.FacebookConfiguration;
import strata.server.lobby.api.promotion.DailyAwardPromotionService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.yazino.platform.Partner.TANGO;
import static com.yazino.platform.Partner.YAZINO;
import static com.yazino.platform.Platform.ANDROID;
import static com.yazino.platform.Platform.IOS;
import static com.yazino.platform.player.LoginResult.NEW_USER;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MobileLoginControllerTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.TEN;
    private static final String MOBILE_IOS_FLAG = IOS.name();
    private static final String MOBILE_ANDROID_FLAG = ANDROID.name();
    private static final String GAME_TYPE = "SLOTS";
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(3141592);
    private static final String REMOTE_IP_ADDRESS = "10.9.8.7";
    private static final String CLIENT_CONTEXT = format("{\"%s\":\"unique identifier for device\"}",
            SessionClientContextKey.DEVICE_ID.name());
    public static final HashMap<String, Object> EMPTY_CLIENT_CONTENT_MAP = new HashMap<String, Object>();
    private static final String ENCRYPTED_DATA = "scrambledeggs";

    @Mock
    private LobbySessionCache lobbySessionCache;
    @Mock
    private LobbySessionFactory lobbySessionFactory;
    @Mock
    private DailyAwardPromotionService promotionService;
    @Mock
    private ExternalWebLoginService externalWebLoginService;
    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private RememberMeHandler rememberMeHandler;
    @Mock
    private LogoutHelper logoutHelper;
    @Mock
    private PlayerInformationHolder playerInformationHolder;
    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private HttpServletResponse httpServletResponse;
    @Mock
    private QueuePublishingService<OpenGraphCredentialsMessage> openGraphCredentialsService;
    @Mock
    private MobileRequestGameGuesser requestGameGuesser;
    @Mock
    private GameAvailabilityService gameAvailabilityService;
    @Mock
    private YazinoConfiguration yazinoConfiguration;
    @Mock
    private TangoPlayerInformationProvider tangoPlayerInformationProvider;
    @Mock
    private SymmetricEncryptor encryptor;

    private final JsonHelper jsonHelper = new JsonHelper();
    private final FacebookConfiguration facebookConfiguration = new FacebookConfiguration();
    private final StringWriter stringWriter = new StringWriter();
    private final WebApiResponses responseWriter = new WebApiResponses(new WebApiResponseWriter());
    private final String gameType = "SLOTS";

    private MobileLoginController underTest;

    @Before
    public void setup() throws IOException {
        final FacebookUserInformationProvider userInformationProvider = mock(FacebookUserInformationProvider.class);
        when(userInformationProvider.getUserInformationHolder(anyString(), anyString(),
                eq(REMOTE_IP_ADDRESS), eq(true))).thenReturn(playerInformationHolder);

        final FacebookAppConfiguration facebookAppConfiguration = new FacebookAppConfiguration();
        facebookAppConfiguration.setGameType(GAME_TYPE);
        facebookAppConfiguration.setCanvasActionsAllowed(true);
        facebookConfiguration.setApplicationConfigs(Arrays.asList(facebookAppConfiguration));
        final PlayerProfile profile = new PlayerProfile();
        profile.setProviderName("FACEBOOK");
        profile.setExternalId("1234");
        when(playerInformationHolder.getPlayerProfile()).thenReturn(profile);

        when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/public/login"));
        when(httpServletRequest.getRemoteAddr()).thenReturn(REMOTE_IP_ADDRESS);
        when(gameAvailabilityService.getAvailabilityOfGameType(any(String.class))).thenReturn(new GameAvailability(GameAvailabilityService.Availability.AVAILABLE));
        when(requestGameGuesser.guessGame(any(HttpServletRequest.class), any(Platform.class))).thenReturn(GAME_TYPE);
        when(yazinoConfiguration.getString("strata.lobby.partnerid", "YAZINO")).thenReturn("YAZINO");

        underTest = new MobileLoginController(lobbySessionCache, lobbySessionFactory,
                facebookConfiguration, userInformationProvider,
                externalWebLoginService, authenticationService, openGraphCredentialsService, rememberMeHandler,
                logoutHelper, requestGameGuesser, gameAvailabilityService, responseWriter, yazinoConfiguration,
                tangoPlayerInformationProvider);
    }

    @Test
    public void shouldReturnFailedLoginInfoWhenMobileFacebookUserIsBlocked() throws IOException {
        when(httpServletResponse.getWriter()).thenReturn(new PrintWriter(stringWriter));
        when(externalWebLoginService.login(eq(httpServletRequest), eq(httpServletResponse), eq(Partner.YAZINO),
                anyString(), eq(playerInformationHolder),
                eq(true), eq(IOS), eq(EMPTY_CLIENT_CONTENT_MAP))).thenReturn(new LoginResponse(LoginResult.BLOCKED));
        underTest.mobileFacebook(httpServletRequest, httpServletResponse, GAME_TYPE, "1234", "IOS", true);
        final MobileLoginController.LoginInfo actual =
                jsonHelper.deserialize(MobileLoginController.LoginInfo.class, stringWriter.toString());
        assertFalse(actual.isSuccess());
        assertEquals("Blocked", actual.getError());
    }

    @Test
    public void shouldReturnFailedLoginInfoWhenMobileFacebookUserSessionRegistrationFails() throws IOException {
        when(httpServletResponse.getWriter()).thenReturn(new PrintWriter(stringWriter));
        when(playerInformationHolder.getPlayerProfile()).thenReturn(null);

        when(externalWebLoginService.login(eq(httpServletRequest), eq(httpServletResponse), eq(Partner.YAZINO),
                anyString(), eq(playerInformationHolder),
                eq(true), eq(IOS), eq(EMPTY_CLIENT_CONTENT_MAP))).thenReturn(new LoginResponse(LoginResult.FAILURE));
        underTest.mobileFacebook(httpServletRequest, httpServletResponse, GAME_TYPE, "1234", "IOS", true);
        final MobileLoginController.LoginInfo actual =
                jsonHelper.deserialize(MobileLoginController.LoginInfo.class, stringWriter.toString());
        assertFalse(actual.isSuccess());
        assertEquals("Failed", actual.getError());
    }

    @Test
    public void shouldReturnSuccessfulLoginInfoWhenMobileFacebookUserSessionRegistrationSucceeds() throws IOException {
        when(httpServletResponse.getWriter()).thenReturn(new PrintWriter(stringWriter));
        when(lobbySessionCache.getActiveSession(httpServletRequest)).thenReturn(
                new LobbySession(SESSION_ID, BigDecimal.ZERO, "", "", Partner.YAZINO, "", "", null, false, IOS, AuthProvider.YAZINO));
        when(externalWebLoginService.login(eq(httpServletRequest), eq(httpServletResponse), eq(Partner.YAZINO),
                anyString(), eq(playerInformationHolder),
                eq(true), eq(IOS), eq(EMPTY_CLIENT_CONTENT_MAP))).thenReturn(new LoginResponse(NEW_USER, aLobbySession()));
        underTest.mobileFacebook(httpServletRequest, httpServletResponse, GAME_TYPE, "1234", "IOS", true);
        final MobileLoginController.LoginInfo actual =
                jsonHelper.deserialize(MobileLoginController.LoginInfo.class, stringWriter.toString());
        assertTrue(actual.isSuccess());
    }

    @Test
    public void shouldReturnSuccessfulLoginInfoWhenAndroidMobileFacebookUserSessionRegistrationSucceeds() throws IOException {
        when(httpServletResponse.getWriter()).thenReturn(new PrintWriter(stringWriter));
        when(lobbySessionCache.getActiveSession(httpServletRequest)).thenReturn(
                new LobbySession(SESSION_ID, BigDecimal.ZERO, "", "", Partner.YAZINO, "", "", null, false, ANDROID, AuthProvider.YAZINO));
        when(externalWebLoginService.login(eq(httpServletRequest), eq(httpServletResponse), eq(Partner.YAZINO),
                anyString(), eq(playerInformationHolder),
                eq(true), eq(ANDROID), eq(EMPTY_CLIENT_CONTENT_MAP))).thenReturn(new LoginResponse(NEW_USER, aLobbySession()));
        underTest.mobileFacebook(httpServletRequest, httpServletResponse, GAME_TYPE, "1234", MOBILE_ANDROID_FLAG, true
        );
        final MobileLoginController.LoginInfo actual =
                jsonHelper.deserialize(MobileLoginController.LoginInfo.class, stringWriter.toString());
        assertTrue(actual.isSuccess());
    }

    @Test
    public void shouldSendAccessTokenToOpenGraphServiceWhenMobileFacebookUserSessionRegistrationSucceeds() throws IOException {
        BigDecimal playerId = BigDecimal.ZERO;
        String gameType = GAME_TYPE;
        String accessToken = "1234";
        when(httpServletResponse.getWriter()).thenReturn(new PrintWriter(stringWriter));
        when(lobbySessionCache.getActiveSession(httpServletRequest)).thenReturn(
                new LobbySession(SESSION_ID, playerId, "", "", Partner.YAZINO, "", "", null, false, IOS, AuthProvider.YAZINO));
        when(externalWebLoginService.login(eq(httpServletRequest), eq(httpServletResponse), eq(Partner.YAZINO),
                anyString(), eq(playerInformationHolder),
                eq(true), eq(IOS), eq(EMPTY_CLIENT_CONTENT_MAP))).thenReturn(new LoginResponse(NEW_USER, aLobbySession()));

        underTest.mobileFacebook(httpServletRequest, httpServletResponse, gameType, accessToken, MOBILE_IOS_FLAG, true
        );

        verify(openGraphCredentialsService).send(new OpenGraphCredentialsMessage(playerId.toBigInteger(), gameType, accessToken));
    }

    @Test
    public void shouldNotPropagateExceptionsThrownByOpenGraphService() throws IOException {
        BigDecimal playerId = BigDecimal.ZERO;
        String gameType = GAME_TYPE;
        String accessToken = "1234";
        when(httpServletResponse.getWriter()).thenReturn(new PrintWriter(stringWriter));
        when(lobbySessionCache.getActiveSession(httpServletRequest)).thenReturn(
                new LobbySession(SESSION_ID, playerId, "", "", Partner.YAZINO, "", "", null, false, IOS, AuthProvider.YAZINO));
        when(externalWebLoginService.login(eq(httpServletRequest), eq(httpServletResponse), eq(Partner.YAZINO),
                anyString(), eq(playerInformationHolder),
                eq(true), eq(IOS), eq(EMPTY_CLIENT_CONTENT_MAP))).thenReturn(new LoginResponse(NEW_USER, aLobbySession()));
        doThrow(new RuntimeException("sample exception")).
                when(openGraphCredentialsService).send(any(OpenGraphCredentialsMessage.class));

        underTest.mobileFacebook(httpServletRequest, httpServletResponse, gameType, accessToken, MOBILE_IOS_FLAG, true);
    }

    @Test
    public void shouldReportPlatformOnPlainMobileLogins() throws IOException {
        // GIVEN a successful authentication
        final PlayerProfileAuthenticationResponse successfulAuthentication
                = new PlayerProfileAuthenticationResponse(PLAYER_ID);
        given(authenticationService.authenticateYazinoUser(
                any(String.class), any(String.class))).willReturn(successfulAuthentication);

        // AND a mail address provided by the form
        final LoginForm form = mock(WebLoginForm.class);
        final String emailAddress = "machin@bidule.com";
        given(form.getEmail()).willReturn(emailAddress);

        // AND there is no open session reported
        given(lobbySessionCache.getActiveSession(httpServletRequest)).willReturn(null);

        // AND the servlet response returns a writer mock
        final PrintWriter responseWriter = mock(PrintWriter.class);
        given(httpServletResponse.getWriter()).willReturn(responseWriter);

        // WHEN asking for plain mobile login
        underTest.mobileYazino(httpServletRequest, httpServletResponse, form, MOBILE_IOS_FLAG, true, null);

        // THEN the correct platform is reported to the session factory
        verify(lobbySessionFactory).registerAuthenticatedSession(eq(httpServletRequest), eq(httpServletResponse),
                eq(Partner.YAZINO), eq(PLAYER_ID),
                eq(LoginResult.EXISTING_USER), eq(true), eq(IOS), eq(EMPTY_CLIENT_CONTENT_MAP), eq(gameType));
    }

    @Test
    public void shouldReportCorrectPlatformOnPlainAndroidLogins() throws IOException {
        // GIVEN a successful authentication
        final PlayerProfileAuthenticationResponse successfulAuthentication
                = new PlayerProfileAuthenticationResponse(PLAYER_ID);
        given(authenticationService.authenticateYazinoUser(
                any(String.class), any(String.class))).willReturn(successfulAuthentication);

        // AND a mail address provided by the form
        final LoginForm form = mock(WebLoginForm.class);
        final String emailAddress = "machin@bidule.com";
        given(form.getEmail()).willReturn(emailAddress);

        // AND there is no open session reported
        given(lobbySessionCache.getActiveSession(httpServletRequest)).willReturn(null);

        // AND the servlet response returns a writer mock
        final PrintWriter responseWriter = mock(PrintWriter.class);
        given(httpServletResponse.getWriter()).willReturn(responseWriter);

        // WHEN asking for plain mobile login
        underTest.mobileYazino(httpServletRequest, httpServletResponse, form, MOBILE_ANDROID_FLAG, true, null);

        // THEN the correct platform is reported to the session factory
        verify(lobbySessionFactory).registerAuthenticatedSession(eq(httpServletRequest), eq(httpServletResponse),
                eq(Partner.YAZINO), eq(PLAYER_ID),
                eq(LoginResult.EXISTING_USER), eq(true), eq(ANDROID), eq(EMPTY_CLIENT_CONTENT_MAP), eq(gameType));
    }

    @Test
    public void shouldReportLoginFailure() throws IOException {
        // GIVEN a failed authentication
        final PlayerProfileAuthenticationResponse failedAuthentication = new PlayerProfileAuthenticationResponse();
        given(authenticationService.authenticateYazinoUser(
                any(String.class), any(String.class))).willReturn(failedAuthentication);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        given(httpServletResponse.getWriter()).willReturn(new PrintWriter(out));

        // WHEN asking for a login
        underTest.mobileYazino(httpServletRequest, httpServletResponse, new WebLoginForm(), MOBILE_IOS_FLAG, true, null);

        // THEN the failure is reported to the response channel
        final LoginInfo info = new LoginInfo();
        info.setError("Your username and/or password were incorrect.");
        assertThat(out.toString(), is(equalTo(new JsonHelper().serialize(info))));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReportExistingSessionAndNotGiveAFuck() throws IOException {
        // GIVEN a lobby session is already cached
        final LobbySession session = new LobbySession(SESSION_ID,
                BigDecimal.TEN, "playerName", "", Partner.YAZINO, "", "", null, false, IOS, AuthProvider.YAZINO);
        given(lobbySessionCache.getActiveSession(httpServletRequest)).willReturn(session);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        given(httpServletResponse.getWriter()).willReturn(new PrintWriter(out));

        // AND there is a parameter in the request requiring the topup
        given(httpServletRequest.getParameterMap()).willReturn(
                Collections.singletonMap("doTopup", new String[]{"false"}));
        given(httpServletRequest.getParameter("doTopup")).willReturn("false");

        // WHEN asking for a login
        underTest.mobileYazino(httpServletRequest, httpServletResponse, new WebLoginForm(), MOBILE_IOS_FLAG, true, null);

        // THEN the failure is reported to the response channel
        final LoginInfo info = new LoginInfo();
        info.setSuccess(true);
        info.setName("playerName");
        info.setPlayerId(BigDecimal.TEN);
        info.setSession(new LobbySessionReference(session).encode());
        info.setAvailability(GameAvailabilityService.Availability.AVAILABLE);
        assertThat(out.toString(), is(equalTo(jsonHelper.serialize(info))));
    }

    @Test
    public void shouldAddCorrectRequestURLFromUserAgent() throws Exception {
        when(lobbySessionCache.getActiveSession(httpServletRequest)).thenReturn(aLobbySession());
        when(httpServletResponse.getWriter()).thenReturn(new PrintWriter(stringWriter));
        when(requestGameGuesser.guessGame(any(HttpServletRequest.class), any(Platform.class))).thenReturn("BLACKJACK");

        when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/publicCommand/mobile/YAZINO"));
        when(httpServletRequest.getHeader(HttpHeaders.USER_AGENT)).thenReturn("Blackjack 2.0 rv:2.0-beta1 (iPhone; iPhone OS 5.1.1; en_GB)");
        underTest.mobileYazino(httpServletRequest, httpServletResponse, new WebLoginForm(), MOBILE_IOS_FLAG, true, null);
        verify(httpServletRequest).setAttribute(PlatformReportingHelper.REQUEST_URL, "http://localhost/public/login/IOS/BLACKJACK/YAZINO");
    }

    @Test
    public void shouldAddRequestAttributeWithCorrectStartPageURLForIOSWheelDealOnYazino() throws Exception {
        when(lobbySessionCache.getActiveSession(httpServletRequest)).thenReturn(aLobbySession());
        when(httpServletResponse.getWriter()).thenReturn(new PrintWriter(stringWriter));

        String userAgent = "Wheel Deal 3.0 rv:3.0-beta1 (iPhone; iPhone OS 5.1.1; en_GB)";
        when(httpServletRequest.getHeader(HttpHeaders.USER_AGENT)).thenReturn(userAgent);
        when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer("http://www.yazino.com/publicCommand/mobile/YAZINO"));
        underTest.mobileYazino(httpServletRequest, httpServletResponse, new WebLoginForm(), "IOS", false, null);
        verify(httpServletRequest).setAttribute(PlatformReportingHelper.REQUEST_URL, "http://www.yazino.com/public/login/IOS/SLOTS/YAZINO");
    }

    @Test
    public void shouldAddRequestAttributeWithCorrectStartPageURLForIOSWheelDealOnFacebook() throws Exception {
        when(lobbySessionCache.getActiveSession(httpServletRequest)).thenReturn(aLobbySession());
        when(httpServletResponse.getWriter()).thenReturn(new PrintWriter(stringWriter));
        when(externalWebLoginService.login(eq(httpServletRequest), eq(httpServletResponse), eq(Partner.YAZINO),
                anyString(), eq(playerInformationHolder),
                anyBoolean(), eq(IOS), eq(EMPTY_CLIENT_CONTENT_MAP))).thenReturn(new LoginResponse(NEW_USER, aLobbySession()));

        String userAgent = "Wheel Deal 3.0 rv:3.0-beta1 (iPhone; iPhone OS 5.1.1; en_GB)";
        when(httpServletRequest.getHeader(HttpHeaders.USER_AGENT)).thenReturn(userAgent);
        when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer("http://www.yazino.com/publicCommand/mobile/FACEBOOK_CANVAS"));
        underTest.mobileFacebook(httpServletRequest, httpServletResponse, GAME_TYPE, "12345", "IOS", false);
        verify(httpServletRequest).setAttribute(PlatformReportingHelper.REQUEST_URL,
                "http://www.yazino.com/public/login/IOS/SLOTS/FACEBOOK_CANVAS");
    }

    @Test
    public void shouldAddRequestAttributeWithCorrectStartPageURLForIOSBlackjackOnYazino() throws Exception {
        when(lobbySessionCache.getActiveSession(httpServletRequest)).thenReturn(aLobbySession());
        when(httpServletResponse.getWriter()).thenReturn(new PrintWriter(stringWriter));
        when(requestGameGuesser.guessGame(any(HttpServletRequest.class), any(Platform.class))).thenReturn("BLACKJACK");

        String userAgent = "Blackjack 2.0 rv:2.0-beta1 (iPhone; iPhone OS 5.1.1; en_GB)";
        when(httpServletRequest.getHeader(HttpHeaders.USER_AGENT)).thenReturn(userAgent);
        when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer("http://www.yazino.com/publicCommand/mobile/YAZINO"));
        underTest.mobileYazino(httpServletRequest, httpServletResponse, new WebLoginForm(), "IOS", false, null);
        verify(httpServletRequest).setAttribute(PlatformReportingHelper.REQUEST_URL,
                "http://www.yazino.com/public/login/IOS/BLACKJACK/YAZINO");
    }

    @Test
    public void shouldAddRequestAttributeWithCorrectStartPageURLForIOSBlackjackOnFacebook() throws Exception {
        when(lobbySessionCache.getActiveSession(httpServletRequest)).thenReturn(aLobbySession());
        when(httpServletResponse.getWriter()).thenReturn(new PrintWriter(stringWriter));
        when(externalWebLoginService.login(eq(httpServletRequest), eq(httpServletResponse), eq(Partner.YAZINO),
                anyString(), eq(playerInformationHolder),
                anyBoolean(), eq(IOS), eq(EMPTY_CLIENT_CONTENT_MAP))).thenReturn(new LoginResponse(NEW_USER, aLobbySession()));

        String userAgent = "Blackjack 2.0 rv:2.0-beta1 (iPhone; iPhone OS 5.1.1; en_GB)";
        when(httpServletRequest.getHeader(HttpHeaders.USER_AGENT)).thenReturn(userAgent);
        when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer("http://www.yazino.com/publicCommand/mobile/FACEBOOK_CANVAS"));
        underTest.mobileFacebook(httpServletRequest, httpServletResponse, "BLACKJACK", "123456", "IOS", false);
        verify(httpServletRequest).setAttribute(PlatformReportingHelper.REQUEST_URL,
                "http://www.yazino.com/public/login/IOS/BLACKJACK/FACEBOOK_CANVAS");
    }


    @Test
    public void shouldAddRequestAttributeWithCorrectStartPageURLForIOSHighStakesOnYazino() throws Exception {
        when(lobbySessionCache.getActiveSession(httpServletRequest)).thenReturn(aLobbySession());
        when(httpServletResponse.getWriter()).thenReturn(new PrintWriter(stringWriter));
        when(requestGameGuesser.guessGame(any(HttpServletRequest.class), any(Platform.class))).thenReturn("HIGH_STAKES");

        String userAgent = "High Stakes 1.0 (beta-2) rv:1.0 (iPhone; iPhone OS 5.1.1; en_GB)";
        when(httpServletRequest.getHeader(HttpHeaders.USER_AGENT)).thenReturn(userAgent);
        when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer("http://www.yazino.com/publicCommand/mobile/YAZINO"));
        underTest.mobileYazino(httpServletRequest, httpServletResponse, new WebLoginForm(), "IOS", false, null);
        verify(httpServletRequest).setAttribute(PlatformReportingHelper.REQUEST_URL,
                "http://www.yazino.com/public/login/IOS/HIGH_STAKES/YAZINO");
    }

    @Test
    public void shouldAddRequestAttributeWithCorrectStartPageURLForIOSHighStakesOnFacebook() throws Exception {
        when(lobbySessionCache.getActiveSession(httpServletRequest)).thenReturn(aLobbySession());
        when(httpServletResponse.getWriter()).thenReturn(new PrintWriter(stringWriter));
        when(externalWebLoginService.login(eq(httpServletRequest), eq(httpServletResponse), eq(Partner.YAZINO),
                anyString(), eq(playerInformationHolder),
                anyBoolean(), eq(IOS), eq(EMPTY_CLIENT_CONTENT_MAP))).thenReturn(new LoginResponse(NEW_USER, aLobbySession()));

        String userAgent = "High Stakes 1.0 (beta-2) rv:1.0 (iPhone; iPhone OS 5.1.1; en_GB)";
        when(httpServletRequest.getHeader(HttpHeaders.USER_AGENT)).thenReturn(userAgent);
        when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer("http://www.yazino.com/publicCommand/mobile/FACEBOOK_CANVAS"));
        underTest.mobileFacebook(httpServletRequest, httpServletResponse, "HIGH_STAKES", "123456", "IOS", false);
        verify(httpServletRequest).setAttribute(PlatformReportingHelper.REQUEST_URL,
                "http://www.yazino.com/public/login/IOS/HIGH_STAKES/FACEBOOK_CANVAS");
    }


    @Test
    public void shouldAddRequestAttributeWithCorrectStartPageURLForAndroidTexasHoldemOnYazino() throws Exception {
        when(lobbySessionCache.getActiveSession(httpServletRequest)).thenReturn(aLobbySession());
        when(httpServletResponse.getWriter()).thenReturn(new PrintWriter(stringWriter));
        when(requestGameGuesser.guessGame(any(HttpServletRequest.class), any(Platform.class))).thenReturn("TEXAS_HOLDEM");

        String userAgent = "Mozilla/5.0 (Android; U; en-GB) AppleWebKit/533.19.4 (KHTML, like Gecko) AdobeAIR/3.3";
        when(httpServletRequest.getHeader(HttpHeaders.USER_AGENT)).thenReturn(userAgent);
        when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer("http://www.yazino.com/publicCommand/mobile/YAZINO"));
        underTest.mobileYazino(httpServletRequest, httpServletResponse, new WebLoginForm(), "ANDROID", false, null);
        verify(httpServletRequest).setAttribute(PlatformReportingHelper.REQUEST_URL,
                "http://www.yazino.com/public/login/ANDROID/TEXAS_HOLDEM/YAZINO");
    }

    @Test
    public void shouldAddRequestAttributeWithCorrectStartPageURLForAndroidTexasHoldemOnFacebook() throws Exception {
        when(lobbySessionCache.getActiveSession(httpServletRequest)).thenReturn(aLobbySession());
        when(httpServletResponse.getWriter()).thenReturn(new PrintWriter(stringWriter));
        when(externalWebLoginService.login(eq(httpServletRequest), eq(httpServletResponse), eq(Partner.YAZINO),
                anyString(), eq(playerInformationHolder),
                anyBoolean(), eq(ANDROID), eq(EMPTY_CLIENT_CONTENT_MAP))).thenReturn(new LoginResponse(NEW_USER, aLobbySession()));

        String userAgent = "Mozilla/5.0 (Android; U; en-GB) AppleWebKit/533.19.4 (KHTML, like Gecko) AdobeAIR/3.3";
        when(httpServletRequest.getHeader(HttpHeaders.USER_AGENT)).thenReturn(userAgent);
        when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer("http://www.yazino.com/publicCommand/mobile/FACEBOOK_CANVAS"));
        underTest.mobileFacebook(httpServletRequest, httpServletResponse, "TEXAS_HOLDEM", "123456", "ANDROID", false
        );
        verify(httpServletRequest).setAttribute(PlatformReportingHelper.REQUEST_URL,
                "http://www.yazino.com/public/login/ANDROID/TEXAS_HOLDEM/FACEBOOK_CANVAS");
    }

    @Test
    public void shouldSetStatusCodeTo400WhenFacebookAccessTokenIsNull() throws Exception {
        underTest.mobileFacebook(httpServletRequest, httpServletResponse, GAME_TYPE, null, IOS.name(), true);
        assertResponseStatusIsSetTo400();
        Mockito.reset(httpServletResponse);
        underTest.loginWithFacebook(httpServletRequest, httpServletResponse, IOS.name(), GAME_TYPE, null, true, CLIENT_CONTEXT);
        assertResponseStatusIsSetTo400();
    }

    @Test
    public void shouldSetStatusCodeTo400WhenFacebookAccessTokenIsEmpty() throws Exception {
        underTest.mobileFacebook(httpServletRequest, httpServletResponse, GAME_TYPE, "", IOS.name(), true);
        assertResponseStatusIsSetTo400();
        Mockito.reset(httpServletResponse);
        underTest.loginWithFacebook(httpServletRequest, httpServletResponse, IOS.name(), GAME_TYPE, "", true, CLIENT_CONTEXT);
        assertResponseStatusIsSetTo400();
    }

    @Test
    public void shouldSetStatusCodeTo400WhenFacebookAccessTokenIsBlank() throws Exception {
        underTest.mobileFacebook(httpServletRequest, httpServletResponse, GAME_TYPE, "   ", IOS.name(), true);
        assertResponseStatusIsSetTo400();
        Mockito.reset(httpServletResponse);
        underTest.loginWithFacebook(httpServletRequest, httpServletResponse, IOS.name(), GAME_TYPE, "   ", true, CLIENT_CONTEXT);
        assertResponseStatusIsSetTo400();
    }

    @Test
    public void shouldWriteNewPlayerIsTrueToResponseWhenRegisteringANewFacebookPlayer() throws IOException {
        MockHttpServletResponse response = setUpSuccessfulFacebookLogin(NEW_USER);

        underTest.loginWithFacebook(httpServletRequest, response, ANDROID.name(), GAME_TYPE, "1234", true, CLIENT_CONTEXT);

        final String content = response.getContentAsString();
        final LoginInfo actualLoginInfo = new JsonHelper().deserialize(LoginInfo.class, content);
        assertThat(actualLoginInfo.isNewPlayer(), is(true));
    }

    @Test
    public void shouldWriteNewPlayerIsFalseToResponseWhenLoggingInAnExistingNewFacebookPlayer() throws IOException {
        MockHttpServletResponse response = setUpSuccessfulFacebookLogin(LoginResult.EXISTING_USER);

        underTest.loginWithFacebook(httpServletRequest, response, ANDROID.name(), GAME_TYPE, "1234", true, CLIENT_CONTEXT);

        final String content = response.getContentAsString();
        final LoginInfo actualLoginInfo = new JsonHelper().deserialize(LoginInfo.class, content);
        assertThat(actualLoginInfo.isNewPlayer(), is(false));
    }

    @Test
    public void shouldWriteNewPlayerIsNullToResponseWhenLoggingWithYazino() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        final PlayerProfileAuthenticationResponse successfulAuthentication
                = new PlayerProfileAuthenticationResponse(PLAYER_ID);
        given(authenticationService.authenticateYazinoUser(
                any(String.class), any(String.class))).willReturn(successfulAuthentication);
        final LoginForm form = new LoginForm();
        form.setEmail("anemail@address.com");
        given(lobbySessionCache.getActiveSession(httpServletRequest)).willReturn(null);
        given(lobbySessionFactory.registerAuthenticatedSession(eq(httpServletRequest), eq(response), eq(Partner.YAZINO), eq(PLAYER_ID),
                eq(LoginResult.EXISTING_USER), eq(true), eq(IOS), anyMap(), eq(gameType))).willReturn(new LobbySession(SESSION_ID,
                BigDecimal.ZERO,
                "",
                "",
                Partner.YAZINO,
                "",
                "",
                null, false,
                IOS,
                AuthProvider.YAZINO));

        // WHEN asking for plain mobile login
        underTest.loginWithYazino(httpServletRequest, response, form, IOS.name(), GAME_TYPE, true, CLIENT_CONTEXT, null);

        final String content = response.getContentAsString();
        final LoginInfo actualLoginInfo = new JsonHelper().deserialize(LoginInfo.class, content);
        assertNull(actualLoginInfo.isNewPlayer());
    }

    @Test
    public void shouldIncludeAvailabilityOfGameTypeInSuccessResponse() throws IOException, TableException {
        GameAvailability value = new GameAvailability(GameAvailabilityService.Availability.AVAILABLE);
        when(gameAvailabilityService.getAvailabilityOfGameType(GAME_TYPE)).thenReturn(value);
        MockHttpServletResponse response = setUpSuccessfulFacebookLogin(LoginResult.EXISTING_USER);

        underTest.loginWithFacebook(httpServletRequest, response, ANDROID.name(), GAME_TYPE, "1234", true, CLIENT_CONTEXT);

        final String content = response.getContentAsString();
        final LoginInfo actualLoginInfo = new JsonHelper().deserialize(LoginInfo.class, content);
        assertThat(actualLoginInfo.getAvailability(), equalTo(GameAvailabilityService.Availability.AVAILABLE));
    }

    @Test
    public void loginWithYazinoShouldPassClientContentMapToLobbySessionFactory() throws IOException {

        Map<String, Object> expectedClientContextMap = ClientContextConverter.toMap(CLIENT_CONTEXT);

        MockHttpServletResponse response = new MockHttpServletResponse();
        final PlayerProfileAuthenticationResponse successfulAuthentication
                = new PlayerProfileAuthenticationResponse(PLAYER_ID);
        given(authenticationService.authenticateYazinoUser(
                any(String.class), any(String.class))).willReturn(successfulAuthentication);
        final LoginForm form = new LoginForm();
        form.setEmail("anemail@address.com");
        given(lobbySessionCache.getActiveSession(httpServletRequest)).willReturn(null);
        given(lobbySessionFactory.registerAuthenticatedSession(eq(httpServletRequest), eq(response), eq(Partner.YAZINO), eq(PLAYER_ID),
                eq(LoginResult.EXISTING_USER), eq(true), eq(IOS), eq(expectedClientContextMap), eq(gameType))).willReturn(new LobbySession(SESSION_ID,
                BigDecimal.ZERO,
                "",
                "",
                Partner.YAZINO,
                "",
                "",
                null, false,
                IOS,
                AuthProvider.YAZINO));

        // WHEN asking for plain mobile login
        underTest.loginWithYazino(httpServletRequest, response, form, IOS.name(), GAME_TYPE, true, CLIENT_CONTEXT, null);
    }

    @Test
    public void failureToDecryptShouldBlowUpWith401() throws IOException, GeneralSecurityException {
        when(tangoPlayerInformationProvider.getPlayerInformationFromEncryptedData(ENCRYPTED_DATA)).thenThrow(new GeneralSecurityException("Braaaap"));
        underTest.loginWithTango(httpServletRequest,
                httpServletResponse,
                IOS.name(),
                GAME_TYPE,
                true,
                CLIENT_CONTEXT,
                ENCRYPTED_DATA);

        verify(httpServletResponse).sendError(HttpServletResponse.SC_UNAUTHORIZED, "could not decrypt");
        verifyZeroInteractions(externalWebLoginService, lobbySessionCache, lobbySessionFactory, authenticationService);

    }

    @Test
    public void invalidTangoLoginJsonShouldReturn400() throws IOException, GeneralSecurityException {
        final String json = "unscrambled";
        when(encryptor.decrypt(ENCRYPTED_DATA)).thenReturn(json);
        when(tangoPlayerInformationProvider.getPlayerInformationFromEncryptedData(ENCRYPTED_DATA))
                .thenThrow(new RuntimeException("JSON deserialization error "));
        underTest.loginWithTango(httpServletRequest,
                httpServletResponse,
                IOS.name(),
                GAME_TYPE,
                true,
                CLIENT_CONTEXT,
                ENCRYPTED_DATA);

        verify(httpServletResponse).sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid json:JSON deserialization error ");
        verifyZeroInteractions(externalWebLoginService, lobbySessionCache, lobbySessionFactory, authenticationService);
    }

    @Test
    public void shouldReturnSuccessfulLoginInfoWhenTangoUserSessionRegistrationSucceeds() throws IOException, GeneralSecurityException {
        Map<String, Object> expectedClientContextMap = ClientContextConverter.toMap(CLIENT_CONTEXT);

        when(httpServletResponse.getWriter()).thenReturn(new PrintWriter(stringWriter));
        when(lobbySessionCache.getActiveSession(httpServletRequest)).thenReturn(
                new LobbySession(SESSION_ID, BigDecimal.ZERO, "", "", TANGO, "", "", null, false, IOS, AuthProvider.TANGO));

        PlayerInformationHolder tangoHolder = setupPlayerHolder();
        when(tangoPlayerInformationProvider.getPlayerInformationFromEncryptedData(ENCRYPTED_DATA)).thenReturn(tangoHolder);

        setupPlayerHolder();
        when(externalWebLoginService.login(eq(httpServletRequest),
                        eq(httpServletResponse),
                        eq(TANGO),
                        eq(GAME_TYPE),
                        eq(tangoHolder),
                        eq(true),
                        eq(IOS),
                        eq(expectedClientContextMap))
        ).thenReturn(new LoginResponse(LoginResult.NEW_USER, aLobbySession()));

        underTest.loginWithTango(httpServletRequest,
                httpServletResponse,
                IOS.name(),
                GAME_TYPE,
                true,
                CLIENT_CONTEXT,
                ENCRYPTED_DATA);

        final MobileLoginController.LoginInfo actual = jsonHelper.deserialize(MobileLoginController.LoginInfo.class,
                stringWriter.toString());

        assertTrue(actual.isSuccess());
    }

    private PlayerInformationHolder setupPlayerHolder() {
        PlayerInformationHolder tangoHolder = new PlayerInformationHolder();
        final PlayerProfile tangoProfile = new PlayerProfile();
        tangoProfile.setProviderName("TANGO");
        tangoProfile.setRpxProvider("TANGO");
        tangoProfile.setPartnerId(TANGO);
        tangoProfile.setExternalId("666");
        tangoProfile.setDisplayName("Jim");
        tangoProfile.setGuestStatus(GuestStatus.NON_GUEST);
        tangoHolder.setPlayerProfile(tangoProfile);
        tangoHolder.setAvatarUrl("http://your.mum/so/fat.gif");
        return tangoHolder;
    }

    @Test
    public void loginWithYazinoShouldPassPlatformToLobbySessionFactory() throws IOException {

        Map<String, Object> expectedClientContextMap = ClientContextConverter.toMap(CLIENT_CONTEXT);

        MockHttpServletResponse response = new MockHttpServletResponse();
        final PlayerProfileAuthenticationResponse successfulAuthentication
                = new PlayerProfileAuthenticationResponse(PLAYER_ID);
        given(authenticationService.authenticateYazinoUser(
                any(String.class), any(String.class))).willReturn(successfulAuthentication);
        final LoginForm form = new LoginForm();
        form.setEmail("anemail@address.com");
        given(lobbySessionCache.getActiveSession(httpServletRequest)).willReturn(null);
        given(lobbySessionFactory.registerAuthenticatedSession(eq(httpServletRequest), eq(response), eq(TANGO), eq(PLAYER_ID),
                eq(LoginResult.EXISTING_USER), eq(true), eq(IOS), eq(expectedClientContextMap), eq(gameType))).willReturn(new LobbySession(SESSION_ID,
                BigDecimal.ZERO, "", "", TANGO, "", "", null, false, IOS, AuthProvider.YAZINO));

        // WHEN asking for plain mobile login
        underTest.loginWithYazino(httpServletRequest, response, form, IOS.name(), GAME_TYPE, true, CLIENT_CONTEXT, "Tango");

    }

    @Test
    public void loginWithFacebookShouldPassClientContentMapToExternalWebLoginService() throws IOException {
        Map<String, Object> expectedClientContextMap = ClientContextConverter.toMap(CLIENT_CONTEXT);
        MockHttpServletResponse response = setUpSuccessfulFacebookLogin(LoginResult.EXISTING_USER);

        underTest.loginWithFacebook(httpServletRequest, response, ANDROID.name(), GAME_TYPE, "1234", true, CLIENT_CONTEXT);

        verify(externalWebLoginService).login(eq(httpServletRequest),
                eq(response), eq(YAZINO), anyString(), any(PlayerInformationHolder.class), anyBoolean(),
                any(Platform.class),
                eq(expectedClientContextMap));

    }

    @Test
    public void shouldSetMaintenanceCountdownInSuccessResponseWhenMaintenanceScheduledForSpecifiedGame() throws TableException, IOException {
        Long expectedCountdown = 10L;
        GameAvailability value = new GameAvailability(GameAvailabilityService.Availability.MAINTENANCE_SCHEDULED, expectedCountdown);
        when(gameAvailabilityService.getAvailabilityOfGameType(GAME_TYPE)).thenReturn(value);
        MockHttpServletResponse response = setUpSuccessfulFacebookLogin(LoginResult.EXISTING_USER);

        underTest.loginWithFacebook(httpServletRequest, response, ANDROID.name(), GAME_TYPE, "1234", true, CLIENT_CONTEXT);

        final String content = response.getContentAsString();
        final LoginInfo actualLoginInfo = new JsonHelper().deserialize(LoginInfo.class, content);
        assertThat(actualLoginInfo.getAvailability(), equalTo(GameAvailabilityService.Availability.MAINTENANCE_SCHEDULED));
        assertThat(actualLoginInfo.getMaintenanceStartsAtMillis(), equalTo(10L));
    }

    @Test
    public void shouldNotSetMaintenanceCountdownInSuccessResponseWhenNoMaintenanceScheduledForSpecifiedGame() throws TableException, IOException {
        GameAvailability value = new GameAvailability(GameAvailabilityService.Availability.AVAILABLE);
        when(gameAvailabilityService.getAvailabilityOfGameType(GAME_TYPE)).thenReturn(value);
        MockHttpServletResponse response = setUpSuccessfulFacebookLogin(LoginResult.EXISTING_USER);

        underTest.loginWithFacebook(httpServletRequest, response, ANDROID.name(), GAME_TYPE, "1234", true, CLIENT_CONTEXT);

        final String content = response.getContentAsString();
        final LoginInfo actualLoginInfo = new JsonHelper().deserialize(LoginInfo.class, content);
        assertThat(actualLoginInfo.getAvailability(), equalTo(GameAvailabilityService.Availability.AVAILABLE));
        assertThat(actualLoginInfo.getMaintenanceStartsAtMillis(), equalTo(null));
    }

    private MockHttpServletResponse setUpSuccessfulFacebookLogin(LoginResult existingUser) {
        MockHttpServletResponse response = new MockHttpServletResponse();
        BigDecimal playerId = BigDecimal.ZERO;
        when(lobbySessionCache.getActiveSession(httpServletRequest)).thenReturn(
                new LobbySession(SESSION_ID, playerId, "", "", Partner.YAZINO, "", "", null, false, ANDROID, AuthProvider.YAZINO));
        when(externalWebLoginService.login(eq(httpServletRequest), eq(response), eq(YAZINO), anyString(), eq(playerInformationHolder),
                eq(true), eq(Platform.ANDROID), anyMap())).thenReturn(new LoginResponse(existingUser, aLobbySession()));
        return response;
    }

    private LobbySession aLobbySession() {
        return new LobbySession(SESSION_ID, BigDecimal.ZERO, "", "", YAZINO, "", "", null, false,
                IOS, AuthProvider.YAZINO);
    }

    private void assertResponseStatusIsSetTo400() throws IOException {
        verify(httpServletResponse).sendError(HttpServletResponse.SC_BAD_REQUEST);
        verifyZeroInteractions(externalWebLoginService, lobbySessionCache, lobbySessionFactory, authenticationService);
    }
}

