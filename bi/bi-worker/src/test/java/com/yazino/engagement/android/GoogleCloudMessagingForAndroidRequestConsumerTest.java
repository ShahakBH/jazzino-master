package com.yazino.engagement.android;

import com.yazino.engagement.GoogleCloudMessage;
import com.yazino.logging.appender.ListAppender;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class GoogleCloudMessagingForAndroidRequestConsumerTest {

    private static final int APP_REQUEST_ID = 5678;
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(101);
    private static final String DEVICE_TOKEN = "DEVICE_TOKEN";
    private static final Integer APP_REQUEST_TARGET_ID = 400;

    private GoogleCloudMessagingForAndroidRequestProcessor processor = mock(GoogleCloudMessagingForAndroidRequestProcessor.class);
    private GoogleCloudMessagingForAndroidRequestConsumer underTest;

    @Before
    public void setup() throws IOException {
        underTest = new GoogleCloudMessagingForAndroidRequestConsumer(processor);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionIfProcessorIsNull() {
        new GoogleCloudMessagingForAndroidRequestConsumer(null);
    }

    @Test
    public void shouldDelegateProcessingToProcessor() throws IOException {
        GoogleCloudMessage message = new GoogleCloudMessage(APP_REQUEST_ID, APP_REQUEST_TARGET_ID, PLAYER_ID, DEVICE_TOKEN);

        underTest.handle(message);

        verify(processor).process(message);
    }

    @Test
    public void shouldLogButNotPropagateExceptions() throws IOException {
        ListAppender listAppender = ListAppender.addTo(GoogleCloudMessagingForAndroidRequestConsumer.class);
        GoogleCloudMessage message = new GoogleCloudMessage(APP_REQUEST_ID, APP_REQUEST_TARGET_ID, PLAYER_ID, DEVICE_TOKEN);

        doThrow(new RuntimeException("sample exception")).when(processor).process(message);

        underTest.handle(new GoogleCloudMessage(APP_REQUEST_ID, APP_REQUEST_TARGET_ID, PLAYER_ID, DEVICE_TOKEN));

        assertTrue(listAppender.getMessages().contains(String.format("Failed to send Google Cloud Message %s.", message)));
    }

}
