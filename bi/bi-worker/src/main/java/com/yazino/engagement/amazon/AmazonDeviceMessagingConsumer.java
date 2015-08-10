package com.yazino.engagement.amazon;

import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AmazonDeviceMessagingConsumer implements QueueMessageConsumer<AmazonDeviceMessage> {
    private static final Logger LOG = LoggerFactory.getLogger(AmazonDeviceMessagingConsumer.class);
    private final AmazonDeviceMessagingSendingService admSendingService;
    private final AmazonDeviceMessagingPublisher admPublisher;

    @Autowired
    public AmazonDeviceMessagingConsumer(final AmazonDeviceMessagingSendingService admSendingService, final AmazonDeviceMessagingPublisher admPublisher) {
        Validate.notNull(admSendingService, "Amazon Sending Service can not be null");
        Validate.notNull(admPublisher, "Amazon Publisher can not be null");
        this.admSendingService = admSendingService;
        this.admPublisher = admPublisher;
    }

    @Override
    public void handle(final AmazonDeviceMessage message) {
        LOG.debug("handling ADM message");
        try {
            AmazonSendStatus amazonSendStatus = admSendingService.sendMessage(message);

            if (amazonSendStatus.equals(AmazonSendStatus.RETRY)) {
                admPublisher.sendPushNotification(message);
            }
        } catch (Exception e) {
            LOG.error("unknown problem with sending amazon message: {}", e);
        }
    }

}
