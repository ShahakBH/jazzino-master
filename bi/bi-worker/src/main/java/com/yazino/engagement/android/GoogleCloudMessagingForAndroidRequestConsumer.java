package com.yazino.engagement.android;

import com.yazino.engagement.GoogleCloudMessage;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

@Component("googleCloudMessagingRequestConsumer")
public class GoogleCloudMessagingForAndroidRequestConsumer implements QueueMessageConsumer<GoogleCloudMessage> {

    private static final Logger LOG = LoggerFactory.getLogger(GoogleCloudMessagingForAndroidRequestConsumer.class);

    private GoogleCloudMessagingForAndroidRequestProcessor processor;

    @Autowired
    public GoogleCloudMessagingForAndroidRequestConsumer(final GoogleCloudMessagingForAndroidRequestProcessor processor) {
        checkNotNull(processor);
        this.processor = processor;
    }

    @Override
    public void handle(final GoogleCloudMessage message) {
        try {
            processor.process(message);
        } catch (Exception e) {
            LOG.warn("Failed to send Google Cloud Message {}.", message, e);
        }
    }
}
