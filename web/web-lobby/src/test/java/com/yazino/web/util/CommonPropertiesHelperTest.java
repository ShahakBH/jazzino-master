package com.yazino.web.util;

import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.web.domain.SiteConfiguration;
import com.yazino.web.service.SafeBuyChipsPromotionServiceWrapper;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.pegdown.PegDownProcessor;
import org.springframework.web.servlet.ModelAndView;
import strata.server.lobby.api.facebook.FacebookAppConfiguration;
import strata.server.lobby.api.facebook.FacebookConfiguration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;

import static com.yazino.platform.Platform.FACEBOOK_CANVAS;
import static com.yazino.platform.Platform.WEB;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CommonPropertiesHelperTest {

    private static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    private static final String EMAIL = "email";
    private static final Partner PARTNER_ID = Partner.YAZINO;
    private static final String PLAYER_NAME = "playerName";
    private static final String GAME_TYPE = "BLACKJACK";
    private static final String FACEBOOK_API_KEY = "anApiKey";
    private static final String FACEBOOK_APPLICATION_ID = "anApplicationId";
    private static final String FACEBOOK_APP_URL_ROOT = "facebookAppUrlRoot";
    private static final String FACEBOOK_APP_NAME = "facebookAppName";
    private static final String FACEBOOK_FAN_PAGE_ID = "facebookFanPageId";
    private static final boolean FACEBOOK_REVIEWS_ENABLED = true;
    private static final boolean FACEBOOK_PUBLISH_STREAM_ENABLED = true;
    private static final boolean FACEBOOK_APPS_ENABLED = true;
    private static final String FACEBOOK_LOGIN_URL = "http://thebookofface.com/giggity/diggity/giggity/";
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(3141592);

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private LobbySessionCache lobbySessionCache;
    @Mock
    private SiteConfiguration siteConfiguration;
    @Mock
    private CookieHelper cookieHelper;
    @Mock
    private MessagingHostResolver messagingHostResolver;
    @Mock
    private SafeBuyChipsPromotionServiceWrapper promotionService;
    @Mock
    private FacebookCanvasDetection facebookCanvasDetection;
    @Mock
    private Environment environment;

    private CommonPropertiesHelper underTest;

    @Before
    public void setUp() {
        underTest = new CommonPropertiesHelper(messagingHostResolver, lobbySessionCache, siteConfiguration,
                cookieHelper, createFacebookConfiguration(), promotionService, facebookCanvasDetection, environment);

        when(request.getParameter("gameType")).thenReturn("gameType");
    }

    @Test
    public void testSetupCommonProperties_shouldAddMessagingHost() {
        ModelAndView modelAndView = new ModelAndView();
        when(lobbySessionCache.getActiveSession(request)).thenReturn(
                new LobbySession(SESSION_ID, PLAYER_ID, PLAYER_NAME, null, PARTNER_ID, null, EMAIL, null, false, WEB, AuthProvider.YAZINO));

        when(messagingHostResolver.resolveMessagingHostForPlayer(PLAYER_ID)).thenReturn("myHost");
        underTest.setupCommonProperties(request, response, modelAndView
        );
        assertEquals("myHost", modelAndView.getModel().get("messagingHost"));
    }

    @Test
    public void testSetupCommonProperties_shouldNotAddMessagingHostWhenLobbySessionNotPresent() {
        ModelAndView modelAndView = new ModelAndView();
        when(lobbySessionCache.getActiveSession(request)).thenReturn(null);
        underTest.setupCommonProperties(request, response, modelAndView
        );
        assertNull(modelAndView.getModel().get("messagingHost"));
    }

    @Test
    public void shouldAddCorrectFacebookAppUrlRoot() {
        assertFacebookConfigurationCorrect("facebookAppUrlRoot", FACEBOOK_APP_URL_ROOT);
    }

    @Test
    public void shouldAddCorrectFacebookAppName() {
        assertFacebookConfigurationCorrect("facebookAppName", FACEBOOK_APP_NAME);
    }

    @Test
    public void shouldAddCorrectFacebookApiKey() {
        assertFacebookConfigurationCorrect("facebookApiKey", FACEBOOK_API_KEY);
    }

    @Test
    public void shouldAddCorrectFacebookApplicationId() {
        assertFacebookConfigurationCorrect("facebookApplicationId", FACEBOOK_APPLICATION_ID);
    }

    @Test
    public void shouldAddCorrectFacebookFanPageId() {
        assertFacebookConfigurationCorrect("facebookFanPageId", FACEBOOK_FAN_PAGE_ID);
    }

    @Test
    public void shouldAddCorrectFacebookReviewsEnabled() {
        assertFacebookConfigurationCorrect("facebookReviewsEnabled", FACEBOOK_REVIEWS_ENABLED);
    }

    @Test
    public void shouldAddCorrectFacebookPublishStreamEnabled() {
        assertFacebookConfigurationCorrect("facebookPublishStreamEnabled", FACEBOOK_PUBLISH_STREAM_ENABLED);
    }

    @Test
    public void shouldAddCorrectFacebookAppsEnabled() {
        assertFacebookConfigurationCorrect("facebookAppsEnabled", FACEBOOK_APPS_ENABLED);
    }

    @Test
    public void shouldHaveCorrectFBLoginUrl() {
        assertFacebookConfigurationCorrect("facebookLoginUrl", FACEBOOK_LOGIN_URL);
    }

    @Test
    public void shouldAddCorrectFacebookOriginalApplicationId() {
        when(cookieHelper.getOriginalGameType(request)).thenReturn(GAME_TYPE);
        assertFacebookConfigurationCorrect("facebookOriginalApplicationId", FACEBOOK_APPLICATION_ID);
    }

    @Test
    public void shouldAddMarkdownProcessor() {
        final ModelAndView modelAndView = new ModelAndView();
        when(lobbySessionCache.getActiveSession(request)).thenReturn(null);

        underTest.setupCommonProperties(request, response, modelAndView);

        assertThat(modelAndView.getModel().get("markdown"), is(instanceOf(PegDownProcessor.class)));
    }

    @Test
    public void shouldUsePlatformObtainedFromLobbySessionWhenCheckingPromotionAvailability() {
        ModelAndView modelAndView = new ModelAndView();

        // try with WEB
        when(lobbySessionCache.getActiveSession(request)).thenReturn(new LobbySession(SESSION_ID, PLAYER_ID, PLAYER_NAME, null, PARTNER_ID, null, EMAIL, null, false, WEB, AuthProvider.YAZINO));
        underTest.setupCommonProperties(request, response, modelAndView);
        verify(promotionService).hasPromotion(PLAYER_ID, WEB);

        // try with FACEBOOK_CANVAS
        when(lobbySessionCache.getActiveSession(request)).thenReturn(new LobbySession(SESSION_ID, PLAYER_ID, PLAYER_NAME, null, PARTNER_ID, null, EMAIL, null, false, FACEBOOK_CANVAS, AuthProvider.YAZINO));
        underTest.setupCommonProperties(request, response, modelAndView);
        verify(promotionService).hasPromotion(PLAYER_ID, FACEBOOK_CANVAS);
    }

    @Test
    public void shouldSetHasPromotionToTrueWhenPromotionAvailable() {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(new LobbySession(SESSION_ID, PLAYER_ID, PLAYER_NAME, null, PARTNER_ID, null, EMAIL, null, false, WEB, AuthProvider.YAZINO));
        when(promotionService.hasPromotion(eq(PLAYER_ID), any(Platform.class))).thenReturn(true);

        final ModelAndView modelAndView = new ModelAndView();
        underTest.setupCommonProperties(request, response, modelAndView);

        assertTrue((Boolean) modelAndView.getModel().get("hasPromotion"));
    }

    @Test
    public void shouldSetHasPromotionToFalseWhenPromotionNotAvailable() {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(new LobbySession(SESSION_ID, PLAYER_ID, PLAYER_NAME, null, PARTNER_ID, null, EMAIL, null, false, WEB, AuthProvider.YAZINO));
        when(promotionService.hasPromotion(eq(PLAYER_ID), any(Platform.class))).thenReturn(false);

        final ModelAndView modelAndView = new ModelAndView();
        underTest.setupCommonProperties(request, response, modelAndView);

        assertFalse((Boolean) modelAndView.getModel().get("hasPromotion"));
    }

    @Test
    public void shouldNotSetHasPromotionWhenNoLobbySession() {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(null);

        final ModelAndView modelAndView = new ModelAndView();
        underTest.setupCommonProperties(request, response, modelAndView);

        assertNull(modelAndView.getModel().get("hasPromotion"));
    }

    private void assertFacebookConfigurationCorrect(String key, Object expectedValue) {
        ModelAndView modelAndView = new ModelAndView();
        underTest.setupCommonProperties(request, response, modelAndView);
        assertEquals(String.format("Looking at model value for key [%s]", key), expectedValue,
                modelAndView.getModel().get(key));

    }

    private FacebookConfiguration createFacebookConfiguration() {
        final FacebookAppConfiguration facebookAppConfiguration = new FacebookAppConfiguration();
        facebookAppConfiguration.setGameType(GAME_TYPE);
        facebookAppConfiguration.setApiKey(FACEBOOK_API_KEY);
        facebookAppConfiguration.setApplicationId(FACEBOOK_APPLICATION_ID);
        facebookAppConfiguration.setAppName(FACEBOOK_APP_NAME);
        facebookAppConfiguration.setFanPageId(FACEBOOK_FAN_PAGE_ID);

        final FacebookConfiguration facebookConfiguration = new FacebookConfiguration();
        facebookConfiguration.setApplicationConfigs(asList(facebookAppConfiguration));
        facebookConfiguration.setAppUrlRoot(FACEBOOK_APP_URL_ROOT);
        facebookConfiguration.setReviewsEnabled(FACEBOOK_REVIEWS_ENABLED);
        facebookConfiguration.setPublishStreamEnabled(FACEBOOK_PUBLISH_STREAM_ENABLED);
        facebookConfiguration.setAppsEnabled(FACEBOOK_APPS_ENABLED);
        facebookConfiguration.setLoginUrl(FACEBOOK_LOGIN_URL);
        return facebookConfiguration;
    }

}
