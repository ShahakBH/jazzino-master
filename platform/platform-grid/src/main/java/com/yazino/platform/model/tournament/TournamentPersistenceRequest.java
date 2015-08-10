package com.yazino.platform.model.tournament;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceIndex;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * A request to persist a tournament in the space to the DB.
 */
@SpaceClass
public class TournamentPersistenceRequest implements Serializable {
    private static final long serialVersionUID = 4372527505824182635L;
    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_ERROR = "Error";

    private String status = STATUS_PENDING;
    private BigDecimal tournamentId;
    private String spaceId;

    public TournamentPersistenceRequest() {
    }

    public TournamentPersistenceRequest(final BigDecimal tournamentId) {
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

    @SpaceIndex
    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
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
        final TournamentPersistenceRequest rhs = (TournamentPersistenceRequest) obj;
        return new EqualsBuilder()
                .append(spaceId, rhs.spaceId)
                .append(status, rhs.status)
                .isEquals()
                && BigDecimals.equalByComparison(tournamentId, rhs.tournamentId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(spaceId)
                .append(status)
                .append(BigDecimals.strip(tournamentId))
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(spaceId)
                .append(status)
                .append(tournamentId)
                .toString();
    }
}
