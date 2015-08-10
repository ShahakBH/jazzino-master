package com.yazino.platform.processor.session;

import com.yazino.platform.audit.AuditService;
import com.yazino.platform.model.session.SessionKeyPersistenceRequest;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.apache.commons.lang3.Validate.notNull;

@EventDriven
@Polling(gigaSpace = "gigaSpace", concurrentConsumers = 1, maxConcurrentConsumers = 2)
public class SessionKeyPersistenceProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(SessionKeyPersistenceProcessor.class);

    private static final SessionKeyPersistenceRequest TEMPLATE = new SessionKeyPersistenceRequest();

    private final AuditService auditService;

    protected SessionKeyPersistenceProcessor() {
        // CGLib constructor

        this.auditService = null;
    }

    @Autowired
    public SessionKeyPersistenceProcessor(final AuditService auditService) {
        notNull(auditService, "auditService may not be null");

        this.auditService = auditService;
    }

    @EventTemplate
    public SessionKeyPersistenceRequest getEventTemplate() {
        return TEMPLATE;
    }

    @SpaceDataEvent
    @Transactional
    public void process(final SessionKeyPersistenceRequest request) {
        ensureValidState();

        if (request == null) {
            LOG.warn("Received null request");
            return;
        }

        if (request.getSessionKey() == null) {
            LOG.warn("Received null request payload for request " + request.getSpaceId());
            return;
        }

        try {
            auditService.auditSessionKey(request.getSessionKey());

        } catch (Exception e) {
            LOG.error("Exception saving Session Key: " + request.getSessionKey(), e);
        }
    }

    private void ensureValidState() {
        if (auditService == null) {
            throw new IllegalStateException("Class was created with CGLib constructor");
        }
    }
}
