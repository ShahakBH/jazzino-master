package com.yazino.yaps;

import org.junit.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.net.Socket;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class AppleSocketFactoryTest {

    private final String mHost = "suabu.local";
    private final int mPort = 2195;
    private final SecurityConfig mSecurityConfig = mock(SecurityConfig.class);
    private final SSLSocketFactory mFactory = mock(SSLSocketFactory.class);

    private final AppleSocketFactory mSocketFactory = new AppleSocketFactory(mHost, mPort, mSecurityConfig) {
        @Override
        SSLSocketFactory factoryFromContext(SSLContext context) {
            return mFactory;
        }
    };

    @Test
    public void shouldReturnSocketThatHasHadHandshakeStarted() throws Exception {
        SSLContext context = mock(SSLContext.class);
        SSLSocket socket = mock(SSLSocket.class);
        when(mFactory.createSocket(anyString(), anyInt())).thenReturn(socket);
        when(mSecurityConfig.getSSLContext()).thenReturn(context);

        Socket actual = mSocketFactory.newSocket();
        verify(mFactory).createSocket(mHost, mPort);
        verify(socket).startHandshake();
        assertEquals(socket, actual);
    }

}
