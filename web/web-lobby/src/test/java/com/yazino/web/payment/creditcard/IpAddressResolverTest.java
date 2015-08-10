package com.yazino.web.payment.creditcard;

import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IpAddressResolverTest {
    @Test
    public void shouldResolveIpAddress() throws UnknownHostException {
        final HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRemoteAddr()).thenReturn("127.0.0.1");
        assertEquals(InetAddress.getByName("127.0.0.1"), IpAddressResolver.resolveFor(req));
    }

    @Test
    public void shouldIgnoreInvalidIpAddress() {
        final HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRemoteAddr()).thenReturn("crazyness");
        assertNull(IpAddressResolver.resolveFor(req));
    }
}
