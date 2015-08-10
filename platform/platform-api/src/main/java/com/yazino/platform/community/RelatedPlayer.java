package com.yazino.platform.community;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

public class RelatedPlayer implements Serializable {
    private static final long serialVersionUID = 7361761462275854774L;

    private final BigDecimal playerId;
    private final String playerName;
    private final RelationshipAction requestedAction;
    private final boolean processingInverseSide;

    public RelatedPlayer(final BigDecimal playerId,
                         final String playerName,
                         final RelationshipAction requestedAction,
                         final boolean processingInverseSide) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.requestedAction = requestedAction;
        this.processingInverseSide = processingInverseSide;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public boolean isProcessingInverseSide() {
        return processingInverseSide;
    }

    public RelationshipAction getRequestedAction() {
        return requestedAction;
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
        final RelatedPlayer rhs = (RelatedPlayer) obj;
        return new EqualsBuilder()
                .append(playerName, rhs.playerName)
                .append(processingInverseSide, rhs.processingInverseSide)
                .append(requestedAction, rhs.requestedAction)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(playerName)
                .append(processingInverseSide)
                .append(requestedAction)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .append(playerName)
                .append(processingInverseSide)
                .append(requestedAction)
                .toString();
    }
}
