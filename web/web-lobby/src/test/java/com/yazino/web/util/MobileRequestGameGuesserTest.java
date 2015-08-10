package com.yazino.web.util;

import com.yazino.platform.Platform;
import org.apache.http.HttpHeaders;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import static com.yazino.web.util.MobileRequestGameGuesser.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MobileRequestGameGuesserTest {

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final MobileRequestGameGuesser guesser = new MobileRequestGameGuesser();

    @Test
    public void shouldReturnNullIfNoUserAgent() throws Exception {
        assertNull(guesser.guessGame(request, Platform.IOS));
    }

    @Test
    public void shouldAlwaysReturnTexasWhenAndroid() throws Exception {
        when(request.getHeader(HttpHeaders.USER_AGENT)).thenReturn("Mozilla/5.0 (Android; U; en-GB) AppleWebKit/533.19.4 (KHTML, like Gecko) AdobeAIR/3.3");
        assertEquals(TEXAS_HOLDEM, guesser.guessGame(request, Platform.ANDROID));
        when(request.getHeader(HttpHeaders.USER_AGENT)).thenReturn("Mozilla/5.0 (Android; Blackjack; U; en-GB) AppleWebKit/533.19.4 (KHTML, like Gecko) AdobeAIR/3.3");
        assertEquals(TEXAS_HOLDEM, guesser.guessGame(request, Platform.ANDROID));
    }

    @Test
    public void shouldAlwaysReturnSlotsWhenAmazon() throws Exception {
        when(request.getHeader(HttpHeaders.USER_AGENT)).thenReturn("Mozilla/5.0 (Android; U; en-GB) AppleWebKit/533.19.4 (KHTML, like Gecko) AdobeAIR/3.3");
        assertEquals(SLOTS, guesser.guessGame(request, Platform.AMAZON));
        when(request.getHeader(HttpHeaders.USER_AGENT)).thenReturn("Mozilla/5.0 (Android; Blackjack; U; en-GB) AppleWebKit/533.19.4 (KHTML, like Gecko) AdobeAIR/3.3");
        assertEquals(SLOTS, guesser.guessGame(request, Platform.AMAZON));
    }

    @Test
    public void shouldReturnSlotsWhenIOSAndWheelDealUserAgent() throws Exception {
        when(request.getHeader(HttpHeaders.USER_AGENT)).thenReturn("Wheel Deal 3.0 rv:3.0-beta1 (iPhone; iPhone OS 5.1.1; en_GB)");
        assertEquals(SLOTS, guesser.guessGame(request, Platform.IOS));
    }

    @Test
    public void shouldReturnHighStakesWhenIOSAndHighStakesUserAgent() throws Exception {
        when(request.getHeader(HttpHeaders.USER_AGENT)).thenReturn("High Stakes 1.0 (beta-2) rv:1.0 (iPhone; iPhone OS 5.1.1; en_GB)");
        assertEquals(HIGH_STAKES, guesser.guessGame(request, Platform.IOS));
    }

    @Test
    public void shouldReturnBlackjackWhenIOSAndBlackjackUserAgent() throws Exception {
        when(request.getHeader(HttpHeaders.USER_AGENT)).thenReturn("Blackjack 2.0 rv:2.0-beta1 (iPhone; iPhone OS 5.1.1; en_GB)");
        assertEquals(BLACKJACK, guesser.guessGame(request, Platform.IOS));
    }

    @Test
    public void shouldReturnNullIfUnsupportedPlatform() throws Exception {
        assertNull(guesser.guessGame(request, Platform.FACEBOOK_CANVAS));
    }

    @Test
    public void shouldReturnNullIfIOSAndUnknownUserAgent() throws Exception {
        when(request.getHeader(HttpHeaders.USER_AGENT)).thenReturn("Fred 2.0 rv:2.0-beta1 (iPhone; iPhone OS 5.1.1; en_GB)");
        assertNull(guesser.guessGame(request, Platform.IOS));
    }

}
