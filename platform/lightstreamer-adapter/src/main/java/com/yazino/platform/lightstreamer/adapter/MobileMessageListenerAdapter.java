package com.yazino.platform.lightstreamer.adapter;

import com.lightstreamer.interfaces.data.ItemEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.yazino.platform.lightstreamer.adapter.CompressionUtils.UTF_8;
import static com.yazino.platform.lightstreamer.adapter.CompressionUtils.inflate;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * This class listens for amqp messages and writes them to lightstreamer event listener.
 */
public class MobileMessageListenerAdapter implements MessageListener {
    private static final Logger LOG = LoggerFactory.getLogger(MobileMessageListenerAdapter.class);

    private static final String BODY = "body";
    private static final String CONTENT_TYPE = "contentType";

    private final String subject;
    private final ItemEventListener eventListener;

    public MobileMessageListenerAdapter(String subject, ItemEventListener eventListener) {
        notNull(subject, "subject may not be null");
        notNull(eventListener, "eventListener may not be null");

        this.subject = subject;
        this.eventListener = eventListener;
    }

    @Override
    public void onMessage(Message message) {
        final Map<String, String> data = new HashMap<String, String>(2);
        try {
            final String contentType = message.getMessageProperties().getContentType();
            String documentMessage = new String(message.getBody(), UTF_8);

            if (contentIsCompressed(message)) {
                documentMessage = inflate(message.getBody());
            }

            LOG.debug("contentType:{} documentMessage:{}", contentType, documentMessage);

            data.put(BODY, documentMessage);
            data.put(CONTENT_TYPE, contentType);
            eventListener.update(subject, data, false);

        } catch (IOException e) {
            LOG.error("Failed to inflate body", e);
            throw new RuntimeException(e);
        }
    }

    String getSubject() {
        return subject;
    }

    ItemEventListener getEventListener() {
        return eventListener;
    }

    private boolean contentIsCompressed(final Message message) {
        final String contentEncoding = message.getMessageProperties().getContentEncoding();
        return contentEncoding != null && "DEF".equals(contentEncoding);
    }

}
