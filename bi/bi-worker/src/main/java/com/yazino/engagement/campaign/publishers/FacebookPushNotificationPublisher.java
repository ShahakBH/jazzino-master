package com.yazino.engagement.campaign.publishers;

import com.yazino.engagement.PushNotificationMessage;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("facebookPushNotificationPublisher")
public class FacebookPushNotificationPublisher extends PushNotificationPublisher {

    @Autowired
    public FacebookPushNotificationPublisher(@Qualifier("facebookQueuePublishingService")
                                             final QueuePublishingService<PushNotificationMessage> publishingService) {
        super(publishingService);
    }
}
