package com.yazino.platform.messaging.host;

import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.messaging.DocumentHeaderType;
import com.yazino.platform.messaging.DocumentType;
import com.yazino.platform.messaging.destination.Destination;
import com.yazino.platform.model.tournament.TrophyLeaderboardPlayerUpdateResult;
import com.yazino.platform.util.JsonHelper;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import static java.util.Collections.singletonMap;
import static org.apache.commons.lang3.Validate.notNull;

public class TrophyLeaderboardPlayerUpdateDocument implements HostDocument {
    private static final long serialVersionUID = 7271638494428204483L;

    private static final JsonHelper JSON_HELPER = new JsonHelper();

    private final TrophyLeaderboardPlayerUpdateResult updateResult;
    private final Destination destination;

    public TrophyLeaderboardPlayerUpdateDocument(final TrophyLeaderboardPlayerUpdateResult updateResult,
                                                 final Destination destination) {
        notNull(updateResult, "updateResult may not be null");
        notNull(destination, "destination may not be null");

        this.updateResult = updateResult;
        this.destination = destination;
    }

    @Override
    public void send(final DocumentDispatcher documentDispatcher) {
        notNull(documentDispatcher, "documentDispatcher may not be null");

        destination.send(new Document(DocumentType.TROPHY_LEADERBOARD_PLAYER_UPDATE.getName(),
                JSON_HELPER.serialize(updateResult),
                singletonMap(DocumentHeaderType.IS_A_PLAYER.getHeader(), Boolean.toString(true))),
                documentDispatcher);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        TrophyLeaderboardPlayerUpdateDocument rhs = (TrophyLeaderboardPlayerUpdateDocument) obj;
        return new EqualsBuilder()
                .append(this.updateResult, rhs.updateResult)
                .append(this.destination, rhs.destination)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(updateResult)
                .append(destination)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("updateResult", updateResult)
                .append("destination", destination)
                .toString();
    }
}
