package com.yazino.engagement.campaign.publishers;

import com.yazino.engagement.PushNotificationMessage;
import com.yazino.platform.messaging.publisher.QueuePublishingService;

public abstract class PushNotificationPublisher {

    private final QueuePublishingService<PushNotificationMessage> publishingService;

    protected PushNotificationPublisher(QueuePublishingService<PushNotificationMessage> publishingService) {
        this.publishingService = publishingService;
    }

    public void sendPushNotification(final PushNotificationMessage message) {
        publishingService.send(message);
    }
}
