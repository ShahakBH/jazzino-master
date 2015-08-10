package com.yazino.yaps;

import com.yazino.engagement.mobile.MobileDeviceService;
import com.yazino.platform.Platform;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.net.Socket;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class FeedbackServiceTest {

    private final Socket socket = mock(Socket.class);
    private final InputStream inputStream = mock(InputStream.class);
    private final FeedbackTransformer transformer = mock(FeedbackTransformer.class);
    private final AppleSocketFactory socketFactory = mock(AppleSocketFactory.class);
    private final MobileDeviceService mobileDeviceDao = mock(MobileDeviceService.class);
    private final FeedbackService service = new FeedbackService("FOO", socketFactory, mobileDeviceDao);

    @Before
    public void setup() throws Exception {
        when(socketFactory.newSocket()).thenReturn(socket);
        when(socket.getInputStream()).thenReturn(inputStream);
    }

    @Test(expected = NullPointerException.class)
    public void shouldFailWhenConstructedWithNullFactory() throws Exception {
        new FeedbackService("FOO", null, mobileDeviceDao);
    }

    @Test(expected = NullPointerException.class)
    public void shouldFailWhenConstructedWithNullDAO() throws Exception {
        new FeedbackService("FOO", socketFactory, null);
    }

    @Test
    public void shouldUseDAOToRemoveFeedbackWhenPresent() throws Exception {
        when(inputStream.read(any(byte[].class))).thenReturn(38).thenReturn(-1);
        Feedback expected = new Feedback("1234456", new Date());
        service.setTransformer(transformer);
        when(transformer.fromBytes(any(byte[].class))).thenReturn(expected);

        service.readFeedback();

        verify(mobileDeviceDao).deregisterToken(Platform.IOS, "1234456");
    }

    @Test
    public void shouldCallConsumerWithSomeFeedbackShouldATransformationProblemOccur() throws Exception {
        service.setTransformer(transformer);
        Feedback expected = new Feedback("1234456", new Date());
        when(inputStream.read(any(byte[].class))).thenReturn(38).thenReturn(38).thenReturn(-1);
        when(transformer.fromBytes(any(byte[].class))).thenReturn(expected).thenThrow(new MessageTransformationException("Not enough bytes"));

        service.readFeedback();

        verify(mobileDeviceDao).deregisterToken(Platform.IOS, "1234456");
    }

    @Test
    public void shouldContinueToProcessInputAfterATransformationProblemOccur() throws Exception {
        service.setTransformer(transformer);
        when(inputStream.read(any(byte[].class))).thenReturn(38).thenReturn(38).thenReturn(-1);
        Feedback expected = new Feedback("A", new Date());
        when(transformer.fromBytes(any(byte[].class))).thenThrow(new MessageTransformationException("Not enough bytes")).thenReturn(expected);

        service.readFeedback();

        verify(mobileDeviceDao).deregisterToken(Platform.IOS, "A");
    }

}
