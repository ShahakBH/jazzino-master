package com.yazino.web.parature.controller;

import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.player.LoginResult;
import com.yazino.platform.player.PlayerProfileAuthenticationResponse;
import com.yazino.platform.player.service.AuthenticationService;
import com.yazino.web.parature.form.LoginForm;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.session.LobbySessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.ModelMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.HashMap;

import static com.yazino.platform.Platform.WEB;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class LoginControllerTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(20);
    public static final HashMap<String,Object> EMPTY_CLIENT_CONTEXT = new HashMap<String, Object>();
    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private HttpServletResponse httpServletResponse;

    @Mock
    private LobbySessionCache lobbySessionCache;

    @Mock
    private LobbySessionFactory lobbySessionFactory;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private LoginForm form;

    @Mock
    private ModelMap model;

    private LoginController underTest;

    private static final String MAIL_ADDRESS = "matin@bidule.com";

    @Before
    public void init() throws IOException {
        MockitoAnnotations.initMocks(this);

        // GIVEN there is enough information to resolve game type
        given(httpServletRequest.getParameter("gameType")).willReturn("BLACKJACK");

        // AND the servlet response returns a writer mock
        final PrintWriter responseWriter = mock(PrintWriter.class);
        given(httpServletResponse.getWriter()).willReturn(responseWriter);

        underTest = new LoginController(lobbySessionCache, lobbySessionFactory, authenticationService);
    }

    @Test
    public void shouldRegisterAuthenticatedSession() throws IOException {
        // GIVEN a successful authentication
        final PlayerProfileAuthenticationResponse authResponse = new PlayerProfileAuthenticationResponse(PLAYER_ID);
        given(authenticationService.authenticateYazinoUser(MAIL_ADDRESS, null)).willReturn(authResponse);

        // AND there is no active session present
        given(lobbySessionCache.getActiveSession(httpServletRequest)).willReturn(null);

        // AND the email address is returned for the player
        given(form.getEmail()).willReturn(MAIL_ADDRESS);

        // WHEN submitting the login form
        underTest.processSubmit(form, httpServletRequest, httpServletResponse, model);

        // THEN the session factory
        verify(lobbySessionFactory).registerAuthenticatedSession(httpServletRequest, httpServletResponse,
                Partner.YAZINO, PLAYER_ID, LoginResult.EXISTING_USER, true, WEB, EMPTY_CLIENT_CONTEXT, "");

        // AND the response redirects to the right page
        verify(httpServletResponse).sendRedirect(contains("/auth"));
    }

    @Test
    public void shouldForwardRedirectIfSessionOpen() throws IOException {
        // GIVEN there is an active session
        given(lobbySessionCache.getActiveSession(httpServletRequest)).willReturn(
                new LobbySession(BigDecimal.valueOf(3141592), null, null, null, null, null, null, null, true, WEB, AuthProvider.YAZINO));

        // AND the form contains a forward address
        final String redirectTo = "redir";
        given(form.getRedirectTo()).willReturn(redirectTo);

        // WHEN submitting the login form
        final String retval = underTest.processSubmit(form, httpServletRequest, httpServletResponse, model);

        // THEN the request is forwarded to the address for from the auth.form
        assertThat(retval, nullValue());
    }

    @Test
    public void shouldTreatLoginErrors() throws IOException {
        // GIVEN we have an unsuccessful login
        given(form.getEmail()).willReturn(MAIL_ADDRESS);
        final PlayerProfileAuthenticationResponse authResponse = new PlayerProfileAuthenticationResponse();
        given(authenticationService.authenticateYazinoUser(MAIL_ADDRESS, null)).willReturn(authResponse);

        // WHEN submitting the login form
        underTest.processSubmit(form, httpServletRequest, httpServletResponse, model);

        // THEN the redirect information is correctly set
        verify(model).addAttribute(eq("loginError"), eq("Your username and/or password were incorrect."));
    }

    @Test
    public void shouldTreatUserWhoHasBeenBlocked() throws IOException {
        // GIVEN we have an unsuccessful login
        given(form.getEmail()).willReturn(MAIL_ADDRESS);
        final PlayerProfileAuthenticationResponse authResponse = new PlayerProfileAuthenticationResponse(BigDecimal.valueOf(2312), Boolean.TRUE);

        given(authenticationService.authenticateYazinoUser(MAIL_ADDRESS, null)).willReturn(authResponse);

        // WHEN submitting the login form
        underTest.processSubmit(form, httpServletRequest, httpServletResponse, model);

        // THEN the redirect information is correctly set
        verify(model).addAttribute(eq("loginError"), eq("Your account has been blocked. Please Contact Customer Support"));
    }

    @Test
    public void shouldForwardGetRequestsToPostForm() throws IOException {
        // GIVEN there is an active session
        given(lobbySessionCache.getActiveSession(httpServletRequest)).willReturn(
                new LobbySession(BigDecimal.valueOf(3141592), null, null, null, null, null, null, null, true, WEB, AuthProvider.YAZINO));

        // WHEN a "get" login is tried
        underTest.login(model, httpServletRequest);

        // THEN before returning, the new form is set to the model
        verify(model).addAttribute(eq("loginForm"), any(LoginForm.class));
    }
}
