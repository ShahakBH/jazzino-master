package com.yazino.platform.model.tournament;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

public class TrophyLeaderboardPlayerUpdateResult implements Serializable {
    private static final long serialVersionUID = 5546033757451162036L;

    private final BigDecimal trophyLeaderboardId;
    private final BigDecimal tournamentId;
    private final int previousPosition;
    private final int newPosition;
    private final long previousPoints;
    private final long tournamentPoints;
    private final long bonusPoints;

    public TrophyLeaderboardPlayerUpdateResult(final BigDecimal trophyLeaderboardId,
                                               final BigDecimal tournamentId,
                                               final int previousPosition,
                                               final int newPosition,
                                               final long previousPoints,
                                               final long tournamentPoints,
                                               final long bonusPoints) {
        notNull(trophyLeaderboardId, "trophyLeaderboardId may not be null");

        this.trophyLeaderboardId = trophyLeaderboardId;
        this.tournamentId = tournamentId;
        this.previousPosition = previousPosition;
        this.newPosition = newPosition;
        this.previousPoints = previousPoints;
        this.tournamentPoints = tournamentPoints;
        this.bonusPoints = bonusPoints;
    }

    public BigDecimal getTrophyLeaderboardId() {
        return trophyLeaderboardId;
    }

    public BigDecimal getTournamentId() {
        return tournamentId;
    }

    public int getPreviousPosition() {
        return previousPosition;
    }

    public int getNewPosition() {
        return newPosition;
    }

    public long getPreviousPoints() {
        return previousPoints;
    }

    public long getTournamentPoints() {
        return tournamentPoints;
    }

    public long getBonusPoints() {
        return bonusPoints;
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
        TrophyLeaderboardPlayerUpdateResult rhs = (TrophyLeaderboardPlayerUpdateResult) obj;
        return new EqualsBuilder()
                .append(this.trophyLeaderboardId, rhs.trophyLeaderboardId)
                .append(this.tournamentId, rhs.tournamentId)
                .append(this.previousPosition, rhs.previousPosition)
                .append(this.newPosition, rhs.newPosition)
                .append(this.previousPoints, rhs.previousPoints)
                .append(this.tournamentPoints, rhs.tournamentPoints)
                .append(this.bonusPoints, rhs.bonusPoints)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(trophyLeaderboardId)
                .append(tournamentId)
                .append(previousPosition)
                .append(newPosition)
                .append(previousPoints)
                .append(tournamentPoints)
                .append(bonusPoints)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("trophyLeaderboardId", trophyLeaderboardId)
                .append("tournamentId", tournamentId)
                .append("previousPosition", previousPosition)
                .append("newPosition", newPosition)
                .append("previousPoints", previousPoints)
                .append("tournamentPoints", tournamentPoints)
                .append("bonusPoints", bonusPoints)
                .toString();
    }
}
