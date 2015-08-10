package com.yazino.web.controller;

import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.table.GameTypeInformation;
import com.yazino.web.data.GameTypeRepository;
import com.yazino.web.domain.SiteConfiguration;
import com.yazino.web.security.LogoutHelper;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;
import com.yazino.game.api.GameType;
import strata.server.lobby.api.facebook.ConversionTrackingData;
import strata.server.lobby.api.facebook.FacebookAppConfiguration;
import strata.server.lobby.api.facebook.FacebookConfiguration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static strata.server.lobby.api.facebook.FacebookConfiguration.ApplicationType.CANVAS;
import static strata.server.lobby.api.facebook.FacebookConfiguration.MatchType.LOOSE;

public class MarketingURLControllerTest {

    private static final String LOGIN_URL = "http://a.login.url:8080/somewhere?appId={0}&redirect_uri=http://somewhere/{1}{2}";
    private static final String BLACKJACK_APP_ID = "aBlackjackApplicationId";
    private static final String BLACKJACK_GAME_TYPE = "BLACKJACK";
    private static final String DEFAULT_APP_ID = "aDefaultApplicationId";
    private static final String DEFAULT_GAME_TYPE = "DEFAULT_GAME_TYPE";
    private static final String NO_REF = "";
    private static final String BLACKJACK_PREFIX = "bj";
    private static final String DEFAULT_PREFIX   = "dd";

    @Mock
    private GameTypeRepository gameTypeRepository;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpSession session;
    @Mock
    private LogoutHelper logoutHelper;
    @Mock
    private LobbySessionCache lobbySessionCache;

    private final Map<String, String[]> requestParameters = new HashMap<String, String[]>();

    private MarketingURLController underTest;

    private String adparlorurl ="http://fbads.adparlor.com/click.php?";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        underTest = new MarketingURLController(siteConfiguration(), facebookConfiguration(),
                gameTypeRepository, adparlorurl, logoutHelper, lobbySessionCache);

