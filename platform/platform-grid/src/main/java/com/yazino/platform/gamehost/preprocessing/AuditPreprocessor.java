package com.yazino.platform.gamehost.preprocessing;

import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.service.audit.Auditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.yazino.game.api.Command;
import com.yazino.game.api.GameRules;
import com.yazino.game.api.ScheduledEvent;

import java.math.BigDecimal;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

public class AuditPreprocessor implements CommandPreprocessor, EventPreprocessor {
    private static final Logger LOG = LoggerFactory.getLogger(AuditPreprocessor.class);

    private final Auditor auditor;

    @Autowired(required = true)
    public AuditPreprocessor(final Auditor auditor) {
        notNull(auditor, "Auditor may not be null");
        this.auditor = auditor;
    }

    public boolean preProcess(final GameRules gameRules,
                              final Command command,
                              final BigDecimal playerBalance,
                              final Table table,
                              final String auditLabel,
                              final List<HostDocument> documentsToSend) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Table {}: using audit label {}", table.getTableId(), auditLabel);
        }
        auditor.audit(auditLabel, command);
        return true;
    }

    public boolean preprocess(final ScheduledEvent event,
                              final Table table) {
        return true;
    }
}
