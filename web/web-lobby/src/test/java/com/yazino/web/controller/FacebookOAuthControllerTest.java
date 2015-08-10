package com.yazino.web.controller;

import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.player.LoginResult;
import com.yazino.platform.player.PlayerInformationHolder;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.table.GameConfiguration;
import com.yazino.platform.table.GameConfigurationProperty;
import com.yazino.platform.table.GameTypeInformation;
import com.yazino.web.data.GameTypeRepository;
import com.yazino.web.domain.GameTypeMapper;
import com.yazino.web.domain.GameTypeResolver;
import com.yazino.web.domain.LoginResponse;
import com.yazino.web.domain.SiteConfiguration;
import com.yazino.web.domain.facebook.FacebookUserInformationProvider;
import com.yazino.web.service.*;
import com.yazino.web.session.LobbySession;
import com.yazino.web.util.CookieHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import com.yazino.game.api.GameType;
import strata.server.lobby.api.facebook.FacebookAppConfiguration;
import strata.server.lobby.api.facebook.FacebookConfiguration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.yazino.platform.Platform.FACEBOOK_CANVAS;
import static com.yazino.platform.Platform.WEB;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FacebookOAuthControllerTest {
    private static final String GAME_TYPE = "EXCITING_GAME";
    private static final String CONNECT_GAME_TYPE = "EXCITING_CONNECT_GAME";
    private static final String REQUEST_GAME_TYPE = "EXCITING_REQUEST_GAME";
    private static final String APP_NAME = "anAppName";
    private static final String APP_ID = "anAppId";
    private static final String SIGNED_REQUEST = "Yc9l5nQ2RqzW5FfAwJgeIaEodOQgSzcs0cN6KGyYSgc.eyJhbGdvcml0aG0iO"
            + "iJITUFDLVNIQTI1NiIsImV4cGlyZXMiOjEzMDU2MjY0MDAsImlzc3VlZF9hdC"
            + "I6MTMwNTYyMjQyNCwib2F1dGhfdG9rZW4iOiI5OTA3MDQ3OTYzMXwyLmRKZV9"
            + "UR204QzNaNnNDUkdnZHNQRkFfXy4zNjAwLjEzMDU2MjY0MDAuMS02NTE5NzE0"
            + "MTh8Y1ZhaTgzRmFsRjIxREY5S3JSUlJTMk1VSkQ0IiwidXNlciI6eyJjb3Vud"
            + "HJ5IjoiZ2IiLCJsb2NhbGUiOiJlbl9HQiIsImFnZSI6eyJtaW4iOjIxfX0sIn" + "VzZXJfaWQiOiI2NTE5NzE0MTgifQ";
    private static final String OAUTH_TOKEN =
            "99070479631|2.dJe_TGm8C3Z6sCRGgdsPFA__.3600.1305626400.1-651971418|cVai83FalF21DF9KrRRRS2MUJD4";
    private static final String OAUTH_CODE =
            "AQCp00aRLhrb4DFU3zFuPMl60D2JcoJDM3o-VIadn_AZPSqcPjPTZxLKkSeg84V2-JCV-D5IYC3AIU4KNpP0SIjajHGFPzh0UgwGWc"
                    + "-UWKo-_2jM7hxWOWR6ru9hFD7vvg1JPuPWVRCZd7HDdnM1ZgvJ2DbTifAQWODbY0UdrqgOXGXfrmz9XAue"
                    + "TlqK2xCgVlEnEH-rXAyNNt8SDerILSvQ";
    private static final String HOST_URL = "http://a.test.host";
    private static final String SECRET_KEY = "5399b76e1b14b3e983934fc9486b854e";
    private static final String ASSET_URL = "anAssetUrl";
    private static final String REDIRECT_URL = "http://something.something/";
    private static final String REF_PARAM = "REF_BLAH_1";
    private static final String REMOTE_IP_ADDRESS = "9.8.7.1";
    public static final String HTTP_URL_FOR_REDIRECT = "http://url.for/redirect";
    private static final String DEFAULT_GAME_TYPE = "BLACKJACK";
    public static final BigDecimal PLAYER_ID = BigDecimal.valueOf(8776);
    public static final String USERNAME = "13452345";
    public static final HashMap<String,Object> EMPTY_CLIENT_CONTEXT_MAP = new HashMap<String, Object>();

    @Mock
    private CookieHelper cookieHelper;
    @Mock
    private GameTypeResolver gameTypeResolver;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FacebookUserInformationProvider userInformationProvider;
    @Mock
    private PlayerInformationHolder playerInformationHolder;
    @Mock
    private FacebookService facebookService;
    @Mock
    private RememberMeHandler rememberMeHandler;
    @Mock
    private GameTypeRepository gameTypeRepository;
    @Mock
    private ExternalWebLoginService externalWebLoginService;
    @Mock
    private FacebookAppToUserRequestHandler fbAppRequestService;
    @Mock
    private GameConfigurationRepository gameConfigurationRepository;
    @Mock
    private HttpSession mockHttpSession;

    private FacebookOAuthController underTest;
    private FacebookAppConfiguration appConfiguration;
    private FacebookConfiguration facebookConfiguration;
    private SiteConfiguration siteConfiguration;
    private String facebookPermissions = "email,user_birthday";

    @Before
    public void setUp() throws IOException {
        appConfiguration = appConfigurationFor(GAME_TYPE);

        facebookConfiguration = new FacebookConfiguration();
        facebookConfiguration.setApplicationConfigs(asList(appConfiguration));
        facebookConfiguration.setUsingSeparateConnectApplication(true);
        facebookConfiguration.setConnectAppConfiguration(appConfigurationFor(CONNECT_GAME_TYPE));

        siteConfiguration = new SiteConfiguration();
        siteConfiguration.setAssetUrl(ASSET_URL);
        siteConfiguration.setHostUrl(HOST_URL);
        siteConfiguration.setDefaultGameType(DEFAULT_GAME_TYPE);

        when(request.getSession()).thenReturn(mockHttpSession);
        when(request.getRemoteAddr()).thenReturn(REMOTE_IP_ADDRESS);

        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        when(request.getRequestURI()).thenReturn(format("/public/facebookLogin/%s/", GAME_TYPE));

        when(userInformationProvider.getUserInformationHolder(eq(OAUTH_TOKEN), anyString(),
                eq(REMOTE_IP_ADDRESS), anyBoolean())).thenReturn(playerInformationHolder);

        when(externalWebLoginService.login(eq(request), eq(response), eq(Partner.YAZINO), anyString(), eq(playerInformationHolder),
                eq(true), any(Platform.class), eq(EMPTY_CLIENT_CONTEXT_MAP))).thenReturn(new LoginResponse(LoginResult.EXISTING_USER, aLobbySession()));

        PlayerProfile playerProfile = new PlayerProfile();
        playerProfile.setPlayerId(PLAYER_ID);
        playerProfile.setExternalId(USERNAME);
        when(playerInformationHolder.getPlayerProfile()).thenReturn(playerProfile);

        underTest = new FacebookOAuthController(facebookConfiguration, facebookPermissions, cookieHelper,
                siteConfiguration, gameTypeResolver, userInformationProvider, facebookService,
                rememberMeHandler, gameTypeRepository, externalWebLoginService, fbAppRequestService,
                gameConfigurationRepository);
    }

    @Test
    public void aFacebookConnectLoginMustHaveAnAuthToken() throws IOException {
        underTest.connectLogin(request, response, null);

        verify(response).sendError(500);
    }

    @Test
    public void aFacebookConnectLoginUsesTheConnectFacebookApplication() throws IOException {
        when(gameTypeResolver.resolveGameType(request, response)).thenReturn(GAME_TYPE);

        underTest.connectLogin(request, response, OAUTH_TOKEN);

        verify(externalWebLoginService).login(request, response, Partner.YAZINO, CONNECT_GAME_TYPE,
                playerInformationHolder, true, WEB, EMPTY_CLIENT_CONTEXT_MAP);
    }

    @Test
    public void aFacebookConnectLoginRedirectsToTheGamePage() throws IOException {
        when(gameTypeResolver.resolveGameType(request, response)).thenReturn(REQUEST_GAME_TYPE);

        final ModelAndView modelAndView = underTest.connectLogin(request, response, OAUTH_TOKEN);
        assertRedirectIsToConnectGame(modelAndView);
    }

    @Test
    public void aFacebookConnectLoginUsesTheGameTypeTo() throws IOException {
        when(gameTypeResolver.resolveGameType(request, response)).thenReturn(REQUEST_GAME_TYPE);

        final ModelAndView modelAndView = underTest.connectLogin(request, response, OAUTH_TOKEN);
        assertRedirectIsToConnectGame(modelAndView);
    }

    @Test
    public void ifTheUserProfileCannotBeRetrievedThenTheFlowContinuesToTheRedirect() throws IOException {
        reset(playerInformationHolder);
        playerInformationHolder.setPlayerProfile(null);
        when(userInformationProvider.getUserInformationHolder(eq(OAUTH_TOKEN), anyString(),
                eq(REMOTE_IP_ADDRESS), eq(true))).thenReturn(playerInformationHolder);

        final ModelAndView modelAndView = underTest.connectLogin(request, response, OAUTH_TOKEN);

        assertThat(((RedirectView) modelAndView.getView()).getUrl(),
                is(equalTo("http://a.test.host/excitingConnectGame")));
    }

    @Test
    public void aFacebookConnectLoginRedirectsToRedirectToCookieValueIfItExists() throws IOException {
        when(gameTypeResolver.resolveGameType(request, response)).thenReturn(GAME_TYPE);
        String redirectTo = "http://redirect/to";
        when(cookieHelper.getRedirectTo(request)).thenReturn(redirectTo);

        final ModelAndView modelAndView = underTest.connectLogin(request, response, OAUTH_TOKEN);

        assertThat(modelAndView, is(not(nullValue())));
        assertThat(modelAndView.getViewName(), is(nullValue()));
        assertThat(modelAndView.getView(), is(instanceOf(RedirectView.class)));
        assertThat(((RedirectView) modelAndView.getView()).getUrl(), is(equalTo(redirectTo)));
    }

    @Test
    public void aFacebookConnectLoginResolvesTheGameTypeViaTheResolver() throws IOException {
        reset(externalWebLoginService);
        when(externalWebLoginService.login(request, response, Partner.YAZINO, CONNECT_GAME_TYPE, playerInformationHolder,
                true, WEB, EMPTY_CLIENT_CONTEXT_MAP)).thenReturn(new LoginResponse(LoginResult.EXISTING_USER, aLobbySession()));
        when(gameTypeResolver.resolveGameType(request, response)).thenReturn(REQUEST_GAME_TYPE);

        final ModelAndView modelAndView = underTest.connectLogin(request, response, OAUTH_TOKEN);

        assertThat(((RedirectView) modelAndView.getView()).getUrl(),
                is(equalTo("http://a.test.host/excitingRequestGame")));
        verify(gameTypeResolver).resolveGameType(request, response);
    }

    @Test
    public void anUnauthenticatedUserIsRedirectedToTheFacebookAuthService() throws IOException {
        final ModelAndView modelAndView = underTest.canvasLogin(request, response, null, null, null, null);

        assertThat(modelAndView, is(not(nullValue())));
        assertThat(modelAndView.getViewName(), is(equalTo("facebookLoginRedirect")));
        assertThat(
                (String) modelAndView.getModel().get("faceBookLoginUrl"),
                is(equalTo("https://www.facebook.com/dialog/oauth?client_id=anAppId&redirect_uri=http%3A%2F%2Fapps.facebook.com%2FanAppName%2F%3Frequest_ids%3Dnull&scope=email,user_birthday")));
    }

    @Test
    public void anUnauthenticatedUserForANonCanvasAppIsRedirectedToTheFacebookAuthServiceWithoutPermissions()
            throws IOException {
        appConfiguration.setCanvasActionsAllowed(false);

        final ModelAndView modelAndView = underTest.canvasLogin(request, response, null, null, null, null);

        assertThat(modelAndView, is(not(nullValue())));
        assertThat(modelAndView.getViewName(), is(equalTo("facebookLoginRedirect")));
        assertThat(
                (String) modelAndView.getModel().get("faceBookLoginUrl"),
                is(equalTo("https://www.facebook.com/dialog/oauth?client_id=anAppId&redirect_uri=http%3A%2F%2Fapps.facebook.com%2FanAppName%2F%3Frequest_ids%3Dnull&scope=")));
    }

    @Test
    public void whenThereIsNoGameTypeInTheUrlForAnUnauthenticatedUserItIsResolvedViaTheResolver() throws IOException {
        reset(request);
        when(request.getRequestURI()).thenReturn("/a/url/without/gametype");
        when(gameTypeResolver.resolveGameType(request, response)).thenReturn(GAME_TYPE);

        final ModelAndView modelAndView = underTest.canvasLogin(request, response, null, null, null, null);

        assertThat(modelAndView, is(not(nullValue())));
        assertThat(modelAndView.getViewName(), is(equalTo("facebookLoginRedirect")));
        assertThat(
                (String) modelAndView.getModel().get("faceBookLoginUrl"),
                is(equalTo("https://www.facebook.com/dialog/oauth?client_id=anAppId&redirect_uri=http%3A%2F%2Fapps.facebook.com%2FanAppName%2F%3Frequest_ids%3Dnull&scope=email,user_birthday")));

        verify(gameTypeResolver).resolveGameType(request, response);
    }

    @Test
    public void ifATableIdIsPresentForAnUnauthenticatedUserThenItIsSetAsACookie() throws IOException {
        when(request.getParameter("ref")).thenReturn("aRef");

        underTest.canvasLogin(request, response, "aTableId", null, null, null);

        verify(cookieHelper).setReferralTableId(response, "aTableId");
    }

    @Test
    public void ifTheRequestIsSecureThenTheRedirectionUrlShouldAlsoBeSecure() throws IOException {
        when(request.isSecure()).thenReturn(true);

        final ModelAndView modelAndView = underTest.canvasLogin(request, response, null, null, null, null);

        assertThat(modelAndView, is(not(nullValue())));
        assertThat(modelAndView.getViewName(), is(equalTo("facebookLoginRedirect")));
        assertThat(
                (String) modelAndView.getModel().get("faceBookLoginUrl"),
                is(equalTo("https://www.facebook.com/dialog/oauth?client_id=anAppId&redirect_uri=https%3A%2F%2Fapps.facebook.com%2FanAppName%2F%3Frequest_ids%3Dnull&scope=email,user_birthday")));
    }

    @Test
    public void exceptionsDuringAuthenticationAreCaughtAndCauseA500ReturnType() throws IOException {
        when(request.getRequestURI()).thenThrow(new RuntimeException("anException"));

        final ModelAndView modelAndView = underTest.canvasLogin(request, response, null, null, null, null);
        assertThat(modelAndView, is(nullValue()));

        verify(response).sendError(500);
    }

    @Test
    public void ifAuthenticationReturnsAnErrorARedirectionIsReturned() throws IOException {
        final ModelAndView modelAndView = underTest.canvasLogin(request, response, null, null, null, "anError");

        assertThat(modelAndView, is(not(nullValue())));
        assertThat(modelAndView.getViewName(), is(equalTo("facebookAuthFailure")));
    }

    @Test
    public void ifAuthenticationReturnsAnErrorCodeAndMessageThenARedirectionIsReturned() throws IOException {
        final ModelAndView modelAndView = underTest.connectFromFacebookWithCodedError(response, "aGameType", "anErrorCode", "anErrorMessage");

        assertThat(modelAndView, is(not(nullValue())));
        assertThat(modelAndView.getView(), is(instanceOf(RedirectView.class)));
        assertThat(((RedirectView) modelAndView.getView()).getUrl(), is(equalTo("/aGameType")));
    }

    @Test
    public void anAuthenticatedUserHasASessionCreatedForThem() throws IOException {
        reset(externalWebLoginService);
        when(externalWebLoginService.login(request, response, Partner.YAZINO, GAME_TYPE, playerInformationHolder,
                true, FACEBOOK_CANVAS, EMPTY_CLIENT_CONTEXT_MAP)).thenReturn(new LoginResponse(LoginResult.EXISTING_USER, aLobbySession()));

        underTest.canvasLogin(request, response, null, SIGNED_REQUEST, null, null);

        verify(externalWebLoginService).login(request, response, Partner.YAZINO, GAME_TYPE, playerInformationHolder,
                true, FACEBOOK_CANVAS, EMPTY_CLIENT_CONTEXT_MAP);
    }

    @Test
    public void anAuthenticatedUserIsRedirectedToTheSplashScreenWhenApplicationIsSetToRedirect()
            throws IOException {
        reset(externalWebLoginService);
        appConfiguration.setRedirecting(true);
        when(externalWebLoginService.login(request, response, Partner.YAZINO, GAME_TYPE, playerInformationHolder,
                true, WEB, EMPTY_CLIENT_CONTEXT_MAP)).thenReturn(new LoginResponse(LoginResult.EXISTING_USER, aLobbySession()));

        final ModelAndView modelAndView = underTest.canvasLogin(request, response, null, SIGNED_REQUEST, null, null);

        assertThat(modelAndView, is(not(nullValue())));
        assertThat(modelAndView.getViewName(), is(equalTo("fbredirect/fanPage")));
        assertThat(modelAndView.getModel().get("targetUrl").toString(), is(equalTo("http://something.something/")));
        assertThat(modelAndView.getModel().get("gameType").toString(), is(equalTo("EXCITING_GAME")));
    }

    @Test
    public void anAuthenticatedUserIsShownAStaticViewWhenApplicationIsSetToRedirectAndCanvasOperationsAreNotAllowed()
            throws IOException {
        reset(externalWebLoginService);
        appConfiguration.setRedirecting(true);
        appConfiguration.setCanvasActionsAllowed(false);
        when(externalWebLoginService.login(request, response, Partner.YAZINO, GAME_TYPE, playerInformationHolder,
                true, WEB, EMPTY_CLIENT_CONTEXT_MAP)).thenReturn(new LoginResponse(LoginResult.EXISTING_USER, aLobbySession()));

        final ModelAndView modelAndView = underTest.canvasLogin(request, response, null, SIGNED_REQUEST, null, null);

        assertThat(modelAndView, is(not(nullValue())));
        assertThat(modelAndView.getViewName(), is(equalTo("fbredirect/fanPage")));
        verifyZeroInteractions(response);
    }

    @Test
    public void anAuthenticatedUserIsRedirectedWhenInCanvasDisplayIsOnAndTheGameUsesTheHTMLLobby() throws IOException {
        reset(externalWebLoginService);
        when(request.isSecure()).thenReturn(true);
        when(externalWebLoginService.login(request, response, Partner.YAZINO, GAME_TYPE, playerInformationHolder,
                true, FACEBOOK_CANVAS, EMPTY_CLIENT_CONTEXT_MAP)).thenReturn(new LoginResponse(LoginResult.EXISTING_USER, aLobbySession()));

        final ModelAndView modelAndView = underTest.canvasLogin(request, response, null, SIGNED_REQUEST, null, null);

        assertThat(modelAndView, is(not(nullValue())));
        assertThat(modelAndView.getViewName(), is(nullValue()));
        assertThat(modelAndView.getView(), is(instanceOf(RedirectView.class)));
        assertThat(((RedirectView) modelAndView.getView()).getUrl(),
                is(equalTo(format("http://a.test.host/table/find/%s", GAME_TYPE))));
    }

    @Test
    public void anAuthenticatedUserIsRedirectedWhenInCanvasDisplayIsOnAndTheGameUsesTheFlashLobby() throws IOException {
        reset(externalWebLoginService);
        when(request.isSecure()).thenReturn(true);
        when(externalWebLoginService.login(request, response, Partner.YAZINO, GAME_TYPE, playerInformationHolder,
                true, FACEBOOK_CANVAS, EMPTY_CLIENT_CONTEXT_MAP)).thenReturn(new LoginResponse(LoginResult.EXISTING_USER, aLobbySession()));
        when(gameConfigurationRepository.find(GAME_TYPE)).thenReturn(aGameConfigurationWithFlashLobby(true));

        final ModelAndView modelAndView = underTest.canvasLogin(request, response, null, SIGNED_REQUEST, null, null);

        assertThat(modelAndView, is(not(nullValue())));
        assertThat(modelAndView.getViewName(), is(nullValue()));
        assertThat(modelAndView.getView(), is(instanceOf(RedirectView.class)));
        assertThat(((RedirectView) modelAndView.getView()).getUrl(),
                is(equalTo(format("http://a.test.host/%s", new GameTypeMapper().getViewName(GAME_TYPE)))));
    }

    @Test
    public void anAuthenticatedUserIsRedirectedToTheSecureFacebookAppsPageWhenInCanvasDisplayIsOnAndTheRequestIsInsecure() throws IOException {
        reset(externalWebLoginService);
        when(externalWebLoginService.login(request, response, Partner.YAZINO, GAME_TYPE, playerInformationHolder,
                true, FACEBOOK_CANVAS, EMPTY_CLIENT_CONTEXT_MAP)).thenReturn(new LoginResponse(LoginResult.EXISTING_USER, aLobbySession()));

        final ModelAndView modelAndView = underTest.canvasLogin(request, response, null, SIGNED_REQUEST, null, null);

        assertThat(modelAndView, is(not(nullValue())));
        assertThat(modelAndView.getViewName(), is(equalTo("fbredirect/secureCanvasRedirect")));
    }

    @Test
    public void anAuthenticatedUserWhoIsBlockedIsRedirectedToTheBlockedView() throws IOException {
        reset(externalWebLoginService);
        when(externalWebLoginService.login(request, response, Partner.YAZINO, GAME_TYPE, playerInformationHolder,
                true, FACEBOOK_CANVAS, EMPTY_CLIENT_CONTEXT_MAP)).thenReturn(new LoginResponse(LoginResult.BLOCKED));

        final ModelAndView modelAndView = underTest.canvasLogin(request, response, null, SIGNED_REQUEST, null, null);

        assertThat(modelAndView, is(not(nullValue())));
        assertThat(modelAndView.getViewName(), is(equalTo("blocked")));
        assertThat((String) modelAndView.getModel().get("assetUrl"), is(equalTo(ASSET_URL)));
    }

    @Test
    public void whenAuthenticationFailsAnInternalServerErrorIsReturned() throws IOException {
        reset(externalWebLoginService);
        when(externalWebLoginService.login(request, response, Partner.YAZINO, GAME_TYPE, playerInformationHolder,
                true, FACEBOOK_CANVAS, EMPTY_CLIENT_CONTEXT_MAP)).thenReturn(new LoginResponse(LoginResult.FAILURE));

        final ModelAndView modelAndView = underTest.canvasLogin(request, response, null, SIGNED_REQUEST, null, null);

        assertThat(modelAndView, is(nullValue()));
        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void anAuthenticatedUserWhoRequestsATableIsRedirectedToPlayTableView() throws IOException {
        String tableId = "123";
        when(request.isSecure()).thenReturn(true);

        final ModelAndView modelAndView = underTest.canvasLogin(request, response, tableId, SIGNED_REQUEST, null, null);

        assertThat(modelAndView, is(not(nullValue())));
        assertThat(modelAndView.getViewName(), is(nullValue()));
        assertThat(modelAndView.getView(), is(instanceOf(RedirectView.class)));
        assertThat(((RedirectView) modelAndView.getView()).getUrl(), is(equalTo("/table/" + tableId)));
    }

    @Test
    public void aSuccessfulCanvasLoginDoesNotStoreTheRememberMeCookie() throws IOException {
        underTest.canvasLogin(request, response, null, SIGNED_REQUEST, null, null);

        verifyZeroInteractions(rememberMeHandler);
    }

    @Test
    public void aSuccessfulConnectFromFacebookLoginDoesStoreTheRememberMeCookie() throws IOException {
        when(request.getParameter("ref")).thenReturn(REF_PARAM);
        when(request.getRequestURL()).thenReturn(new StringBuffer(HTTP_URL_FOR_REDIRECT));
        when(facebookService.getAccessTokenForGivenCode(OAUTH_CODE, APP_ID, SECRET_KEY, HTTP_URL_FOR_REDIRECT + "?ref=" + REF_PARAM)).thenReturn(OAUTH_TOKEN);

        underTest.connectFromFacebook(request, response, GAME_TYPE, OAUTH_CODE, REF_PARAM);

        verify(rememberMeHandler).storeRememberMeCookie(Partner.YAZINO, Platform.WEB, PLAYER_ID, USERNAME, request, response);
    }

    @Test
    public void aSuccessfulConnectLoginLoginDoesStoreTheRememberMeCookie() throws IOException {
        underTest.connectLogin(request, response, OAUTH_TOKEN);

        verify(rememberMeHandler).storeRememberMeCookie(Partner.YAZINO, Platform.WEB, PLAYER_ID, USERNAME, request, response);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerExceptionForNullFacebookConfiguration() {
        new FacebookOAuthController(null, facebookPermissions, cookieHelper, siteConfiguration,
                gameTypeResolver, userInformationProvider, facebookService,
                rememberMeHandler, gameTypeRepository, externalWebLoginService, fbAppRequestService, gameConfigurationRepository);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerExceptionForNullCookieHelper() {
        new FacebookOAuthController(facebookConfiguration, facebookPermissions, null, siteConfiguration,
                gameTypeResolver, userInformationProvider, facebookService,
                rememberMeHandler, gameTypeRepository, externalWebLoginService, fbAppRequestService, gameConfigurationRepository);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerExceptionForNullSiteConfiguration() {
        new FacebookOAuthController(facebookConfiguration, facebookPermissions, cookieHelper, null,
                gameTypeResolver, userInformationProvider, facebookService,
                rememberMeHandler, gameTypeRepository, externalWebLoginService, fbAppRequestService, gameConfigurationRepository);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerExceptionForNullExternalAuthenticationService() {
        new FacebookOAuthController(facebookConfiguration, facebookPermissions, cookieHelper, siteConfiguration,
                gameTypeResolver, userInformationProvider, facebookService,
                rememberMeHandler, gameTypeRepository, null, fbAppRequestService, gameConfigurationRepository);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerExceptionForNullGameTypeResolver() {
        new FacebookOAuthController(facebookConfiguration, facebookPermissions, cookieHelper, siteConfiguration,
                null, userInformationProvider, facebookService,
                rememberMeHandler, gameTypeRepository, externalWebLoginService, fbAppRequestService, gameConfigurationRepository);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerExceptionForNullFacebookUserInformationProvider() {
        new FacebookOAuthController(facebookConfiguration, facebookPermissions, cookieHelper, siteConfiguration,
                gameTypeResolver, null, facebookService,
                rememberMeHandler, gameTypeRepository, externalWebLoginService, fbAppRequestService, gameConfigurationRepository);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerExceptionForNullFacebookService() {
        new FacebookOAuthController(facebookConfiguration, facebookPermissions, cookieHelper, siteConfiguration,
                gameTypeResolver, userInformationProvider, null,
                rememberMeHandler, gameTypeRepository, externalWebLoginService, fbAppRequestService, gameConfigurationRepository);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerExceptionForGameTypeRepository() {
        new FacebookOAuthController(facebookConfiguration, facebookPermissions, cookieHelper, siteConfiguration,
                gameTypeResolver, userInformationProvider, facebookService,
                rememberMeHandler, null, externalWebLoginService, fbAppRequestService, gameConfigurationRepository);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerExceptionForGameConfigurationRepository() {
        new FacebookOAuthController(facebookConfiguration, facebookPermissions, cookieHelper, siteConfiguration,
                gameTypeResolver, userInformationProvider, facebookService,
                rememberMeHandler, gameTypeRepository, externalWebLoginService, fbAppRequestService, null);
    }

    @Test
    public void shouldGetAccessTokenFromFacebookWhenUsingCode() {
        when(request.getRequestURL()).thenReturn(new StringBuffer(HTTP_URL_FOR_REDIRECT));
        underTest.connectFromFacebook(request, response, GAME_TYPE, OAUTH_CODE, null);
        verify(facebookService).getAccessTokenForGivenCode(OAUTH_CODE, APP_ID, SECRET_KEY, HTTP_URL_FOR_REDIRECT);
    }

    @Test
    public void shouldGetAccessTokenFromFacebookWhenUsingCodeAndrefParam() {
        when(request.getRequestURL()).thenReturn(new StringBuffer(HTTP_URL_FOR_REDIRECT));
        underTest.connectFromFacebook(request, response, GAME_TYPE, OAUTH_CODE, REF_PARAM);
        verify(facebookService).getAccessTokenForGivenCode(OAUTH_CODE, APP_ID, SECRET_KEY, HTTP_URL_FOR_REDIRECT + "?ref=" + REF_PARAM);
    }

    @Test
    public void shouldPassCorrectAdCodeToRegisterSession() {
        when(request.getParameter("ref")).thenReturn(REF_PARAM);
        when(request.getRequestURL()).thenReturn(new StringBuffer(HTTP_URL_FOR_REDIRECT));
        when(facebookService.getAccessTokenForGivenCode(OAUTH_CODE, APP_ID, SECRET_KEY, HTTP_URL_FOR_REDIRECT + "?ref=" + REF_PARAM)).thenReturn(OAUTH_TOKEN);
        underTest.connectFromFacebook(request, response, GAME_TYPE, OAUTH_CODE, REF_PARAM);
        verify(externalWebLoginService).login(request,
                response,
                Partner.YAZINO,
                CONNECT_GAME_TYPE,
                playerInformationHolder,
                true, WEB, EMPTY_CLIENT_CONTEXT_MAP);
    }

    @Test
    public void shouldSetCookieWhenUsingCode() {
        when(request.getRequestURL()).thenReturn(new StringBuffer(HTTP_URL_FOR_REDIRECT));
        underTest.connectFromFacebook(request, response, GAME_TYPE, OAUTH_CODE, null);
        verify(cookieHelper).setLastGameType(response, GAME_TYPE);
    }

    @Test
    public void shouldReturnInternalServerErrorOnCodeError() throws IOException {
        when(request.getRequestURL()).thenReturn(new StringBuffer(HTTP_URL_FOR_REDIRECT));
        when(facebookService.getAccessTokenForGivenCode(OAUTH_CODE, APP_ID, SECRET_KEY, HTTP_URL_FOR_REDIRECT)).thenReturn(null);

        final ModelAndView modelAndView = underTest.connectFromFacebook(request, response, GAME_TYPE, OAUTH_CODE, null);

        assertThat(modelAndView, is(nullValue()));
        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void shouldRedirectToFacebookToReauthenticateWhenFacebookReturnsAnOAuthException() throws IOException {
        // this behaviour is based on http://developers.facebook.com/blog/post/2011/05/13/how-to--handle-expired-access-tokens/

        when(request.getRequestURL()).thenReturn(new StringBuffer(HTTP_URL_FOR_REDIRECT));
        when(facebookService.getAccessTokenForGivenCode(OAUTH_CODE, APP_ID, SECRET_KEY, HTTP_URL_FOR_REDIRECT))
                .thenThrow(new FacebookOAuthException("aFacebookType", "aFacebookMessage"));

        final ModelAndView modelAndView = underTest.connectFromFacebook(request, response, GAME_TYPE, OAUTH_CODE, null);

        assertThat(modelAndView, is(not(nullValue())));
        assertThat(modelAndView.getView(), is(instanceOf(RedirectView.class)));
        assertThat(((RedirectView) modelAndView.getView()).getUrl(),
                is(equalTo(String.format("https://www.facebook.com/dialog/oauth?client_id=%s&scope=email,publish_actions&redirect_uri=%s", APP_ID, HTTP_URL_FOR_REDIRECT))));
    }

    @Test
    public void shouldRedirectToGamePageWhenUsingConnectFromFacebook() {
        when(request.getRequestURL()).thenReturn(new StringBuffer(HTTP_URL_FOR_REDIRECT));
        when(facebookService.getAccessTokenForGivenCode(OAUTH_CODE, APP_ID, SECRET_KEY, HTTP_URL_FOR_REDIRECT)).thenReturn(OAUTH_TOKEN);
        final ModelAndView modelAndView = underTest.connectFromFacebook(request, response, REQUEST_GAME_TYPE, OAUTH_CODE, null);
        assertRedirectIsToConnectGame(modelAndView);
    }

    @Test
    public void ifTheUserDoesNotAllowPermissionsThenTheyShouldBeRedirectedToTheNotAllowedPageForTheGivenGameType() {
        final ModelAndView modelAndView = underTest.connectFromFacebookError(response, GAME_TYPE, "user_denied", "access_denied", "The+user+denied+your+request.");

        assertThat(modelAndView.getView(), is(instanceOf(RedirectView.class)));
        assertThat(((RedirectView) modelAndView.getView()).getUrl(), is(equalTo("/not-allowed/" + GAME_TYPE)));
    }

    @Test
    public void theNotAllowedPageAddsTheGivenGameTypeToTheModel() {
        when(gameTypeRepository.getGameTypes()).thenReturn(aMapOfGameTypes());
        final ModelMap model = new ModelMap();

        underTest.notAllowed("aGameOfSorts", model);

        assertThat((String) model.get("gameType"), is(equalTo("GAME_OF_SORTS")));
    }

    @Test
    public void theNotAllowedPageUsesTheDefaultGameTypeIsNoneIsSupplied() {
        when(gameTypeRepository.getGameTypes()).thenReturn(aMapOfGameTypes());
        final ModelMap model = new ModelMap();

        underTest.notAllowed(null, model);

        assertThat((String) model.get("gameType"), is(equalTo(DEFAULT_GAME_TYPE)));
    }

    @Test
    public void theNotAllowedPageReturnsTheDoNotAllowView() {
        when(gameTypeRepository.getGameTypes()).thenReturn(aMapOfGameTypes());
        final ModelMap model = new ModelMap();

        final String viewName = underTest.notAllowed("aGameOfSorts", model);

        assertThat(viewName, is(equalTo("do-not-allow")));
    }

    @Test
    public void anAuthenticatedUserAppRequestIsTracked() throws IOException {
        reset(externalWebLoginService);
        when(externalWebLoginService.login(request, response, Partner.YAZINO, GAME_TYPE, playerInformationHolder,
                true, WEB, EMPTY_CLIENT_CONTEXT_MAP)).thenReturn(new LoginResponse(LoginResult.EXISTING_USER, aLobbySession()));
        when(request.getParameter("request_ids")).thenReturn(APP_ID);

        underTest.canvasLogin(request, response, null, SIGNED_REQUEST, null, null);

        verify(fbAppRequestService).processAppRequests(APP_ID, OAUTH_TOKEN);
    }


    private Map<String, GameTypeInformation> aMapOfGameTypes() {
        return Collections.singletonMap("GAME_OF_SORTS",
                new GameTypeInformation(new GameType("GAME_OF_SORTS", "Game Of Sorts", Collections.singleton("aGameOfSorts")), true));
    }

    private void assertRedirectIsToConnectGame(ModelAndView modelAndView) {
        assertThat(modelAndView, is(not(nullValue())));
        assertThat(modelAndView.getViewName(), is(nullValue()));
        assertThat(modelAndView.getView(), is(instanceOf(RedirectView.class)));
        assertThat(((RedirectView) modelAndView.getView()).getUrl(), is(equalTo("http://a.test.host/excitingRequestGame")));
    }

    private GameConfiguration aGameConfigurationWithFlashLobby(final boolean flashLobby) {
        return new GameConfiguration(GAME_TYPE, "aGameType", "aDisplayGameType", Collections.<String>emptySet(), 1)
                .withProperties(asList(
                        new GameConfigurationProperty(BigDecimal.ONE, GAME_TYPE, "usesFlashLobby", Boolean.toString(flashLobby))));
    }

    private LobbySession aLobbySession() {
        return new LobbySession(BigDecimal.valueOf(3141592), PLAYER_ID, "aPlayerName", "aSessionKey", Partner.YAZINO, "aPicutreUrl",
                "anEmailAddress", null, false, Platform.WEB, AuthProvider.YAZINO);
    }

    private FacebookAppConfiguration appConfigurationFor(final String gameType) {
        final FacebookAppConfiguration appConfig = new FacebookAppConfiguration();
        appConfig.setGameType(gameType);
        appConfig.setAppName(APP_NAME);
        appConfig.setApplicationId(APP_ID);
        appConfig.setSecretKey(SECRET_KEY);
        appConfig.setRedirecting(false);
        appConfig.setRedirectUrl(REDIRECT_URL);
        appConfig.setCanvasActionsAllowed(true);
        return appConfig;
    }

}
