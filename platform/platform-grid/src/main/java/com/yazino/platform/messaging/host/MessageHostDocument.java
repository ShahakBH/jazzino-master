package com.yazino.platform.messaging.host;

import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.messaging.DocumentHeaderType;
import com.yazino.platform.messaging.DocumentType;
import com.yazino.platform.messaging.destination.Destination;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.yazino.game.api.ParameterisedMessage;

import java.math.BigDecimal;

import static com.yazino.platform.messaging.host.format.HostDocumentBodyFormatter.body;
import static java.util.Collections.singletonMap;
import static org.apache.commons.lang3.Validate.notNull;

public class MessageHostDocument implements HostDocument {
    private static final long serialVersionUID = -3525771313900581265L;

    private final Destination destination;
    private final ParameterisedMessage message;
    private final long gameId;
    private final String commandUUID;
    private final BigDecimal tableId;

    public MessageHostDocument(final long gameId,
                               final String commandUUID,
                               final BigDecimal tableId,
                               final ParameterisedMessage message,
                               final Destination destination) {
        notNull(tableId, "Table ID may not be null");
        notNull(message, "Message may not be null");
        notNull(destination, "Destination may not be null");

        this.message = message;
        this.gameId = gameId;
        this.commandUUID = commandUUID;
        this.tableId = tableId;
        this.destination = destination;
    }

    @Override
    public void send(final DocumentDispatcher documentDispatcher) {
        notNull(documentDispatcher, "documentDispatcher may not be null");

        final Document document = new Document(DocumentType.MESSAGE.getName(),
                body(gameId, commandUUID).withMessage(message).build(),
                singletonMap(DocumentHeaderType.TABLE.getHeader(), tableId.toPlainString()));
        destination.send(document, documentDispatcher);
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
        final MessageHostDocument rhs = (MessageHostDocument) obj;
        return new EqualsBuilder()
                .append(gameId, rhs.gameId)
                .append(commandUUID, rhs.commandUUID)
                .append(tableId, rhs.tableId)
                .append(message, rhs.message)
                .append(destination, rhs.destination)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(gameId)
                .append(commandUUID)
                .append(tableId)
                .append(message)
                .append(destination)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(gameId)
                .append(commandUUID)
                .append(tableId)
                .append(destination)
                .append(message)
                .toString();
    }
}
