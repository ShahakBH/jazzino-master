package com.yazino.web.controller;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.player.LoginResult;
import com.yazino.web.form.WebLoginForm;
import com.yazino.web.service.YazinoWebLoginService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.CookieHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;

import static com.yazino.platform.Platform.WEB;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class WebLoginControllerTest {
    public static final String REDIRECT_TO = "/i/am/sorry";
    public static final String BLACKJACK = "BLACKJACK";
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(3141592);

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private HttpServletResponse httpServletResponse;

    @Mock
    private LobbySessionCache lobbySessionCache;

    @Mock
    private CookieHelper cookieHelper;

    @Mock
    private WebLoginForm form;

    @Mock
    private YazinoWebLoginService yazinoWebLoginService;

    @Mock
    RegistrationHelper registrationHelper;

    @Mock
    private ModelMap model;

    @Mock
    private BindingResult result;

    @Mock
    private YazinoConfiguration yazinoConfiguration;

    private WebLoginController underTest;


    @Before
    public void init() throws IOException {
        initMocks(this);
        // GIVEN there is enough information to resolve game type
        given(httpServletRequest.getParameter("gameType")).willReturn("BLACKJACK");

        // AND the servlet response returns a writer mock
        final PrintWriter responseWriter = mock(PrintWriter.class);
        given(httpServletResponse.getWriter()).willReturn(responseWriter);
        when(yazinoConfiguration.getString(anyString(), anyString())).thenReturn(Partner.YAZINO.name());

        underTest = new WebLoginController(lobbySessionCache, cookieHelper, yazinoWebLoginService, registrationHelper, yazinoConfiguration);
    }

    @Test
    public void aRedirectionForAnActiveSessionOnlyOccursIfTheResponseIsNotYetCommited() throws IOException {
        given(lobbySessionCache.getActiveSession(httpServletRequest)).willReturn(
                new LobbySession(SESSION_ID, null, null, null, null, null, null, null, true, WEB, AuthProvider.YAZINO));
        given(form.getRedirectTo()).willReturn("theEndPoint");
        given(httpServletResponse.isCommitted()).willReturn(true);

        final String view = underTest.processSubmit(form, result, httpServletRequest, httpServletResponse, model, "true");

        verify(httpServletResponse).isCommitted();
        verifyNoMoreInteractions(httpServletResponse);
        assertThat(view, is(nullValue()));
    }

    @Test
    public void aRedirectionThatFailsWillReturnAStatusOfInternalServerError() throws IOException {
        given(lobbySessionCache.getActiveSession(httpServletRequest)).willReturn(
                new LobbySession(SESSION_ID, null, null, null, null, null, null, null, true, WEB, AuthProvider.YAZINO));
        given(form.getRedirectTo()).willReturn("theEndPoint");
        doThrow(new IOException("responseCommitted")).when(httpServletResponse).sendRedirect(anyString());

        final String view = underTest.processSubmit(form, result, httpServletRequest, httpServletResponse, model, "true");

        assertThat(view, is(nullValue()));
        verify(httpServletResponse).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void shouldTreatLoginErrors() throws IOException {
        given(form.getRegistered()).willReturn("existing-user");
        given(yazinoWebLoginService.login(httpServletRequest, httpServletResponse, null, null, WEB, Partner.YAZINO)).willReturn(LoginResult.FAILURE);

        assertThat(underTest.processSubmit(form, result, httpServletRequest, httpServletResponse, model, "true"), is(equalTo("partials/loginPanel")));
    }

    @Test
    public void shouldReturnBlockViewIfUserBlocked() throws IOException {
        given(form.getRegistered()).willReturn("existing-user");
        given(yazinoWebLoginService.login(httpServletRequest, httpServletResponse, null, null, WEB, Partner.YAZINO)).willReturn(LoginResult.BLOCKED);

        assertThat(underTest.processSubmit(form, result, httpServletRequest, httpServletResponse, model, "true"), is(equalTo("partials/loginPanel")));
    }

    @Test
    public void shouldTreatRegistrationErrors() throws IOException {
        given(registrationHelper.register(form, result, httpServletRequest, httpServletResponse, WEB, BLACKJACK, Partner.YAZINO)).willReturn(RegistrationResult.FAILURE);
        when(form.getRegistered()).thenReturn("new-user");
        // WHEN submitting the login form
        assertThat(underTest.processSubmit(form, result, httpServletRequest, httpServletResponse, model, "true"), is(equalTo("partials/loginPanel")));
    }

    @Test
    public void registrationShouldBePassedRegistrationAndLoginPages() throws IOException {
        given(form.getRegistered()).willReturn("new-user");
        given(form.getRedirectTo()).willReturn("infinity");
        ModelMap stubModel = new ModelMap();
        given(registrationHelper.register(form, result, httpServletRequest, httpServletResponse, WEB, BLACKJACK, Partner.YAZINO)).willReturn(RegistrationResult.SUCCESS);

        underTest.processSubmit(form, result, httpServletRequest, httpServletResponse, stubModel, "true");

        assertThat(underTest.processSubmit(form, result, httpServletRequest, httpServletResponse, stubModel, "true"), is(equalTo("partials/loginRedirection")));
        assertThat((String) stubModel.get(WebLoginController.JS_REDIRECT_TARGET), equalTo("infinity"));
    }

    @Test
    public void loginShouldBePassedSuccessPageAndRedirected() {
        given(form.getEmail()).willReturn("u");
        given(form.getRegisteredPassword()).willReturn("p");

        given(form.getRegistered()).willReturn("existing-user");
        given(form.getRedirectTo()).willReturn("redirectYa");
        when(yazinoWebLoginService.login(httpServletRequest, httpServletResponse, "u", "p", WEB, Partner.YAZINO)).thenReturn(LoginResult.EXISTING_USER);

        underTest.processSubmit(form, result, httpServletRequest, httpServletResponse, model, "true");
        verify(yazinoWebLoginService).login(httpServletRequest, httpServletResponse, "u", "p",
                WEB, Partner.YAZINO);
        verify(model).addAttribute(eq(WebLoginController.JS_REDIRECT_TARGET), eq("redirectYa"));
        verify(model).addAttribute(eq("partial"), eq("true"));
    }

    @Test
    public void shouldForwardGetRequestsToLoggedIn() throws IOException {
        // GIVEN there is an active session
        given(lobbySessionCache.getActiveSession(httpServletRequest)).willReturn(
                new LobbySession(SESSION_ID, null, null, null, null, null, null, null, true, WEB, AuthProvider.YAZINO));

        // WHEN a "get" login is tried
        assertThat(underTest.login(model,
                httpServletRequest,
                httpServletResponse,
                REDIRECT_TO,
                false, null).getViewName(), is(equalTo("partials/loginRedirection")));

        // THEN before returning, the new form is set to the model
        verify(model).addAttribute(eq("jsRedirectLocation"), eq(REDIRECT_TO));
        verify(model).addAttribute(eq("partial"), eq("true"));
    }

    @Test
    public void loginShouldReturnCorrectWebLoginForm() throws Exception {
        given(lobbySessionCache.getActiveSession(httpServletRequest)).willReturn(null);

        underTest.login(model, httpServletRequest, httpServletResponse, REDIRECT_TO, false, null);
        WebLoginForm expectedWebLoginForm = new WebLoginForm();
        expectedWebLoginForm.setRedirectTo(REDIRECT_TO);
        expectedWebLoginForm.setRegistered("existing-user");
        verify(model, times(1)).addAttribute("loginForm", expectedWebLoginForm);
    }

    @Test
    public void loginShouldReturnCorrectWebLoginFormForNoRedirect() throws Exception {
        given(lobbySessionCache.getActiveSession(httpServletRequest)).willReturn(null);

        underTest.login(model, httpServletRequest, httpServletResponse, null, false, null);
        WebLoginForm expectedWebLoginForm = new WebLoginForm();
        expectedWebLoginForm.setRegistered("existing-user");
        verify(model, times(1)).addAttribute("loginForm", expectedWebLoginForm);
    }

}
