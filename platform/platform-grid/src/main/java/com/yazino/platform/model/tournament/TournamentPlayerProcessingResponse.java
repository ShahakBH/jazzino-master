package com.yazino.platform.model.tournament;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.platform.tournament.TournamentOperationResult;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@SpaceClass(replicate = false)
public class TournamentPlayerProcessingResponse implements Serializable {
    private static final long serialVersionUID = 548459677789715430L;

    private String spaceId;
    private String requestSpaceId;
    private BigDecimal tournamentId;
    private TournamentOperationResult tournamentOperationResult;

    public TournamentPlayerProcessingResponse() {
    }

    public TournamentPlayerProcessingResponse(final String requestSpaceId,
                                              final BigDecimal tournamentId,
                                              final TournamentOperationResult tournamentOperationResult) {
        notNull(requestSpaceId, "Request Space ID may not be null");
        notNull(tournamentId, "Tournament ID may not be null");
        notNull(tournamentOperationResult, "Operation Result may not be null");

        this.requestSpaceId = requestSpaceId;
        this.tournamentOperationResult = tournamentOperationResult;
        this.tournamentId = tournamentId;
    }

    @SpaceId(autoGenerate = true)
    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(final String spaceId) {
        this.spaceId = spaceId;
    }

    public String getRequestSpaceId() {
        return requestSpaceId;
    }

    public void setRequestSpaceId(final String requestSpaceId) {
        this.requestSpaceId = requestSpaceId;
    }

    public TournamentOperationResult getTournamentOperationResult() {
        return tournamentOperationResult;
    }

    public void setTournamentOperationResult(final TournamentOperationResult tournamentOperationResult) {
        this.tournamentOperationResult = tournamentOperationResult;
    }

    @SpaceRouting
    public BigDecimal getTournamentId() {
        return tournamentId;
    }

    public void setTournamentId(final BigDecimal tournamentId) {
        this.tournamentId = tournamentId;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(spaceId)
                .append(requestSpaceId)
                .append(BigDecimals.strip(tournamentId))
                .append(tournamentOperationResult)
                .toHashCode();
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

        final TournamentPlayerProcessingResponse rhs = (TournamentPlayerProcessingResponse) obj;
        return new EqualsBuilder()
                .append(spaceId, rhs.spaceId)
                .append(requestSpaceId, rhs.requestSpaceId)
                .append(tournamentOperationResult, rhs.tournamentOperationResult)
                .isEquals()
                && BigDecimals.equalByComparison(tournamentId, rhs.tournamentId);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
