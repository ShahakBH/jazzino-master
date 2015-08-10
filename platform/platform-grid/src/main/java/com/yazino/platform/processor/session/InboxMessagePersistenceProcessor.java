package com.yazino.platform.processor.session;

import com.yazino.platform.model.session.InboxMessage;
import com.yazino.platform.model.session.InboxMessagePersistenceRequest;
import com.yazino.platform.persistence.session.InboxMessageDAO;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.TransactionalEvent;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.apache.commons.lang3.Validate.notNull;

@EventDriven
@Polling(gigaSpace = "gigaSpace")
@TransactionalEvent(transactionManager = "spaceTransactionManager")
public class InboxMessagePersistenceProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(InboxMessagePersistenceProcessor.class);
    private static final InboxMessagePersistenceRequest TEMPLATE = new InboxMessagePersistenceRequest();
    private InboxMessageDAO inboxMessageDAO;

    //Needed for CGLib
    InboxMessagePersistenceProcessor() {
        inboxMessageDAO = null;
    }

    @Autowired
    public InboxMessagePersistenceProcessor(final InboxMessageDAO inboxMessageDAO) {
        notNull(inboxMessageDAO, "inboxMessageDAO is null");
        this.inboxMessageDAO = inboxMessageDAO;
    }

    @EventTemplate
    public InboxMessagePersistenceRequest template() {
        return TEMPLATE;
    }

    @SpaceDataEvent
    @Transactional
    public InboxMessagePersistenceRequest processRequest(final InboxMessagePersistenceRequest request) {
        checkInitialisation();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Entering processRequest " + ReflectionToStringBuilder.reflectionToString(request));
        }
        final InboxMessage message = request.getMessage();
        if (message == null) {
            LOG.warn("Empty request! Ignoring...");
            return null;
        }
        InboxMessagePersistenceRequest afterProcessing;
        try {
            inboxMessageDAO.save(message);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Saved " + ReflectionToStringBuilder.reflectionToString(request));
            }
            return null;
        } catch (Throwable t) {
            request.setStatus(InboxMessagePersistenceRequest.STATUS_ERROR);
            afterProcessing = request;
            LOG.error("Exception saving inbox message see stack trace: ", t);
        }
        return afterProcessing;
    }

    private void checkInitialisation() {
        if (inboxMessageDAO == null) {
            throw new IllegalStateException("inboxMessageDAO was not initialized by spring container");
        }
    }
}
