package com.yazino.platform.messaging.host;

import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.messaging.DocumentHeaderType;
import com.yazino.platform.messaging.DocumentType;
import com.yazino.platform.messaging.destination.Destination;
import com.yazino.platform.util.JsonHelper;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.yazino.game.api.AwardMedalEvent;

import java.math.BigDecimal;

import static java.util.Collections.singletonMap;
import static org.apache.commons.lang3.Validate.notNull;

public class AwardMedalHostDocument implements HostDocument {
    private static final long serialVersionUID = 7271638495429204483L;

    private static final JsonHelper JSON_HELPER = new JsonHelper();

    private final BigDecimal awardId;
    private final String gameType;
    private final Destination destination;

    public AwardMedalHostDocument(final BigDecimal awardId,
                                  final String gameType,
                                  final Destination destination) {
        notNull(awardId, "awardId may not be null");
        notNull(gameType, "gameType may not be null");
        notNull(destination, "destination may not be null");

        this.awardId = awardId;
        this.gameType = gameType;
        this.destination = destination;
    }

    @Override
    public void send(final DocumentDispatcher documentDispatcher) {
        notNull(documentDispatcher, "documentDispatcher may not be null");

        destination.send(new Document(DocumentType.AWARD_MEDAL_EVENT.getName(),
                JSON_HELPER.serialize(new AwardMedalEvent(awardId, gameType)),
                singletonMap(DocumentHeaderType.IS_A_PLAYER.getHeader(), Boolean.toString(true))),
                documentDispatcher);
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
        final AwardMedalHostDocument rhs = (AwardMedalHostDocument) obj;
        return new EqualsBuilder()
                .append(awardId, rhs.awardId)
                .append(gameType, rhs.gameType)
                .append(destination, rhs.destination)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(awardId)
                .append(gameType)
                .append(destination)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(awardId)
                .append(gameType)
                .append(destination)
                .toString();
    }

}
