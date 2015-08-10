package com.yazino.engagement.amazon;

import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class AmazonDeviceMessagingPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(AmazonDeviceMessagingPublisher.class);
    private final QueuePublishingService<AmazonDeviceMessage> publishingService;

    @Autowired
    public AmazonDeviceMessagingPublisher(@Qualifier("amazonDeviceMessageQueuePublishingService") final QueuePublishingService<AmazonDeviceMessage> publishingService) {
        this.publishingService = publishingService;
    }

    public void sendPushNotification(final AmazonDeviceMessage message) {
        LOG.debug("putting message on ADM queue");
        publishingService.send(message);
    }
}
