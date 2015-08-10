package com.yazino.web.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.codec.Base64;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HeaderTokenBasedRememberMeServicesTest {
    private static final String COOKIE_NAME = "aCookieName";

    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private HeaderTokenBasedRememberMeServices underTest;

    @Before
    public void setUp() {
        underTest = new HeaderTokenBasedRememberMeServices("aKey", COOKIE_NAME, userDetailsService);

        when(request.getContextPath()).thenReturn("/aContextPath");
    }

    @Test
    public void whenACookieIsPresentThenTheHeaderValueIsNotQueried() {
        when(request.getCookies()).thenReturn(new Cookie[]{aCookieNamed("bob"), aCookieNamed(COOKIE_NAME), aCookieNamed("fred")});

        final String cookieValue = underTest.extractRememberMeCookie(request);

        assertThat(cookieValue, is(equalTo("aCookieValueFor" + COOKIE_NAME)));
        verify(request, times(0)).getHeader(COOKIE_NAME);
    }

    @Test
    public void whenACookieIsNotPresentThenTheHeaderValueIsQueried() {
        when(request.getHeader(COOKIE_NAME)).thenReturn("aHeaderValue");

        final String cookieValue = underTest.extractRememberMeCookie(request);

        assertThat(cookieValue, is(equalTo("aHeaderValue")));
    }

    @Test
    public void whenACookieIsNotPresentAndTheHeaderIsNotPresentThenNullIsReturned() {
        final String cookieValue = underTest.extractRememberMeCookie(request);

        assertThat(cookieValue, is(nullValue()));
    }

    @Test
    public void theHeaderIsSetOnTheResponse() {
        underTest.setCookie(new String[]{"token1", "token2"}, 1000, request, response);

        verify(response).setHeader(COOKIE_NAME, base64EncodeAndStrip("token1:token2"));
    }

    private Cookie aCookieNamed(final String cookieName) {
        return new Cookie(cookieName, "aCookieValueFor" + cookieName);
    }

    private String base64EncodeAndStrip(final String cookieToEncode) {
        final StringBuilder sb = new StringBuilder(new String(Base64.encode(cookieToEncode.getBytes())));
        while (sb.charAt(sb.length() - 1) == '=') {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

}
