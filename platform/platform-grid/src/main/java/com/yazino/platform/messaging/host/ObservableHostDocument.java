package com.yazino.platform.messaging.host;

import com.yazino.platform.messaging.*;
import com.yazino.platform.messaging.destination.Destination;
import com.yazino.platform.messaging.host.format.HostDocumentBodyFormatter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.HashMap;
import java.util.Map;

import static com.yazino.platform.messaging.host.format.HostDocumentBodyFormatter.body;
import static org.apache.commons.lang3.Validate.notNull;

public abstract class ObservableHostDocument implements HostDocument {
    private static final long serialVersionUID = 267961084859938917L;

    private final DocumentType documentType;
    private final ObservableDocumentContext context;
    private final Destination destination;
    private final boolean playerHeaderRequired;

    public ObservableHostDocument(final DocumentType documentType,
                                  final ObservableDocumentContext context,
                                  final Destination destination,
                                  final boolean playerHeaderRequired) {
        notNull(documentType, "documentType may not be null");
        notNull(context, "observableContext may not be null");
        notNull(destination, "destination may not be null");

        this.documentType = documentType;
        this.context = context;
        this.destination = destination;
        this.playerHeaderRequired = playerHeaderRequired;
    }

    @Override
    public void send(final DocumentDispatcher documentDispatcher) {
        notNull(documentDispatcher, "documentDispatcher may not be null");

        destination.send(new Document(documentType.getName(), buildBody(), buildHeaders()), documentDispatcher);
    }

    private Map<String, String> buildHeaders() {
        final Map<String, String> headers = new HashMap<String, String>();
        if (playerHeaderRequired) {
            headers.put(DocumentHeaderType.IS_A_PLAYER.getHeader(), Boolean.toString(context.isaPlayer()));
        }
        if (context.getTableId() != null) {
            headers.put(DocumentHeaderType.TABLE.getHeader(), context.getTableId().toString());
        }
        return headers;
    }

    private String buildBody() {
        HostDocumentBodyFormatter body = body(context.getGameId(), context.getCommandUUID())
                .withChanges(context)
                .withMessage(context.getMessage())
                .withTableProperties(context.getTableProperties())
                .withPlayerStatus(context.isaPlayer());
        if (context.getStatus() != null) {
            body = body.withWarningCodes(context.getStatus().getWarningCodes());
        }
        return body.build();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final ObservableHostDocument rhs = (ObservableHostDocument) obj;
        return new EqualsBuilder()
                .append(documentType, rhs.documentType)
                .append(context, rhs.context)
                .append(destination, rhs.destination)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(11, 17)
                .append(documentType)
                .append(context)
                .append(destination)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(documentType)
                .append(context)
                .append(destination)
                .toString();
    }

}
