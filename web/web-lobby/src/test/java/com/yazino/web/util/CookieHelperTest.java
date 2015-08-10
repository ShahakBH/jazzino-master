package com.yazino.web.util;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;

import static com.yazino.web.util.CookieHelper.PAYMENT_GAME_TYPE;
import static com.yazino.web.util.CookieHelper.SCREEN_SOURCE;
import static org.junit.Assert.assertEquals;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class CookieHelperTest {
    private static final String MASSIVE_FAIL = "massive_fail";
    private static final String ACCOUNT_ID = "10";
    private static final String ORIGINAL_GAME_TYPE = "GREATGAME";
    private static final String EXPECTED = "partnerTest";

    private HttpServletResponse response;
    private HttpServletRequest request;

    private CookieHelper underTest;

    @Before
    public void init() {
        underTest = new CookieHelper();
        response = mock(HttpServletResponse.class);
        request = mock(HttpServletRequest.class);
    }

    @Test
    public void testLastGameType() {
        final String expected = "partnerTest";
        Cookie[] cookies = new Cookie[]{underTest.makeCookie(CookieHelper.LAST_GAME_TYPE_COOKIE_NAME, expected, true)};
        String result = underTest.getLastGameType(cookies, MASSIVE_FAIL);
        assertEquals(expected, result);
    }

    @Test
    public void testReferralAccountId() {
        final String expected = "10";
        Cookie[] cookies = new Cookie[]{underTest.makeCookie(CookieHelper.REFERRAL_COOKIE_NAME, expected, true)};
        BigDecimal result = underTest.getReferralPlayerId(cookies);
        assertEquals(new BigDecimal(expected), result);
    }

    @Test
    public void testSetReferralId() {
        final Cookie cookie = underTest.makeCookie(CookieHelper.REFERRAL_COOKIE_NAME, ACCOUNT_ID, true);
        underTest.setReferralPlayerId(response, ACCOUNT_ID);
        ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(captor.capture());
        assertThat(captor.getValue(), CookieMatcher.matchesCookie(cookie));
    }

    @Test
    public void testSetOriginalGameType() {
        final Cookie cookie = underTest.makeCookie(CookieHelper.ORIGINAL_GAME_TYPE, ORIGINAL_GAME_TYPE, true);
        underTest.setOriginalGameType(response, ORIGINAL_GAME_TYPE);
        ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(captor.capture());
        assertThat(captor.getValue(), CookieMatcher.matchesCookie(cookie));
    }

    @Test
    public void testSetLastGameType() {
        final Cookie cookie = underTest.makeCookie(CookieHelper.LAST_GAME_TYPE_COOKIE_NAME, EXPECTED, true);
        underTest.setLastGameType(response, EXPECTED);
        ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(captor.capture());
        assertThat(captor.getValue(), CookieMatcher.matchesCookie(cookie));
    }

    @Test
    public void testSetRedirectTo() {
        String redirectTo = "redirect/to";
        final Cookie expectedCookie = underTest.makeCookie(CookieHelper.REDIRECT_TO, redirectTo, false);
        underTest.setRedirectTo(response, redirectTo);
        ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(captor.capture());
        assertThat(captor.getValue(), CookieMatcher.matchesCookie(expectedCookie));
    }

    @Test
    public void testGetRedirectTo() {
        String redirectTo = "redirect/to";
        HttpServletRequest request = mock(HttpServletRequest.class);
        Cookie[] cookies = new Cookie[1];
        cookies[0] = new Cookie(CookieHelper.REDIRECT_TO, redirectTo);
        when(request.getCookies()).thenReturn(cookies);
        assertEquals(redirectTo, underTest.getRedirectTo(request));
    }

    @Test
    public void testGetRedirectToShouldReturnNullIfNoCookie() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Cookie[] cookies = new Cookie[0];

        when(request.getCookies()).thenReturn(cookies);
        assertEquals(null, underTest.getRedirectTo(request));
    }


