package com.yazino.web.domain;

import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.web.session.LobbySession;
import com.yazino.web.util.CookieHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GameTypeResolverTest {
    private static final Partner PARTNER = Partner.YAZINO;
    private static final Platform PLATFORM = Platform.ANDROID;
    private static final String PARAM_GAME_TYPE = "paramGameType";
    private static final String RESOLVED_GAME_TYPE = "resolvedGameType";
    private static final String DEFAULT_GAME_TYPE = "defaultGameType";

    @Mock
    private CookieHelper cookieHelper;
    @Mock
    private SiteConfiguration siteConfiguration;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private LobbySession lobbySession;

    private GameTypeResolver underTest;

    @Before
    public void setUp() {
        final Cookie[] cookies = new Cookie[0];
        when(lobbySession.getPartnerId()).thenReturn(PARTNER);
        when(lobbySession.getPlatform()).thenReturn(PLATFORM);
        when(request.getParameter("gameType")).thenReturn(null);
        when(request.getCookies()).thenReturn(cookies);
        when(siteConfiguration.getDefaultGameType()).thenReturn(DEFAULT_GAME_TYPE);
        when(cookieHelper.getLastGameType(cookies, DEFAULT_GAME_TYPE)).thenReturn(RESOLVED_GAME_TYPE);

        underTest = new GameTypeResolver(cookieHelper, siteConfiguration);
    }

    @Test
    public void shouldResolveViaRequestParameter() {
        when(request.getParameter("gameType")).thenReturn(PARAM_GAME_TYPE);

        final String actual = underTest.resolveGameType(request, response);

        assertThat(actual, is(equalTo(PARAM_GAME_TYPE)));
        verify(cookieHelper).setLastGameType(response, PARAM_GAME_TYPE);
    }

    @Test
    public void shouldRemoveSlashIfPresent() {
        when(request.getParameter("gameType")).thenReturn(PARAM_GAME_TYPE + "/");

        final String actual = underTest.resolveGameType(request, response);

        assertThat(actual, is(equalTo(PARAM_GAME_TYPE)));
        verify(cookieHelper).setLastGameType(response, PARAM_GAME_TYPE);
    }

    @Test
    public void shouldRelyOnCookieIfNotPresentInParameters() {
        final String actual = underTest.resolveGameType(request, response);

        assertThat(actual, is(equalTo(RESOLVED_GAME_TYPE)));
        verify(cookieHelper).setLastGameType(response, RESOLVED_GAME_TYPE);
    }

    @Test
    public void shouldReturnAppInfoWithPartnerFromSession() {
        final ApplicationInformation appInfo = underTest.appInfoFor(request, response, lobbySession);

        assertThat(appInfo.getPartner(), is(equalTo(PARTNER)));
    }

    @Test
    public void shouldReturnAppInfoWithPlatformFromSession() {
        final ApplicationInformation appInfo = underTest.appInfoFor(request, response, lobbySession);

        assertThat(appInfo.getPlatform(), is(equalTo(PLATFORM)));
    }

    @Test
    public void shouldReturnAppInfoWithGameTypeFromParameterWhenPresent() {
        when(request.getParameter("gameType")).thenReturn(PARAM_GAME_TYPE);

        final ApplicationInformation appInfo = underTest.appInfoFor(request, response, lobbySession);

        assertThat(appInfo.getGameType(), is(equalTo(PARAM_GAME_TYPE)));
    }

    @Test
    public void shouldReturnAppInfoWithResolvedGameTypeWhenNoParameterPresent() {
        final ApplicationInformation appInfo = underTest.appInfoFor(request, response, lobbySession);

        assertThat(appInfo.getGameType(), is(equalTo(RESOLVED_GAME_TYPE)));
    }
}
