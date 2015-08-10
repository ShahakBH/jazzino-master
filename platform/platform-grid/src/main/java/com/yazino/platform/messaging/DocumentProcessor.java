package com.yazino.platform.messaging;

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
@Polling(gigaSpace = "gigaSpace", concurrentConsumers = 5, maxConcurrentConsumers = 30)
public class DocumentProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(DocumentProcessor.class);
    private static final DocumentWrapper TEMPLATE = new DocumentWrapper();

    private final DocumentDispatcher documentDispatcher;

    @Autowired
    public DocumentProcessor(@Qualifier("documentDispatcher") final DocumentDispatcher documentDispatcher) {
        notNull(documentDispatcher, "Document Dispatcher may not be null");

        this.documentDispatcher = documentDispatcher;
    }

    @EventTemplate
    public DocumentWrapper template() {
        return TEMPLATE;
    }

    @SpaceDataEvent
    public void processDocument(final DocumentWrapper documentWrapper) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Entering DocumentProcessor process: "
                    + ToStringBuilder.reflectionToString(documentWrapper));
        }

        if (documentWrapper == null) {
            return;
        }

        try {
            if (documentWrapper.getRecipients() != null && documentWrapper.getRecipients().size() > 0) {
                documentDispatcher.dispatch(documentWrapper.getDocument(), documentWrapper.getRecipients());
            } else {
                documentDispatcher.dispatch(documentWrapper.getDocument());
            }

        } catch (Throwable t) {
            LOG.error("Exception processing document", t);
        }
    }
}
