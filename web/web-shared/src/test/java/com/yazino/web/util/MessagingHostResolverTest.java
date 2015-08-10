package com.yazino.web.util;

import com.yazino.web.util.MessagingHostResolver;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class MessagingHostResolverTest {

    @Test
    public void shouldResolveHostWhenOnlyOneIsDefined() {
        MessagingHostResolver underTest = new MessagingHostResolver("server");
        assertEquals("server", underTest.resolveMessagingHostForPlayer(BigDecimal.ONE));
    }

    @Test
    public void shouldResolveHostWhenMultipleAreDefined() {
        MessagingHostResolver underTest = new MessagingHostResolver("server1,server2, server3");
        assertEquals("server1", underTest.resolveMessagingHostForPlayer(BigDecimal.valueOf(0)));
        assertEquals("server2", underTest.resolveMessagingHostForPlayer(BigDecimal.valueOf(1)));
        assertEquals("server3", underTest.resolveMessagingHostForPlayer(BigDecimal.valueOf(2)));
        assertEquals("server1", underTest.resolveMessagingHostForPlayer(BigDecimal.valueOf(3)));
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotResolveHostWhenNoPlayerIsProvided() {
        MessagingHostResolver underTest = new MessagingHostResolver("server");
        underTest.resolveMessagingHostForPlayer(null);
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotCreateWithNullServer() {
        new MessagingHostResolver(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotCreateWithEmptyServer() {
        new MessagingHostResolver("");
    }
}
