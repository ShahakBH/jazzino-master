package com.yazino.platform.community;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

public class RelationshipRequest implements Serializable {
    private static final long serialVersionUID = -4832351441247794680L;

    private final Set<RelatedPlayer> relatedPlayers = new HashSet<>();
    private final BigDecimal playerId;

    public RelationshipRequest(final BigDecimal playerId,
                               final BigDecimal relatedPlayerId,
                               final String relatedPlayerName,
                               final RelationshipAction requestedAction,
                               final Boolean processingInverseSide) {
        this.playerId = playerId;
        relatedPlayers.add(new RelatedPlayer(
                relatedPlayerId, relatedPlayerName, requestedAction, processingInverseSide));
    }

    public Set<RelatedPlayer> getRelatedPlayers() {
        return relatedPlayers;
    }

    public BigDecimal getPlayerId() {
        return playerId;
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
        final RelationshipRequest rhs = (RelationshipRequest) obj;
        return new EqualsBuilder()
                .append(relatedPlayers, rhs.relatedPlayers)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(relatedPlayers)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .append(relatedPlayers)
                .toString();
    }
}