//    @Test
//    public void testGetRedirectToRemovesCookie() {
//        String redirectTo = "redirect/to";
//        HttpServletRequest request = mock(HttpServletRequest.class);
//        Cookie[] cookies = new Cookie[1];
//        cookies[0] = new Cookie(CookieHelper.REDIRECT_TO, redirectTo);
//        when(request.getCookies()).thenReturn(cookies);
//        assertEquals(redirectTo, underTest.getRedirectTo(request));
//        assertEquals(null, underTest.getRedirectTo(request));
//    }

    @Test
    public void testGetReferralTableIdRemovesCookie() {
        final String expected = "10";
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        final Cookie originalCookie = underTest.makeCookie(CookieHelper.TABLE_INVITATION_ID, expected, true);
        Cookie[] cookies = new Cookie[]{originalCookie};
        when(request.getCookies()).thenReturn(cookies);
        final Cookie cookie = underTest.makeCookie(CookieHelper.TABLE_INVITATION_ID, "", true);
        cookie.setMaxAge(0);
        BigDecimal result = underTest.getReferralTableId(request, response);
        assertEquals(BigDecimal.TEN, result);
        ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(captor.capture());
        assertThat(captor.getValue(), CookieMatcher.matchesCookie(cookie));
    }

    @Test
    public void shouldCreateAnOnCanvasCookieWhichIsNotPersistent() {
        ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
        underTest.setOnCanvas(response, true);
        verify(response).addCookie(captor.capture());
        assertEquals(captor.getValue().getMaxAge(), -1);
    }

    @Test
    public void shouldInvalidateTheCanvasCookie() {
        ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);

        underTest.invalidateCanvas(response);

        verify(response).addCookie(captor.capture());
        assertThat(captor.getValue().getMaxAge(), is(equalTo(0)));
        assertThat(captor.getValue().getName(), is(equalTo("CANVAS")));
    }

    @Test
    public void paymentGameTypeShouldBeNullIfCookieIsAbsent() {
        assertThat(underTest.getPaymentGameType(new Cookie[0]), is(nullValue()));
    }

    @Test
    public void paymentGameTypeShouldBeValueOfCookie() {
        final Cookie[] cookies = prepareCookies(new Cookie(PAYMENT_GAME_TYPE, "BLACKJACK"));

        assertThat(underTest.getPaymentGameType(cookies), is(equalTo("BLACKJACK")));
    }

    @Test
    public void settingThePaymentGameTypeShouldCreateACookie() {
        final ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);

        underTest.setPaymentGameType(response, "BLACKJACK");

        verify(response).addCookie(captor.capture());
        assertThat(captor.getValue().getValue(), is(equalTo("BLACKJACK")));
    }
    
    @Test
    public void screenSourceShouldBeNullIfCookieIsAbsent() {
        assertThat(underTest.getScreenSource(new Cookie[0]), is(nullValue()));
    }

    @Test
    public void screenSourceShouldBeValueOfCookie() {
        final Cookie[] cookies = prepareCookies(new Cookie(SCREEN_SOURCE, "SOURCE"));

        assertThat(underTest.getScreenSource(cookies), is(equalTo("SOURCE")));
    }

    @Test
    public void settingScreenSourceShouldCreateACookie() {
        final ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);

        underTest.setScreenSource(response, "SOURCE");

        verify(response).addCookie(captor.capture());
        assertThat(captor.getValue().getValue(), is(equalTo("SOURCE")));
    }

    @Test
    public void shouldRemoveDataAfterSemiColonForCookieValues(){
        String cookieName = "bob";
        String cookieValue = "something/something;something";
        String expectedCookieValue = "something/something";
        Cookie cookie = underTest.makeCookie(cookieName, cookieValue, true);
        assertEquals(expectedCookieValue, cookie.getValue());
    }

    private Cookie[] prepareCookies(final Cookie... cookies) {
        when(request.getCookies()).thenReturn(cookies);
        return cookies;
    }
}
