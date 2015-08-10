package com.yazino.engagement.amazon;

import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AmazonDeviceMessagingPublisherTest {
    public static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    @Mock
    QueuePublishingService<AmazonDeviceMessage> publishingQueue;
    AmazonDeviceMessagingPublisher underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new AmazonDeviceMessagingPublisher(publishingQueue);
    }

    @Test
    public void sendPushNotificationShouldPutMessageOnQueue(){
        AmazonDeviceMessage message = new AmazonDeviceMessage(PLAYER_ID, "registrationId", "title", "ticker", "message", "action", 1l);
        underTest.sendPushNotification(message);
         verify(publishingQueue).send(message);
    }
}
