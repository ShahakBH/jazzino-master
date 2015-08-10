package com.yazino.platform.service.audit;

import com.yazino.platform.model.table.Table;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yazino.game.api.Command;
import com.yazino.game.api.GameRules;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Auditor implementation that uses the commons logging mechanism
 */
public class CommonsLoggingAuditor implements Auditor {
    private static AtomicLong label = new AtomicLong();
    private static final Logger LOG = LoggerFactory.getLogger(CommonsLoggingAuditor.class);

    public void audit(final String auditLabel,
                      final Command c) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(auditLabel + "\t" + ToStringBuilder.reflectionToString(c));
        }
    }

    public void audit(final String auditLabel,
                      final Table t, final GameRules gameRules) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(auditLabel + "\t" + ToStringBuilder.reflectionToString(t));
        }
    }

    public String newLabel() {
        return String.valueOf(label.incrementAndGet());
    }

}
