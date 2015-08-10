package com.yazino.platform.messaging.destination;

import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentDispatcher;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Collections;

import static org.apache.commons.lang3.Validate.notNull;

public class PlayerDestination implements Destination {
    private static final long serialVersionUID = 3621375497470954003L;

    private static final Logger LOG = LoggerFactory.getLogger(PlayerDestination.class);

    private final BigDecimal playerId;

    PlayerDestination(final BigDecimal playerId) {
        notNull(playerId, "Player ID may not be null");

        this.playerId = playerId;
    }

    @Override
    public void send(final Document document,
                     final DocumentDispatcher documentDispatcher) {
        notNull(document, "Document may not be null");
        notNull(documentDispatcher, "documentDispatcher may not be null");

        if (LOG.isDebugEnabled()) {
            LOG.debug("Dispatching document to player " + playerId + ": "
                    + ToStringBuilder.reflectionToString(document));
        }

        documentDispatcher.dispatch(document, Collections.singleton(playerId));
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
        final PlayerDestination rhs = (PlayerDestination) obj;
        return new EqualsBuilder()
                .append(playerId, rhs.playerId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(playerId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .toString();
    }
}
