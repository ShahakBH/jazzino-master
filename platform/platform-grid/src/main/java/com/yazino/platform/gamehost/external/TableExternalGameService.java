package com.yazino.platform.gamehost.external;

import com.yazino.game.api.ExternalGameService;
import com.yazino.platform.service.audit.AuditLabelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

public class TableExternalGameService implements ExternalGameService {

    private static final Logger LOG = LoggerFactory.getLogger(TableExternalGameService.class);
    private final BigDecimal tableId;
    private final ExternalGameRequestService externalGameRequestService;
    private final List<NovomaticRequest> requests = new LinkedList<>();
    private final AuditLabelFactory auditLabelFactory;

    public TableExternalGameService(BigDecimal tableId,
                                    ExternalGameRequestService externalGameRequestService,
                                    AuditLabelFactory auditLabelFactory) {
        this.tableId = tableId;
        this.externalGameRequestService = externalGameRequestService;
        this.auditLabelFactory = auditLabelFactory;
    }

    @SuppressWarnings("unchecked")
    @Override
    public String makeExternalCall(BigDecimal playerId, String callName, Object context) {
        LOG.debug("Making {} call with context {}", callName, context);
        final String requestId = auditLabelFactory.newLabel();
        requests.add(new NovomaticRequest(requestId, tableId, playerId, callName, context));
        return requestId;
    }

    public void flush() {
        LOG.debug("Flushing {} requests", requests.size());
        externalGameRequestService.postRequests(requests);
    }
}
