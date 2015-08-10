package com.yazino.platform.messaging.host;

import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.messaging.DocumentHeaderType;
import com.yazino.platform.messaging.DocumentType;
import com.yazino.platform.messaging.destination.Destination;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class ShutdownMessageHostDocument implements HostDocument {
    private static final long serialVersionUID = 7201264330228538520L;

    private static final String PATTERN = "{\"refundedAmount\":%s}";

    private final Destination destination;
    private final BigDecimal tableId;
    private final BigDecimal refundedAmount;

    public ShutdownMessageHostDocument(final Destination destination,
                                       final BigDecimal tableId,
                                       final BigDecimal refundedAmount) {
        this.destination = destination;
        this.tableId = tableId;
        this.refundedAmount = refundedAmount;
    }

    @Override
    public void send(final DocumentDispatcher documentDispatcher) {
        final String message = String.format(PATTERN, refundedAmount.toString());
        destination.send(new Document(DocumentType.SHUTDOWN.name(), message, getHeaders(tableId)), documentDispatcher);
    }

    private Map<String, String> getHeaders(final BigDecimal headerTableId) {
        final Map<String, String> headers = new HashMap<String, String>();
        headers.put(DocumentHeaderType.TABLE.getHeader(), headerTableId.toString());
        return headers;
    }
}
