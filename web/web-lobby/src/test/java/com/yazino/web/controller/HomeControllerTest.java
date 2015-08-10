package com.yazino.web.controller;

import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.web.domain.LobbyInformation;
import com.yazino.web.domain.SiteConfiguration;
import com.yazino.web.service.LobbyInformationService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.CookieHelper;
import com.yazino.web.util.FacebookCanvasDetection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openspaces.remoting.RemoteTimeoutException;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HomeControllerTest {
    private static final String GAME_TYPE = "gameType";
    private static final LobbySession LOBBY_SESSION = new LobbySession(BigDecimal.valueOf(3141592), new BigDecimal(1), "", "", Partner.YAZINO, "", "", null, false,
            Platform.WEB, AuthProvider.YAZINO);
    private static final LobbyInformation LOBBY_INFORMATION = new LobbyInformation(GAME_TYPE, 99, 999, true);

    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private HttpServletResponse httpServletResponse;
    @Mock
    private LobbySessionCache lobbySessionCache;
    @Mock
    private LobbyInformationService lobbyInformationService;
    @Mock
    private SiteConfiguration siteConfiguration;
    @Mock
    private CookieHelper cookieHelper;
    @Mock
    private FacebookCanvasDetection facebookCanvasDetection;

    private HomeController underTest;


    @Before
    public void init() {
        final Cookie[] cookies = new Cookie[0];
        when(httpServletRequest.getCookies()).thenReturn(cookies);
        when(siteConfiguration.getDefaultGameType()).thenReturn("aDefaultGameType");
        when(cookieHelper.getLastGameType(cookies, "aDefaultGameType"))
                .thenReturn("theLastGameType");

        underTest = new HomeController(lobbySessionCache, lobbyInformationService, facebookCanvasDetection,
                cookieHelper, siteConfiguration);
    }

    @Test
    public void shouldRedirectToHome() throws IOException {
        final ModelAndView model = underTest.redirectToHome(httpServletRequest, null);
        assertThat(model.getViewName(), is("home"));
    }

    @Test
    public void shouldProcessHomeForUnauthenticatedUsers() throws IOException {
        final ModelAndView model = underTest.processHome(httpServletRequest, null);
        assertThat(model.getViewName(), is("home"));
    }

    @Test
    public void shouldProcessHomeForAuthenticatedUsers() throws IOException {
        given(lobbySessionCache.getActiveSession(httpServletRequest)).willReturn(LOBBY_SESSION);

        final ModelAndView model = underTest.processHome(httpServletRequest, null);
        assertThat(model.getView(), is(instanceOf(RedirectView.class)));
        assertThat(((RedirectView) model.getView()).getUrl(), is(equalTo("/thelastgametype")));
        assertThat(((RedirectView) model.getView()).isExposePathVariables(), is(equalTo(false)));
    }

    @Test
    public void shouldProcessBrowserWarning() throws IOException {
        final String viewName = underTest.processBrowserWarning();
        assertThat(viewName, is("browserWarning"));
    }

    @Test
    public void shouldReturnLobbyInformation() throws IOException {
        given(lobbyInformationService.getLobbyInformation(GAME_TYPE)).willReturn(LOBBY_INFORMATION);

        HttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        underTest.lobbyInformation(mockHttpServletResponse, GAME_TYPE);
        assertThat(mockHttpServletResponse.getContentType(), is(equalTo("application/json")));
    }

    @Test
    public void shouldRedirectToFacebookCanvasIfRequired() {
        when(lobbySessionCache.getActiveSession(httpServletRequest)).thenReturn(LOBBY_SESSION);
        when(facebookCanvasDetection.isOnCanvas(httpServletRequest)).thenReturn(true);
        when(facebookCanvasDetection.redirectionEnabled()).thenReturn(true);
        final ModelAndView expected = new ModelAndView();
        when(facebookCanvasDetection.createRedirection(httpServletRequest, httpServletResponse)).thenReturn(expected);
        final ModelAndView actual = underTest.processHome(httpServletRequest, httpServletResponse);
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void shouldReturnHttpTimeoutOnRemoteTimeoutWhenRequestingLobbyInformation() throws IOException {
        when(lobbyInformationService.getLobbyInformation(GAME_TYPE)).thenThrow(new RemoteTimeoutException("testTimeout", 1));
        final MockHttpServletResponse response = new MockHttpServletResponse();

        underTest.lobbyInformation(response, GAME_TYPE);

        assertThat(response.getStatus(), is(equalTo(HttpServletResponse.SC_REQUEST_TIMEOUT)));
        assertThat(response.getContentType(), is(equalTo("application/json")));
        assertThat(response.getContentAsString(), is(equalTo("{}")));
    }

}
