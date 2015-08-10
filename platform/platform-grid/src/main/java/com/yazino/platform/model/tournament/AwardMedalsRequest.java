package com.yazino.platform.model.tournament;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.math.BigDecimal;

@SpaceClass(replicate = false)
public class AwardMedalsRequest {
    private String id;
    private BigDecimal tournamentId;
    private String gameType;

    //GS
    public AwardMedalsRequest() {
    }

    public AwardMedalsRequest(final BigDecimal tournamentId,
                              final String gameType) {
        this.tournamentId = tournamentId;
        this.gameType = gameType;
    }

    @SpaceId(autoGenerate = true)
    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public BigDecimal getTournamentId() {
        return tournamentId;
    }

    public void setTournamentId(final BigDecimal tournamentId) {
        this.tournamentId = tournamentId;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(final String gameType) {
        this.gameType = gameType;
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
        final AwardMedalsRequest rhs = (AwardMedalsRequest) obj;
        return new EqualsBuilder()
                .append(id, rhs.id)
                .append(gameType, rhs.gameType)
                .isEquals()
                && BigDecimals.equalByComparison(tournamentId, rhs.tournamentId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(id)
                .append(BigDecimals.strip(tournamentId))
                .append(gameType)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(id)
                .append(tournamentId)
                .append(gameType)
                .toString();
    }
}
