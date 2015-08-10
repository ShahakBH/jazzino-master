package com.yazino.engagement.campaign.publishers;

import com.yazino.engagement.ChannelType;
import com.yazino.engagement.PlayerTarget;
import com.yazino.engagement.PushNotificationMessage;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class IosPushNotificationPublisherTest {

    @Mock
    private QueuePublishingService<PushNotificationMessage> publishingService;
    private PushNotificationPublisher underTest;
    private final BigDecimal playerId = BigDecimal.TEN;

    private final Map<String, String> contentMap = newHashMap();
    private final String message = "Oh!!! you got the best score ever";


    @Before
    public void setUp() throws Exception {
        contentMap.put("message", message);
        underTest = new IosPushNotificationPublisher(publishingService);

    }

    @Test
    public void sendPushNotificationShouldCallSenderToPutTheMessageInQueue() {

        PlayerTarget playerTarget = new PlayerTarget("Slots", "", playerId, "", "bundle", null);
        PushNotificationMessage iosPushNotificationMessage = new PushNotificationMessage(playerTarget, contentMap, ChannelType.IOS, 9876l);

        underTest.sendPushNotification(iosPushNotificationMessage);

        verify(publishingService).send(iosPushNotificationMessage);
    }
}
