package com.yazino.platform.model.community;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

@SpaceClass(replicate = false)
public class PlayerTrophyPersistenceRequest implements Serializable {
    private static final long serialVersionUID = -8148230809841055161L;

    private BigDecimal playerId;
    private PlayerTrophy playerTrophy;

    //for gs
    public PlayerTrophyPersistenceRequest() {
    }

    public PlayerTrophyPersistenceRequest(final BigDecimal playerId,
                                          final PlayerTrophy playerTrophy) {
        this.playerId = playerId;
        this.playerTrophy = playerTrophy;
    }

    @SpaceId
    @SpaceRouting
    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public PlayerTrophy getPlayerTrophy() {
        return playerTrophy;
    }

    public void setPlayerTrophy(final PlayerTrophy playerTrophy) {
        this.playerTrophy = playerTrophy;
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
        final PlayerTrophyPersistenceRequest rhs = (PlayerTrophyPersistenceRequest) obj;
        return new EqualsBuilder()
                .append(playerTrophy, rhs.playerTrophy)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(playerTrophy)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .append(playerTrophy)
                .toString();
    }
}
