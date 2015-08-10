package com.yazino.platform.model.community;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.platform.community.RelatedPlayer;
import com.yazino.platform.community.RelationshipAction;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@SpaceClass(replicate = false)
public class RelationshipActionRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String spaceId;
    private BigDecimal playerId;
    private Set<RelatedPlayer> relatedPlayers;

    public RelationshipActionRequest() {
        // for gigaspaces
    }

    public RelationshipActionRequest(final BigDecimal playerId,
                                     final Set<RelatedPlayer> relatedPlayers) {
        this.playerId = playerId;
        this.relatedPlayers = relatedPlayers;
    }

    public RelationshipActionRequest(final BigDecimal playerId,
                                     final BigDecimal relatedPlayerId,
                                     final String relatedPlayerName,
                                     final RelationshipAction requestedAction,
                                     final Boolean processingInverseSide) {
        this.playerId = playerId;
        this.relatedPlayers = new HashSet<>();
        relatedPlayers.add(new RelatedPlayer(
                relatedPlayerId, relatedPlayerName, requestedAction, processingInverseSide));
    }

    public Set<RelatedPlayer> getRelatedPlayers() {
        return relatedPlayers;
    }

    public void setRelatedPlayers(final Set<RelatedPlayer> relatedPlayers) {
        this.relatedPlayers = relatedPlayers;
    }

    @SpaceId(autoGenerate = true)
    public String getSpaceId() {
        return spaceId;
    }

    @SpaceRouting
    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setSpaceId(final String spaceId) {
        this.spaceId = spaceId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
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
        final RelationshipActionRequest rhs = (RelationshipActionRequest) obj;
        return new EqualsBuilder()
                .append(relatedPlayers, rhs.relatedPlayers)
                .append(spaceId, rhs.spaceId)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(relatedPlayers)
                .append(spaceId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .append(relatedPlayers)
                .append(spaceId)
                .toString();
    }

}
