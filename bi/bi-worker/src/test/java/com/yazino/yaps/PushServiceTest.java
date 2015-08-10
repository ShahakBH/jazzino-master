package com.yazino.yaps;

import com.yazino.mobile.yaps.message.PushMessage;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

public class PushServiceTest {

    private final TargetedMessage mMessage = new TargetedMessage("354f4726a1da594ea216ff0dae37283041696b49b1cfdef66af283271d0c0888", new PushMessage("FOO", BigDecimal.ONE));
    private final GenericObjectPool<AppleConnection> mPool = mock(GenericObjectPool.class);
    private final AppleConnection mConnection = mock(AppleConnection.class);
    private final MessagePusher mMessagePusher = mock(MessagePusher.class);
    private final PushService mService = new PushService(mPool);

    @Before
    public void setup() throws Exception {
        mService.setMessagePusher(mMessagePusher);
        when(mPool.borrowObject()).thenReturn(mConnection);
    }

    @Test
    public void shouldShutdownPoolWhenClosed() throws Exception {
        mService.destroy();
        verify(mPool).close();
    }

    @Test
    public void shouldCloseConnectionExplicitlyIfResponseFromAppleIsNotOk() throws Exception {
        when(mMessagePusher.pushAndReadResponse(mConnection, mMessage)).thenReturn(new PushResponse(AppleResponseCode.InvalidPayloadSize, BigDecimal.ONE));
        mService.pushMessage(mMessage);
        verify(mConnection).close();
    }

    @Test
    public void shouldReturnConnectionToPoolWhenExceptionThrownByMessagePusher() throws Exception {
        when(mMessagePusher.pushAndReadResponse(mConnection, mMessage)).thenThrow(new MessageTransformationException("FOO"));
        try {
            mService.pushMessage(mMessage);
        } catch (MessageTransformationException e) {
            // expected
        }
        verify(mPool).returnObject(mConnection);
    }

    @Test
    public void shouldReturnConnectionToPoolWhenOkResponse() throws Exception {
        when(mMessagePusher.pushAndReadResponse(mConnection, mMessage)).thenReturn(PushResponse.OK);
        mService.pushMessage(mMessage);
        verify(mPool).returnObject(mConnection);
    }

    @Test
    public void shouldReturnConnectionToPoolWhenNonOkResponse() throws Exception {
        when(mMessagePusher.pushAndReadResponse(mConnection, mMessage)).thenReturn(new PushResponse(AppleResponseCode.InvalidPayloadSize, BigDecimal.ONE));
        mService.pushMessage(mMessage);
        verify(mPool).returnObject(mConnection);
    }
}
