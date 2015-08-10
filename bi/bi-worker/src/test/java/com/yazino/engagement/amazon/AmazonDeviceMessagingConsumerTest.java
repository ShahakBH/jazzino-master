package com.yazino.engagement.amazon;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AmazonDeviceMessagingConsumerTest {
    private static final Logger LOG = LoggerFactory.getLogger(AmazonDeviceMessagingConsumerTest.class);
    public static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    AmazonDeviceMessagingConsumer underTest;
    @Mock
    AmazonDeviceMessagingSendingService admSendingService;
    @Mock
    AmazonDeviceMessagingPublisher admPublisher;

    @Before
    public void setUp() throws Exception {
        underTest = new AmazonDeviceMessagingConsumer(admSendingService, admPublisher);
    }

    @Test
    public void handleShouldSendMessage() throws Exception {
        LOG.debug("handling AmazonDevice message");
        AmazonDeviceMessage message = new AmazonDeviceMessage(PLAYER_ID, "registrationId", "title", "ticker", "message", "action", 1l);
        underTest.handle(message);
        verify(admSendingService).sendMessage(message);
    }

    @Test
    public void handleShouldPutMessageBackOntoQueueIfRetry() throws Exception {
        LOG.debug("putting message back on queue");
        AmazonDeviceMessage message = new AmazonDeviceMessage(PLAYER_ID, "registrationId", "title", "ticker", "message", "action", 1l);
        when(admSendingService.sendMessage(message)).thenReturn(AmazonSendStatus.RETRY);

        underTest.handle(message);

        verify(admPublisher).sendPushNotification(message);
    }


}
