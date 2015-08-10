package com.yazino.engagement.campaign.publishers;

import com.yazino.engagement.PushNotificationMessage;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("androidPushNotificationPublisher")
public class AndroidPushNotificationPublisher extends PushNotificationPublisher {

    @Autowired
    public AndroidPushNotificationPublisher(@Qualifier("androidQueuePublishingService")
                                            final QueuePublishingService<PushNotificationMessage> publishingService) {
        super(publishingService);
    }
}
