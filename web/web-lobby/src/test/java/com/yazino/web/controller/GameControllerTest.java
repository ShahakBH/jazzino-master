package com.yazino.web.controller;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.account.WalletService;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.CommunityService;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.table.CountdownService;
import com.yazino.platform.table.GameConfiguration;
import com.yazino.platform.table.TableService;
import com.yazino.web.data.AchievementInfoRepository;
import com.yazino.web.data.LevelInfoRepository;
import com.yazino.web.domain.GameTypeResolver;
import com.yazino.web.domain.SiteConfiguration;
import com.yazino.web.domain.TournamentLobbyCache;
import com.yazino.web.service.GameConfigurationRepository;
import com.yazino.web.service.LobbyInformationService;
import com.yazino.web.service.TableLobbyService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.LandingUrlRegistry;
import com.yazino.web.util.MobilePlatformSniffer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.Collections;

import static com.yazino.platform.Platform.WEB;
import static com.yazino.web.util.MobilePlatformSniffer.MobilePlatform;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GameControllerTest {

    private static final String DEFAULT_GAME_TYPE = "BLACKJACK";
    private static final String DEFAULT_GAME_ID = "BLACKJACK_ID";
    private static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    private static final String PLAYER_NAME = "playerName";
    private static final String SESSION_KEY = "sessionKey";
    private static final Partner PARTNER_ID = Partner.YAZINO;
    private static final String PLAYER_EMAIL = "playerEmail";
    public static final LobbySession LOBBY_SESSION = new LobbySession(BigDecimal.valueOf(3141592), PLAYER_ID, PLAYER_NAME, SESSION_KEY, PARTNER_ID,
            null, PLAYER_EMAIL, null, false,
            WEB, AuthProvider.YAZINO);
    private static final BigDecimal BALANCE = BigDecimal.valueOf(2000);
    private static final long COUNTDOWN_TIME = 1000000L;

    private final LobbySessionCache lobbySessionCache = mock(LobbySessionCache.class);

    @Mock
    private PlayerService playerService;
    @Mock
    private WalletService walletService;
    @Mock
    private TableLobbyService tableLobbyService;
    @Mock
    private TableService tableService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private GameTypeResolver gameTypeResolver;
    @Mock
    private LobbyInformationService lobbyInformationService;
    @Mock
    private TournamentLobbyCache tournamentLobbyCache;
    @Mock
    private LevelInfoRepository levelInfoRepository;
    @Mock
    private AchievementInfoRepository achievementInfoRepository;
    @Mock
    private CommunityService communityService;
    @Mock
    private GameConfigurationRepository gameConfigurationRepository;
    @Mock
    private CountdownService countdownService;
    @Mock
    private GameConfiguration gameConfiguration;
    @Mock
    private MobilePlatformSniffer mobilePlatformSniffer;
    @Mock
    private LandingUrlRegistry landingUrlRegistry;
    @Mock
    private YazinoConfiguration yazinoConfiguration;

    private GameController underTest;

    @Before
    public void init() throws WalletServiceException {
        final SiteConfiguration siteConfiguration = new SiteConfiguration();
        siteConfiguration.setDefaultGameType(DEFAULT_GAME_TYPE);

        underTest = new GameController(lobbySessionCache,
                tableService, gameTypeResolver, lobbyInformationService,
                tournamentLobbyCache, levelInfoRepository, achievementInfoRepository, communityService,
                playerService, walletService, gameConfigurationRepository, countdownService, mobilePlatformSniffer,
                landingUrlRegistry, yazinoConfiguration);

        when(lobbySessionCache.getActiveSession(org.mockito.Mockito.any(HttpServletRequest.class))).thenReturn(LOBBY_SESSION);
        when(playerService.getAccountId(LOBBY_SESSION.getPlayerId())).thenReturn(BigDecimal.TEN);
        when(walletService.getBalance(BigDecimal.TEN)).thenReturn(BALANCE);
        when(communityService.getLatestSystemMessage()).thenReturn("aSystemMessage");

        when(gameConfigurationRepository.find(DEFAULT_GAME_TYPE)).thenReturn(gameConfiguration);
        when(gameConfiguration.getProperty("usesFlashLobby")).thenReturn("false");
        when(gameConfiguration.getGameId()).thenReturn(DEFAULT_GAME_ID);
        when(countdownService.findAll()).thenReturn(Collections.singletonMap("ALL", COUNTDOWN_TIME));

        when(yazinoConfiguration.getString(anyString(), eq("home"))).thenReturn("home");
    }

    @Test
    public void theCountdownIsNotAddedToTheModelForGamesUsingTheWebLobby() {
        underTest.gameLobby(DEFAULT_GAME_TYPE, request, response);

        verifyZeroInteractions(countdownService);
    }

    @Test
    public void theCountdownIsNotAddedToTheModelForGamesMissingTheLobbyPreferences() {
        reset(gameConfiguration);

        underTest.gameLobby(DEFAULT_GAME_TYPE, request, response);

        verifyZeroInteractions(countdownService);
    }

    @Test
    public void theCountdownIsAddedToTheModelForGamesUsingTheFlashLobby() {
        reset(gameConfiguration);
        when(gameConfiguration.getProperty("usesFlashLobby")).thenReturn("true");

        ModelAndView modelAndView = underTest.gameLobby(DEFAULT_GAME_TYPE, request, response);

        verify(countdownService).findAll();
        assertThat(modelAndView.getModel().get("countdown"), is(not(nullValue())));
        assertThat(modelAndView.getModel().get("countdown").toString(), is(equalTo(Long.toString(COUNTDOWN_TIME))));
    }

    @Test
    public void theLoggedOutSplashPageUsesAHomePrefixByDefault() {
        reset(lobbySessionCache);

        final ModelAndView modelAndView = underTest.gameLobby(DEFAULT_GAME_TYPE, request, response);

        assertThat(modelAndView.getViewName(), is(equalTo("home" + DEFAULT_GAME_ID)));
    }

    @Test
    public void theLoggedOutSplashPageUsesAnOverriddenPrefixIfPresent() {
        reset(lobbySessionCache);
        when(yazinoConfiguration.getString("lobby.game." + DEFAULT_GAME_ID + ".splash.view-prefix", "home")).thenReturn("aNewPrefix");

        final ModelAndView modelAndView = underTest.gameLobby(DEFAULT_GAME_TYPE, request, response);

        assertThat(modelAndView.getViewName(), is(equalTo("aNewPrefix" + DEFAULT_GAME_ID)));
    }

    @Test
    public void redirectsToHomePageIfNoSessionAndNoGameType() {
        when(lobbySessionCache.getActiveSession(org.mockito.Mockito.any(HttpServletRequest.class))).thenReturn(null);
        ModelAndView modelAndView = underTest.gameLobby(null, request, response);
        assertThatRedirectsTo(modelAndView, "/");
    }

    @Test
    public void redirectsToWelcomePageIfNoSessionAndNoGameConfigurationFound() {
        when(lobbySessionCache.getActiveSession(org.mockito.Mockito.any(HttpServletRequest.class))).thenReturn(null);
        when(gameConfigurationRepository.find(DEFAULT_GAME_TYPE)).thenReturn(null);
        ModelAndView modelAndView = underTest.gameLobby(DEFAULT_GAME_TYPE, request, response);
        assertThatRedirectsTo(modelAndView, "/");
    }

    @Test
    public void redirectsToPlatformSpecificGamePageWhenOneIsFound() {
        when(mobilePlatformSniffer.inferPlatform(org.mockito.Mockito.any(HttpServletRequest.class))).thenReturn(MobilePlatform.ANDROID);
        String androidBlackjackLobbyUrl = "http://www.yazino.com/android/blackjack";
        when(landingUrlRegistry.getLandingUrlForGameAndPlatform(DEFAULT_GAME_ID, MobilePlatform.ANDROID)).thenReturn(androidBlackjackLobbyUrl);
        ModelAndView modelAndView = underTest.gameLobby(DEFAULT_GAME_TYPE, request, response);
        assertThatRedirectsTo(modelAndView, androidBlackjackLobbyUrl);
    }

    private void assertThatRedirectsTo(ModelAndView modelAndView, String url) {
        assertTrue(modelAndView.getView() instanceof RedirectView);
        assertThat(((RedirectView) modelAndView.getView()).getUrl(), is(equalTo(url)));
    }

    @Test
    public void doesNotRedirectsToPlatformSpecificGamePageWhenNoneIsFound() {
        when(mobilePlatformSniffer.inferPlatform(org.mockito.Mockito.any(HttpServletRequest.class))).thenReturn(MobilePlatform.ANDROID);
        when(landingUrlRegistry.getLandingUrlForGameAndPlatform(DEFAULT_GAME_ID, MobilePlatform.ANDROID)).thenReturn(null);
        ModelAndView modelAndView = underTest.gameLobby(DEFAULT_GAME_TYPE, request, response);
        assertThat(modelAndView.getViewName(), is(equalTo("games")));
    }

    @Test
    public void doesNotLookForPlatformSpecificGamePageWhenPlatformNotIdentified() {
        when(mobilePlatformSniffer.inferPlatform(org.mockito.Mockito.any(HttpServletRequest.class))).thenReturn(null);
        underTest.gameLobby(DEFAULT_GAME_TYPE, request, response);
        verify(landingUrlRegistry, never()).getLandingUrlForGameAndPlatform(
                anyString(), org.mockito.Mockito.any(MobilePlatform.class));
    }

}
