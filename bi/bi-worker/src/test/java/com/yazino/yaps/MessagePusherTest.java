package com.yazino.yaps;

import com.yazino.mobile.yaps.message.PushMessage;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.Socket;
import java.net.SocketTimeoutException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class MessagePusherTest {

    private final InputStream mInputStream = mock(InputStream.class);
    private final OutputStream mOutputStream = mock(OutputStream.class);
    private final Socket mSocket = mock(Socket.class);
    private final TargetedMessage mMessage = new TargetedMessage("354f4726a1da594ea216ff0dae37283041696b49b1cfdef66af283271d0c0888", new PushMessage("TEST_GAME", BigDecimal.ONE));
    private final MessagePusher mMessagePusher = new MessagePusher();
    private final AppleConnection mConnection = new AppleConnection("TEST", mSocket);

    @Before
    public void setup() throws Exception {
        when(mSocket.getInputStream()).thenReturn(mInputStream);
        when(mSocket.getOutputStream()).thenReturn(mOutputStream);
    }

    @Test
    public void shouldWriteMessageToOuputStream() throws Exception {
        mMessagePusher.pushAndReadResponse(mConnection, mMessage);
        verify(mOutputStream).write(anyBytes());
        verify(mOutputStream).flush();
    }

    @Test(expected = MessageTransformationException.class)
    public void shouldProprogateExceptionIfOutgoingMessageTransformationFails() throws Exception {
        TargetedMessageTransformer transformer = mock(TargetedMessageTransformer.class);
        when(transformer.toBytes(mMessage)).thenThrow(new MessageTransformationException("TEST"));
        mMessagePusher.setMessageTransformer(transformer);
        mMessagePusher.pushAndReadResponse(mConnection, mMessage);
    }

    @Test
    public void shouldReturnOkResponseShouldTimeoutOccurOnReadingResponse() throws Exception {
        when(mInputStream.read(anyBytes())).thenThrow(new SocketTimeoutException());
        assertEquals(PushResponse.OK, mMessagePusher.pushAndReadResponse(mConnection, mMessage));
        verify(mInputStream, times(1)).read(anyBytes());
    }

    @Test
    public void shouldReturnOkResponseShouldInputStreamEnd() throws Exception {
        when(mInputStream.read(anyBytes())).thenReturn(-1);
        assertEquals(PushResponse.OK, mMessagePusher.pushAndReadResponse(mConnection, mMessage));
        verify(mInputStream, times(1)).read(anyBytes());
    }

    @Test
    public void shouldReturnTransformedResponseWhenResponseAvailble() throws Exception {
        when(mInputStream.read(anyBytes())).thenReturn(8);
        PushResponseTransformer transformer = mock(PushResponseTransformer.class);
        mMessagePusher.setResponseTransformer(transformer);

        PushResponse response = new PushResponse(AppleResponseCode.MissingDeviceToken, BigDecimal.ONE);
        when(transformer.fromBytes(anyBytes())).thenReturn(response);
        assertEquals(response, mMessagePusher.pushAndReadResponse(mConnection, mMessage));
    }

    @Test(expected = IOException.class)
    public void shouldProprogateExceptionIfWriteFails() throws Exception {
        doThrow(new IOException()).when(mOutputStream).write(anyBytes());
        mMessagePusher.pushAndReadResponse(mConnection, mMessage);
    }

    @Test(expected = IOException.class)
    public void shouldThrowIOExceptionIfReadFails() throws Exception {
        when(mInputStream.read(anyBytes())).thenThrow(new IOException());
        mMessagePusher.pushAndReadResponse(mConnection, mMessage);
    }

    @Test
    public void shouldSetSocketTimeoutOnSocketBeforeReading() throws Exception {
        mMessagePusher.setSocketReadTimeout(600);
        mMessagePusher.pushAndReadResponse(mConnection, mMessage);
        verify(mSocket).setSoTimeout(600);
    }

    public static byte[] anyBytes() {
        return any(byte[].class);
    }

}
