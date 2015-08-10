package com.yazino.platform.messaging;

import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.messaging.host.HostDocumentWrapper;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import static org.apache.commons.lang3.Validate.notNull;

@EventDriven
@Polling(gigaSpace = "gigaSpace", concurrentConsumers = 5, maxConcurrentConsumers = 40)
public class HostDocumentProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(HostDocumentProcessor.class);
    private static final HostDocumentWrapper TEMPLATE = new HostDocumentWrapper();

    private final DocumentDispatcher documentDispatcher;

    @Autowired(required = true)
    public HostDocumentProcessor(@Qualifier("documentDispatcher") final DocumentDispatcher documentDispatcher) {
        notNull(documentDispatcher, "documentDispatcher may not be null");

        this.documentDispatcher = documentDispatcher;
    }

    @EventTemplate
    public HostDocumentWrapper template() {
        return TEMPLATE;
    }

    @SpaceDataEvent
    public void process(final HostDocumentWrapper request) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Entering process: " + ToStringBuilder.reflectionToString(request));
        }

        if (request == null) {
            return;
        }

        try {
            for (final HostDocument hostDocument : request.getHostDocuments()) {
                hostDocument.send(documentDispatcher);
            }

        } catch (Throwable t) {
            LOG.error("Exception processing request", t);
        }
    }
}
