package com.yazino.platform.messaging.destination;

import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentDispatcher;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

public class PlayersDestination implements Destination {
    private static final long serialVersionUID = 4781463487840658391L;

    private static final Logger LOG = LoggerFactory.getLogger(PlayersDestination.class);

    private final Set<BigDecimal> playerIds;

    PlayersDestination(final Set<BigDecimal> playerIds) {
        notEmpty(playerIds, "Player IDs may not be null/empty");

        this.playerIds = playerIds;
    }

    @Override
    public void send(final Document document,
                     final DocumentDispatcher documentDispatcher) {
        notNull(document, "Document may not be null");
        notNull(documentDispatcher, "documentDispatcher may not be null");

        if (LOG.isDebugEnabled()) {
            LOG.debug("Dispatching document to players " + playerIds + ": "
                    + ToStringBuilder.reflectionToString(document));
        }

        documentDispatcher.dispatch(document, playerIds);
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
        final PlayersDestination rhs = (PlayersDestination) obj;
        return new EqualsBuilder()
                .append(playerIds, rhs.playerIds)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(playerIds)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerIds)
                .toString();
    }
}
