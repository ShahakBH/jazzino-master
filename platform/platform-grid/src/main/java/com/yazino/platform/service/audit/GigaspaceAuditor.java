package com.yazino.platform.service.audit;

import com.yazino.platform.model.table.AuditContext;
import com.yazino.platform.model.table.ClosedGameAuditWrapper;
import com.yazino.platform.model.table.CommandAuditWrapper;
import com.yazino.platform.model.table.Table;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.space.mode.PrePrimary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.yazino.game.api.Command;
import com.yazino.game.api.GameRules;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.UUID;

@Service
public class GigaspaceAuditor implements Auditor {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceAuditor.class);

    private boolean enabled = true;
    private GigaSpace gigaSpace;

    private String hostname;

    /*
      * required for cgi-lib
      */
    @Autowired
    public void setGigaSpace(final GigaSpace gigaSpace) {
        this.gigaSpace = gigaSpace;
    }

    private AuditContext buildContext(final String label) {
        return new AuditContext(label, new Date(), getHostname());
    }

    public void audit(final String label, final Command c) {
        writeToSpace(new CommandAuditWrapper(c, buildContext(label)));
    }

    public void audit(final String label, final Table table, final GameRules gameRules) {
        LOG.debug("Audit closed game {}", label);

        if (enabled && table.getCurrentGame() != null) {
            final ClosedGameAuditWrapper closedGameAuditWrapper = new ClosedGameAuditWrapper(
                    table.getTableId(), table.getGameId(),
                    table.incrementDefaultedToOne(),
                    gameRules.toAuditString(table.getCurrentGame()),
                    null,
                    table.playerIds(gameRules),
                    buildContext(label)
            );
            if (LOG.isDebugEnabled()) {
                LOG.debug("Writing ClosedGameAuditWrapper into space {}", ReflectionToStringBuilder.reflectionToString(closedGameAuditWrapper));
            }
            writeToSpace(closedGameAuditWrapper);
        }
    }

    public String newLabel() {
        return UUID.randomUUID().toString();
    }

    private void writeToSpace(final Object obj) {
        if (enabled) {
            gigaSpace.write(obj);
        }
    }

    String getHostname() {
        if (hostname == null) {
            updateHostname();
        }
        return hostname;
    }

    @PrePrimary
    public void updateHostname() {
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            final String systemHostname = System.getenv("HOSTNAME");
            if (systemHostname != null) {
                hostname = systemHostname;
            } else {
                hostname = "Unknown";
            }
        }
    }

    @Value("${strata.auditing.enabled}")
    public void setEnabled(final boolean enabled) {
        if (enabled) {
            LOG.info("Auditing is now enabled");
        } else {
            LOG.info("Auditing is now disabled");
        }

        this.enabled = enabled;
    }
}
