package com.yazino.web.service;

import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.player.LoginResult;
import com.yazino.web.session.LobbySessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.RememberMeServices;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.Arrays.asList;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RememberMeHandlerTest {
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private RememberMeServices rememberMeServices;
    @Mock
    private RememberMeTokenHandler rememberMeTokenHandler;
    @Mock
    private LobbySessionFactory lobbySessionFactory;
    @Mock
    private Authentication authentication;

    private RememberMeHandler underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new RememberMeHandler(rememberMeServices, rememberMeTokenHandler, lobbySessionFactory, "rememberMeKey", "rememberMeCookie");
    }

    @Test
    public void attemptAutoLoginShouldRegisterAuthenticatedSessionWithClientContext() {
        when(rememberMeServices.autoLogin(request, response)).thenReturn(authentication);

        when(authentication.getPrincipal()).thenReturn(
                new User(
                        new RememberMeUserInfo(Partner.YAZINO,
                                Platform.IOS,
                                BigDecimal.ONE,
                                "fuUserName").toString(),
                        "password",
                        asList(new SimpleGrantedAuthority("role"))));

        Map<String, Object> expectedClientContext = newHashMap();
        expectedClientContext.put("DEVICE_ID", "unique device id");
        underTest.attemptAutoLogin(request, response, expectedClientContext, "SLOTS");
        verify(lobbySessionFactory).registerAuthenticatedSession(
                eq(request),
                eq(response),
                any(Partner.class),
                any(BigDecimal.class),
                any(LoginResult.class),
                anyBoolean(),
                any(Platform.class),
                eq(expectedClientContext),
                eq("SLOTS"));
    }
}
