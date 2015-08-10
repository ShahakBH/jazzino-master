package com.yazino.platform.model.tournament;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Request the update of the leaderboard for a given tournament.
 */
@SpaceClass(replicate = false)
public class TournamentLeaderboardUpdateRequest implements Serializable {
    private static final long serialVersionUID = 5967762893404075071L;

    private String spaceId;

    private BigDecimal tournamentId;

    public TournamentLeaderboardUpdateRequest() {
    }

    public TournamentLeaderboardUpdateRequest(final BigDecimal tournamentId) {
        notNull(tournamentId, "Tournament ID may not be null");

        this.tournamentId = tournamentId;
    }

    @SpaceId(autoGenerate = true)
    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(final String spaceId) {
        this.spaceId = spaceId;
    }

    @SpaceRouting
    public BigDecimal getTournamentId() {
        return tournamentId;
    }

    public void setTournamentId(final BigDecimal tournamentId) {
        this.tournamentId = tournamentId;
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

        final TournamentLeaderboardUpdateRequest rhs = (TournamentLeaderboardUpdateRequest) obj;
        return BigDecimals.equalByComparison(tournamentId, rhs.tournamentId);
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(tournamentId))
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
