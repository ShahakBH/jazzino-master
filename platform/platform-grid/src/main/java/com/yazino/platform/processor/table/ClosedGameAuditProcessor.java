package com.yazino.platform.processor.table;

import com.yazino.platform.audit.AuditService;
import com.yazino.platform.audit.message.GameAudit;
import com.yazino.platform.model.table.ClosedGameAuditWrapper;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import static org.apache.commons.lang3.Validate.notNull;

@EventDriven
@Polling(gigaSpace = "gigaSpace")
public class ClosedGameAuditProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ClosedGameAuditProcessor.class);

    private static final ClosedGameAuditWrapper TEMPLATE = new ClosedGameAuditWrapper();

    private final AuditService auditService;

    @Autowired
    public ClosedGameAuditProcessor(final AuditService auditService) {
        notNull(auditService, "auditService may not be null");

        this.auditService = auditService;
    }

    @EventTemplate
    public ClosedGameAuditWrapper receivedTemplate() {
        return TEMPLATE;
    }

    @SpaceDataEvent
    public void store(final ClosedGameAuditWrapper auditRequest) {
        if (auditRequest == null) {
            return;
        }

        try {
            auditService.auditGame(asGameAudit(auditRequest));

        } catch (Exception e) {
            LOG.error("error storing audit game: "
                    + ReflectionToStringBuilder.reflectionToString(auditRequest), e);
        }
    }

    private GameAudit asGameAudit(final ClosedGameAuditWrapper auditRequest) {
        return new GameAudit(
                auditRequest.getAuditContext().getLabel(),
                auditRequest.getAuditContext().getHostname(),
                auditRequest.getAuditContext().getAuditDate(),
                auditRequest.getTableId(),
                auditRequest.getGameId(),
                auditRequest.getIncrement(),
                auditRequest.getObservableStatusXml(),
                auditRequest.getInternalStatusXml(),
                auditRequest.getPlayerIds());
    }
}
