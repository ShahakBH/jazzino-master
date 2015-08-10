package com.yazino.bi.opengraph;

import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import com.yazino.platform.opengraph.OpenGraphActionMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Qualifier("openGraphActionMessageConsumer")
public class OpenGraphActionMessageConsumer implements QueueMessageConsumer<OpenGraphActionMessage> {

    private static final Logger LOG = LoggerFactory.getLogger(OpenGraphActionMessageConsumer.class);

    private final OpenGraphManager openGraphManager;

    @Autowired
    public OpenGraphActionMessageConsumer(final OpenGraphManager openGraphManager) {
        LOG.debug("OpenGraphActionMessageConsumer is instantiated.");
        this.openGraphManager = openGraphManager;
    }

    @Override
    public void handle(final OpenGraphActionMessage message) {
        LOG.debug("OpenGraphActionMessageConsumer is invoked: {}", message);
        try {
            this.openGraphManager.publishAction(message.getAction(), message.getPlayerId(), message.getGameType());
        } catch (Exception e) {
            LOG.error("Unable to handle event.", e);
        }
    }

}
