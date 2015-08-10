package com.yazino.platform.model.tournament;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@SpaceClass(replicate = false)
public class TournamentPlayerProcessingRequest implements Serializable {
    private static final long serialVersionUID = 7801154829652308737L;

    private String spaceId;
    private BigDecimal tournamentId;
    private BigDecimal playerId;
    private TournamentPlayerProcessingType processingType;
    private Boolean async;

    public TournamentPlayerProcessingRequest() {
    }

    public TournamentPlayerProcessingRequest(final BigDecimal playerId,
                                             final BigDecimal tournamentId,
                                             final TournamentPlayerProcessingType processingType,
                                             final Boolean async) {
        notNull(playerId, "Player ID may not be null");
        notNull(tournamentId, "Tournament ID may not be null");
        notNull(processingType, "Processing Type may not be null");
        notNull(async, "Async flag may not be null");

        this.playerId = playerId;
        this.tournamentId = tournamentId;
        this.processingType = processingType;
        this.async = async;
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

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public TournamentPlayerProcessingType getProcessingType() {
        return processingType;
    }

    public void setProcessingType(final TournamentPlayerProcessingType processingType) {
        this.processingType = processingType;
    }

    public Boolean isAsync() {
        return async;
    }

    public void setAsync(final Boolean async) {
        this.async = async;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(spaceId)
                .append(BigDecimals.strip(tournamentId))
                .append(BigDecimals.strip(playerId))
                .append(processingType)
                .append(async)
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

        final TournamentPlayerProcessingRequest rhs = (TournamentPlayerProcessingRequest) obj;
        return new EqualsBuilder()
                .append(spaceId, rhs.spaceId)
                .append(processingType, rhs.processingType)
                .append(async, rhs.async)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId)
                && BigDecimals.equalByComparison(tournamentId, rhs.tournamentId);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
