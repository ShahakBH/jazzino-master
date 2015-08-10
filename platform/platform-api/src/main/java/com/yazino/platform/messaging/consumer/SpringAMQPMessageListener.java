package com.yazino.platform.messaging.consumer;

import com.yazino.platform.messaging.Message;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpringAMQPMessageListener {
    private static final Logger LOG = LoggerFactory.getLogger(SpringAMQPMessageListener.class);

    private static final int MAX_SUPPORTED_VERSION = 1;
    private final QueueMessageConsumer consumerQueue;

    public SpringAMQPMessageListener(final QueueMessageConsumer<?> consumerQueue) {
        this.consumerQueue = consumerQueue;
    }

    public void handleMessage(final byte[] unconvertedMessage) {
        LOG.error(String.format("Unconverted message received, ignoring [%s]",
                ArrayUtils.toString(unconvertedMessage)));
    }

    public void handleMessage(final Message message) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Received message [%s]", message));
        }

        if (message == null) {
            return;
        }

        if (message.getVersion() > MAX_SUPPORTED_VERSION) {
            LOG.error(String.format("Received unsupported message version [%s] from message [%s]",
                    message.getVersion(), message));
            return;
        }

        if (message.getMessageType() == null) {
            LOG.error(String.format("Received message with null type [%s]", message));
            return;
        }

        consumerQueue.handle(message);

    }
}
