package com.yazino.web.parature.controller;

import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.web.parature.service.ParatureSupportUserService;
import com.yazino.web.parature.service.SupportUserServiceException;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.ui.ModelMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;

import static com.yazino.platform.Platform.WEB;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AuthControllerTest {
    @Mock
    private LobbySessionCache lobbySessionCache;
    @Mock
    private PlayerProfileService playerProfileService;
    @Mock
    private ParatureSupportUserService supportUserService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private AuthController underTest;
    private LobbySession lobbySession;
    private PlayerProfile userProfile;
    private String adminEmail;
    private String target;
    private String loggedOutLink;

    @Before
    public void setUp() {
        final String adminPassword = "test";
        adminEmail = "admin@email.com";
        target = "http://www.the.target.for.sign.in";
        loggedOutLink = "http://the.logged.out.link/";

        underTest = new AuthController(lobbySessionCache, playerProfileService,
                supportUserService, adminPassword, adminEmail, target, loggedOutLink);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);

        lobbySession = new LobbySession(BigDecimal.valueOf(3141592), new BigDecimal(14), "Billy", "", Partner.YAZINO, "", "test@test.com", null, false, WEB, AuthProvider.YAZINO);
        userProfile = new PlayerProfile();
        userProfile.setEmailAddress("test@test.com");
        userProfile.setLastName("TheKid");
        userProfile.setFirstName("Billy");
        userProfile.setDisplayName("Billy The Kid");
    }

    @Test
    public void shouldRedirectWhenNoLobbySession() throws IOException, SupportUserServiceException, NoSuchAlgorithmException {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(null);

        underTest.view(new ModelMap(), request, response);

        verify(response).sendRedirect("/support/parature/login");
    }

    @Test
    public void shouldRedriectToSUpportWhenNoEmail() throws IOException, SupportUserServiceException, NoSuchAlgorithmException {

        userProfile = new PlayerProfile();

        when(lobbySessionCache.getActiveSession(request)).thenReturn(lobbySession);
        when(playerProfileService.findByPlayerId(lobbySession.getPlayerId())).thenReturn(userProfile);

        underTest.view(new ModelMap(), request, response);

        verify(response).sendRedirect(loggedOutLink);

    }

    @Test
    public void shouldRegisterUserIfNotAlreadyExists() throws IOException, SupportUserServiceException, NoSuchAlgorithmException {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(lobbySession);
        when(playerProfileService.findByPlayerId(lobbySession.getPlayerId())).thenReturn(userProfile);
        when(supportUserService.hasUserRegistered(lobbySession.getPlayerId())).thenReturn(false);

        ModelMap values = new ModelMap();
        assertEquals("parature/auth", underTest.view(values, request, response));

        assertModelMap(values);

        verify(supportUserService).createSupportUser(lobbySession.getPlayerId(), userProfile);
    }

    @Test
    public void shouldNotRegisterUserIfAlreadyExists() throws IOException, SupportUserServiceException, NoSuchAlgorithmException {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(lobbySession);
        when(playerProfileService.findByPlayerId(lobbySession.getPlayerId())).thenReturn(userProfile);
        when(supportUserService.hasUserRegistered(lobbySession.getPlayerId())).thenReturn(true);

        ModelMap values = new ModelMap();
        assertEquals("parature/auth", underTest.view(values, request, response));

        assertModelMap(values);

        verify(supportUserService, never()).createSupportUser(lobbySession.getPlayerId(), userProfile);
    }

    private void assertModelMap(ModelMap values) {
        assertEquals("test@test.com", values.get("cEmail"));
        assertEquals("Billy", values.get("cFname"));
        assertEquals("TheKid", values.get("cLname"));
        assertEquals(new BigDecimal(14), values.get("cUname"));
        assertEquals("9d7436d204715d80718fc709355529fb", values.get("sessID"));
        assertEquals(adminEmail, values.get("adminEmail"));
        assertEquals(target, values.get("target"));
    }

    @Test
    public void shouldPutDisplayNameIfNoFirstNameSet() throws IOException, SupportUserServiceException, NoSuchAlgorithmException {
        lobbySession = new LobbySession(BigDecimal.valueOf(3141592), new BigDecimal(14), "Billy", "", Partner.YAZINO, "", "test@test.com", null, false, WEB, AuthProvider.YAZINO);
        userProfile = new PlayerProfile();
        userProfile.setEmailAddress("test@test.com");
        userProfile.setLastName("TheKid");
        userProfile.setFirstName("");
        userProfile.setDisplayName("Billy The Kid");

        when(lobbySessionCache.getActiveSession(request)).thenReturn(lobbySession);
        when(playerProfileService.findByPlayerId(lobbySession.getPlayerId())).thenReturn(userProfile);
        when(supportUserService.hasUserRegistered(lobbySession.getPlayerId())).thenReturn(true);

        ModelMap values = new ModelMap();
        assertEquals("parature/auth", underTest.view(values, request, response));

        assertEquals("Billy The Kid", values.get("cFname"));
    }


}
