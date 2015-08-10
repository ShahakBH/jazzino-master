package com.yazino.platform.messaging.publisher;

import com.yazino.platform.messaging.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.Validate.notNull;

public class SafeQueuePublishingEventService<T extends Message> implements QueuePublishingService<T> {
    private static final Logger LOG = LoggerFactory.getLogger(SafeQueuePublishingEventService.class);

    private final QueuePublishingService delegate;


    public SafeQueuePublishingEventService(final QueuePublishingService delegate) {
        notNull(delegate, "delegate may not be null");

        this.delegate = delegate;
    }

    @Override
    public void send(final T message) {
        notNull(message, "message may not be null");
        if (LOG.isDebugEnabled()) {
            LOG.debug("Publishing message: " + message);
        }
        try {
            delegate.send(message);
        } catch (Exception e) {
            LOG.error(String.format("Cannot publish message \n\t%s\n Error was:", message), e);

        }
    }
}
