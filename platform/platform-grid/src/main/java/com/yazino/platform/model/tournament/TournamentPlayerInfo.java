package com.yazino.platform.model.tournament;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceIndex;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * An object to allow quick queries on tournament memebership.
 */
@SpaceClass
public class TournamentPlayerInfo implements Serializable {
    private static final long serialVersionUID = 2333653541513511581L;

    private BigDecimal playerId;
    private BigDecimal tournamentId;
    private TournamentPlayerStatus status;

    public TournamentPlayerInfo() {
    }

    public TournamentPlayerInfo(final BigDecimal playerId,
                                final BigDecimal tournamentId,
                                final TournamentPlayerStatus status) {
        notNull(playerId, "Player ID may not be null");
        notNull(tournamentId, "Tournament ID may not be null");
        notNull(status, "Status may not be null");

        this.playerId = playerId;
        this.tournamentId = tournamentId;
        this.status = status;
    }

    @SpaceIndex
    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    @SpaceRouting
    public BigDecimal getTournamentId() {
        return tournamentId;
    }

    public void setTournamentId(final BigDecimal tournamentId) {
        this.tournamentId = tournamentId;
    }

    public TournamentPlayerStatus getStatus() {
        return status;
    }

    public void setStatus(final TournamentPlayerStatus status) {
        this.status = status;
    }

    @SpaceId
    public String getSpaceId() {
        if (tournamentId == null || playerId == null) {
            return null;
        }
        return String.valueOf(tournamentId) + "|" + String.valueOf(playerId);
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setSpaceId(final String spaceId) {
        // require for GigaSpace indexing
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj.getClass() != getClass()) {
            return false;
        }

        final TournamentPlayerInfo rhs = (TournamentPlayerInfo) obj;
        return BigDecimals.equalByComparison(playerId, rhs.playerId)
                && BigDecimals.equalByComparison(tournamentId, rhs.tournamentId);
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(BigDecimals.strip(tournamentId))
                        // status intentionally omitted
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