        when(request.getSession()).thenReturn(session);
        when(request.getPathInfo()).thenReturn("/fb/blackjack");
        when(request.getParameterMap()).thenReturn(requestParameters);
        when(gameTypeRepository.getGameTypes()).thenReturn(gameTypeInformation());
    }

    @Test(expected = NullPointerException.class)
    public void aNullSiteConfigurationCausesANullPointerException() {
        new MarketingURLController(null, facebookConfiguration(), gameTypeRepository, adparlorurl, logoutHelper, lobbySessionCache);
    }

    @Test(expected = NullPointerException.class)
    public void aNullFacebookConfigurationCausesANullPointerException() {
        new MarketingURLController(siteConfiguration(), null, gameTypeRepository, adparlorurl, logoutHelper, lobbySessionCache);
    }

    @Test(expected = NullPointerException.class)
    public void aNullLogoutHelperCausesANullPointerException() {
        new MarketingURLController(siteConfiguration(), facebookConfiguration(), gameTypeRepository, adparlorurl, null, lobbySessionCache);
    }

    @Test(expected = NullPointerException.class)
    public void aNullLobbySessionCacheCausesANullPointerException() {
        new MarketingURLController(siteConfiguration(), facebookConfiguration(), gameTypeRepository, adparlorurl, logoutHelper, null);
    }

    @Test(expected = NullPointerException.class)
    public void aNullGameTypeRepositoryCausesANullPointerException() {
        new MarketingURLController(siteConfiguration(), facebookConfiguration(), null, adparlorurl, logoutHelper, lobbySessionCache);
    }

    @Test(expected = IllegalStateException.class)
    public void aFacebookConfigurationWithANullLoginURLCausesAnIllegalStateException() {
        final FacebookConfiguration facebookConfiguration = facebookConfiguration();
        facebookConfiguration.setLoginUrl(null);

        new MarketingURLController(siteConfiguration(), facebookConfiguration, gameTypeRepository, adparlorurl, logoutHelper, lobbySessionCache);
    }

    @Test(expected = IllegalStateException.class)
    public void aFacebookConfigurationWithAMissingGameTypeCausesAnIllegalStateExceptionOnRedirect() {
        final FacebookConfiguration facebookConfiguration = facebookConfiguration();
        facebookConfiguration.setApplicationConfigs(new ArrayList<FacebookAppConfiguration>());

        underTest = new MarketingURLController(siteConfiguration(), facebookConfiguration,
                gameTypeRepository, adparlorurl, logoutHelper, lobbySessionCache);

        underTest.redirectGamePseudonym(request);
    }

    @Test(expected = IllegalStateException.class)
    public void aFacebookConfigurationWithAMissingAppIdForTheGivenGameTypeCausesAnIllegalStateExceptionOnRedirect() {
        final FacebookConfiguration facebookConfiguration = facebookConfiguration();
        facebookConfiguration.getAppConfigFor(BLACKJACK_GAME_TYPE, CANVAS, LOOSE).setApplicationId(null);

        underTest = new MarketingURLController(siteConfiguration(), facebookConfiguration,
                gameTypeRepository, adparlorurl, logoutHelper, lobbySessionCache);

        underTest.redirectGamePseudonym(request);
    }

    @Test
    public void aRedirectionForAnAdvertReturnsARedirectionToTheConfiguredURL() {
        final View view = underTest.redirectAdvertWithSocialContext("654321");

        assertThat(view, is(instanceOf(RedirectView.class)));

        final RedirectView redirectView = (RedirectView) view;
        assertThat(redirectView.getUrl(), is(equalTo("http://fbads.adparlor.com/click.php?adgrpid=654321")));
    }

    @Test
    public void aRedirectionForBlackjackReturnsARedirectionToTheConfiguredLoginURLIfTheUserHasNoSession() {
        final View view = underTest.redirectGamePseudonym(request);

        assertThat(view, is(instanceOf(RedirectView.class)));

        final RedirectView redirectView = (RedirectView) view;
        assertThat(redirectView.getUrl(), is(equalTo(loginUrlFor(BLACKJACK_APP_ID, BLACKJACK_GAME_TYPE, NO_REF))));
    }

    @Test
    public void aRedirectionForBlackjackReturnsARedirectionToTheGameTypeIfTheUserHasAnExistingSession() {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(aSession());

        final View view = underTest.redirectGamePseudonym(request);

        assertThat(view, is(instanceOf(RedirectView.class)));
        final RedirectView redirectView = (RedirectView) view;
        assertThat(redirectView.getUrl(), is(equalTo("/blackjack")));
    }

    @Test
    public void parametersToTheControllerAreExcludedFromTheRedirectionURL() throws UnsupportedEncodingException {
        requestParameters.put("aParam", new String[]{"aValue"});
        requestParameters.put("anotherParam", new String[]{"anotherValue"});

        final View view = underTest.redirectGamePseudonym(request);

        assertThat(view, is(instanceOf(RedirectView.class)));
        final RedirectView redirectView = (RedirectView) view;
        assertThat(redirectView.getUrl(), is(equalTo(loginUrlFor(BLACKJACK_APP_ID, BLACKJACK_GAME_TYPE, NO_REF))));
    }

    @Test
    public void aSubDirectoryOfThePseudonymIsTranslatedToARefParameter() throws UnsupportedEncodingException {
        reset(request);
        when(request.getPathInfo()).thenReturn("/fb/blackjack/aRef");

        final View view = underTest.redirectGamePseudonym(request);

        assertThat(view, is(instanceOf(RedirectView.class)));
        final RedirectView redirectView = (RedirectView) view;
        assertThat(redirectView.getUrl(), is(equalTo(loginUrlFor(BLACKJACK_APP_ID, BLACKJACK_GAME_TYPE, URLEncoder.encode("?ref=aRef", "UTF8")))));
    }

    @Test
    public void aRedirectionForASecondaryPseudonymForBlackjackReturnsARedirectionToTheConfiguredURL() {
        reset(request);
        when(request.getPathInfo()).thenReturn("/fb/bj");

        final View view = underTest.redirectGamePseudonym(request);

        assertThat(view, is(instanceOf(RedirectView.class)));
        final RedirectView redirectView = (RedirectView) view;
        assertThat(redirectView.getUrl(), is(equalTo(loginUrlFor(BLACKJACK_APP_ID, BLACKJACK_GAME_TYPE, NO_REF))));
    }

    @Test
    public void aRedirectionForAnUnknownGameReturnsARedirectionToTheDefaultGameType() {
        reset(request);
        when(request.getPathInfo()).thenReturn("/fb/anInvalidValue");

        final View view = underTest.redirectGamePseudonym(request);

        assertThat(view, is(instanceOf(RedirectView.class)));
        final RedirectView redirectView = (RedirectView) view;
        assertThat(redirectView.getUrl(), is(equalTo(loginUrlFor(DEFAULT_APP_ID, DEFAULT_GAME_TYPE, NO_REF))));
    }

    @Test
    public void anUtterlyMalformedRedirectionReturnsARedirectionToTheDefaultGameType() {
        reset(request);
        when(request.getPathInfo()).thenReturn("/unexpected/anInvalidValue");

        final View view = underTest.redirectGamePseudonym(request);

        assertThat(view, is(instanceOf(RedirectView.class)));
        final RedirectView redirectView = (RedirectView) view;
        assertThat(redirectView.getUrl(), is(equalTo(loginUrlFor(DEFAULT_APP_ID, DEFAULT_GAME_TYPE, NO_REF))));
    }

    @Test
    public void aRedirectionToAURLOnPort80StripsThePortNumberFromTheURL() {
        // This rather odd requirement is because Facebook uses the redirect URL
        // for encoding the verification code. Hence they need to match exactly.

        underTest = new MarketingURLController(siteConfiguration(),
                facebookConfiguration("http://somewhere:80/else?redirect_uri=http://nowhere:80/{0}/{1}/{2}"),
                gameTypeRepository, adparlorurl, logoutHelper, lobbySessionCache);

        final View view = underTest.redirectGamePseudonym(request);

        assertThat(view, is(instanceOf(RedirectView.class)));
        final RedirectView redirectView = (RedirectView) view;
        assertThat(redirectView.getUrl(), is(equalTo(String.format("http://somewhere/else?redirect_uri=http://nowhere/%s/%s/%s",
                BLACKJACK_APP_ID, BLACKJACK_GAME_TYPE, NO_REF))));
    }

    @Test
    public void aRedirectionToASecureURLOnPort443StripsThePortNumberFromTheURL() {
        // This rather odd requirement is because Facebook uses the redirect URL
        // for encoding the verification code. Hence they need to match exactly.

        underTest = new MarketingURLController(siteConfiguration(),
                facebookConfiguration("http://somewhere:443/else?redirect_uri=https://thingee:443/{0}/{1}/{2}"),
                gameTypeRepository, adparlorurl, logoutHelper, lobbySessionCache);

        final View view = underTest.redirectGamePseudonym(request);

        assertThat(view, is(instanceOf(RedirectView.class)));
        final RedirectView redirectView = (RedirectView) view;
        assertThat(redirectView.getUrl(), is(equalTo(String.format("http://somewhere:443/else?redirect_uri=https://thingee/%s/%s/%s",
                BLACKJACK_APP_ID, BLACKJACK_GAME_TYPE, NO_REF))));
    }

    @Test
    public void shouldProduceCorrectRefForSocialContextRedirectUrl() throws Exception {
        underTest = new MarketingURLController(siteConfiguration(),
                facebookConfiguration("http://somewhere:80/else?redirect_uri=http://nowhere:80/{0}/{1}/{2}"),
                gameTypeRepository, adparlorurl, logoutHelper, lobbySessionCache);

        final View view = underTest.redirectGamePseudonymWithSocialContext(request, BLACKJACK_GAME_TYPE.toLowerCase(), "aRef");

        assertThat(view, is(instanceOf(RedirectView.class)));

        final RedirectView redirectView = (RedirectView) view;
        assertThat(redirectView.getUrl(), is(equalTo(String.format("http://somewhere/else?redirect_uri=http://nowhere/%s/%s/%s",
                BLACKJACK_APP_ID, BLACKJACK_GAME_TYPE, URLEncoder.encode("?ref=aRef", "UTF8")))));
    }

    @Test
    public void aRedirectionForAMigrationURLShouldRedirectTheUserToTheRedirectionUrl() throws Exception {
        underTest = new MarketingURLController(siteConfiguration(),
                facebookConfiguration("http://somewhere:80/else?redirect_uri=http://nowhere:80/{0}/{1}/{2}"),
                gameTypeRepository, adparlorurl, logoutHelper, lobbySessionCache);

        final View view = underTest.redirectGameFromCanvasToWebsite(
                BLACKJACK_GAME_TYPE.toLowerCase(), request, response);

        assertThat(view, is(instanceOf(RedirectView.class)));
        final RedirectView redirectView = (RedirectView) view;
        assertThat(redirectView.getUrl(),
                is(equalTo(String.format("http://somewhere/else?redirect_uri=http://nowhere/%s/%s/%s",
                BLACKJACK_APP_ID, BLACKJACK_GAME_TYPE, ""))));
    }


    @Test
    public void aRedirectionForAMigrationURLShouldCallLogout() throws Exception {
        underTest = new MarketingURLController(siteConfiguration(),
                facebookConfiguration("http://somewhere:80/else?redirect_uri=http://nowhere:80/{0}/{1}/{2}"),
                gameTypeRepository, adparlorurl, logoutHelper, lobbySessionCache);

        underTest.redirectGameFromCanvasToWebsite(BLACKJACK_GAME_TYPE.toLowerCase(), request, response);

        verify(logoutHelper).logout(session, request, response);
    }

    @Test
    public void theSplashScreenReturnsTheViewForTheSpecifiedGameType() {
        final ModelAndView modelAndView = underTest.showFacebookSplash(BLACKJACK_GAME_TYPE);

        assertThat(modelAndView.getViewName(), is(equalTo("fbredirect/fanPage")));
        assertThat((String) modelAndView.getModel().get("gameType"), is(equalTo(BLACKJACK_GAME_TYPE)));
    }

    @Test
    public void theSplashScreenReturnsTheViewForTheSpecifiedGameAlias() {
        final ModelAndView modelAndView = underTest.showFacebookSplash("blackjack");

        assertThat(modelAndView.getViewName(), is(equalTo("fbredirect/fanPage")));
        assertThat((String) modelAndView.getModel().get("gameType"), is(equalTo(BLACKJACK_GAME_TYPE)));
    }

    @Test
    public void theSplashScreenReturnsTheDefaultGameTypeViewForAnUnparsableAlias() {
        final ModelAndView modelAndView = underTest.showFacebookSplash("invalid");

        assertThat(modelAndView.getViewName(), is(equalTo("fbredirect/fanPage")));
        assertThat((String) modelAndView.getModel().get("gameType"), is(equalTo(DEFAULT_GAME_TYPE)));
    }

    @Test
    public void theSplashScreenAddsTheRedirectionUrlToTheModel() {
        final ModelAndView modelAndView = underTest.showFacebookSplash("blackjack");

        assertThat(modelAndView.getModel().get("targetUrl"), is(not(nullValue())));
        assertThat(modelAndView.getModel().get("targetUrl").toString(), is(equalTo("http://aRedirectUrl/")));
    }

    private LobbySession aSession() {
        return new LobbySession(BigDecimal.valueOf(3141592), BigDecimal.valueOf(10), "aPlayerName", "aSessionKey", Partner.YAZINO, "aPictureUrl", "anEmail", null, true, Platform.WEB, AuthProvider.YAZINO);
    }

    private String loginUrlFor(final String appId,
                               final String gameType,
                               final String marketingRef) {
        return MessageFormat.format(LOGIN_URL, appId, gameType, marketingRef);
    }

    private SiteConfiguration siteConfiguration() {
        final SiteConfiguration siteConfiguration = new SiteConfiguration();
        siteConfiguration.setDefaultGameType(DEFAULT_GAME_TYPE);
        return siteConfiguration;
    }

    private FacebookConfiguration facebookConfiguration() {
        return facebookConfiguration(LOGIN_URL);
    }

    private FacebookConfiguration facebookConfiguration(final String loginUrl) {
        final FacebookAppConfiguration defaultConfig = appConfigFor(DEFAULT_APP_ID, DEFAULT_GAME_TYPE, DEFAULT_PREFIX);
        final FacebookAppConfiguration blackjackConfig = appConfigFor(BLACKJACK_APP_ID, BLACKJACK_GAME_TYPE, BLACKJACK_PREFIX);

        final FacebookConfiguration facebookConfiguration = new FacebookConfiguration();
        facebookConfiguration.setApplicationConfigs(asList(blackjackConfig, defaultConfig));
        facebookConfiguration.setAppUrlRoot("http://root/url");
        facebookConfiguration.setConversionTracking(
                Collections.singletonMap("conversionKey", new ConversionTrackingData("anId", "aValue")));
        facebookConfiguration.setLoginUrl(loginUrl);
        return facebookConfiguration;
    }

    private Map<String, GameTypeInformation> gameTypeInformation() {
        final Map<String, GameTypeInformation> gameTypeMap = new HashMap<String, GameTypeInformation>();
        gameTypeMap.put(DEFAULT_GAME_TYPE, new GameTypeInformation(
                new GameType(DEFAULT_GAME_TYPE, DEFAULT_GAME_TYPE, newHashSet("something")), true));
        gameTypeMap.put(BLACKJACK_GAME_TYPE, new GameTypeInformation(
                new GameType(BLACKJACK_GAME_TYPE, BLACKJACK_GAME_TYPE, newHashSet("blackjack", "bj")), true));
        return gameTypeMap;
    }

    private FacebookAppConfiguration appConfigFor(final String appId, final String gameType, final String prefix) {
        final FacebookAppConfiguration facebookAppConfiguration = new FacebookAppConfiguration();
        facebookAppConfiguration.setApiKey("anApiKey");
        facebookAppConfiguration.setSecretKey("aSecretKey");
        facebookAppConfiguration.setApplicationId(appId);
        facebookAppConfiguration.setFanPageId("aFanPageId");
        facebookAppConfiguration.setGameType(gameType);
        facebookAppConfiguration.setAppName("anAppName");
        facebookAppConfiguration.setRedirecting(true);
        facebookAppConfiguration.setRedirectUrl("http://aRedirectUrl/");
        facebookAppConfiguration.setOpenGraphObjectPrefix(prefix);
        return facebookAppConfiguration;
    }

}
