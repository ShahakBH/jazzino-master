package com.yazino.platform.model.tournament;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@SpaceClass(replicate = false)
public class TrophyLeaderboardCreationResponse implements Serializable {
    private static final long serialVersionUID = 5655871640418048097L;

    public enum Status {
        SUCCESS,
        INVALID_DATA,
        FAILURE
    }

    private String spaceId;
    private String requestSpaceId;
    private BigDecimal trophyLeaderboardId;
    private Status status;

    public TrophyLeaderboardCreationResponse() {
    }

    public TrophyLeaderboardCreationResponse(final String requestSpaceId,
                                             final BigDecimal trophyLeaderboardId) {
        notNull(requestSpaceId, "Request Space ID may not be null");
        notNull(trophyLeaderboardId, "Trophy Leaderboard ID may not be null");

        this.requestSpaceId = requestSpaceId;
        this.trophyLeaderboardId = trophyLeaderboardId;
        this.status = Status.SUCCESS;
    }

    public TrophyLeaderboardCreationResponse(final String requestSpaceId,
                                             final Status status) {
        notNull(requestSpaceId, "Request Space ID may not be null");
        notNull(status, "Status may not be null");

        this.requestSpaceId = requestSpaceId;
        this.status = status;
    }

    @SpaceId(autoGenerate = true)
    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(final String spaceId) {
        this.spaceId = spaceId;
    }

    @SpaceRouting
    public String getRequestSpaceId() {
        return requestSpaceId;
    }

    public void setRequestSpaceId(final String requestSpaceId) {
        this.requestSpaceId = requestSpaceId;
    }

    public BigDecimal getTrophyLeaderboardId() {
        return trophyLeaderboardId;
    }

    public void setTrophyLeaderboardId(final BigDecimal trophyLeaderboardId) {
        this.trophyLeaderboardId = trophyLeaderboardId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(spaceId)
                .append(requestSpaceId)
                .append(trophyLeaderboardId)
                .append(status)
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

        final TrophyLeaderboardCreationResponse rhs = (TrophyLeaderboardCreationResponse) obj;
        return new EqualsBuilder()
                .append(spaceId, rhs.spaceId)
                .append(requestSpaceId, rhs.requestSpaceId)
                .append(trophyLeaderboardId, rhs.trophyLeaderboardId)
                .append(status, rhs.status)
                .isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(spaceId)
                .append(requestSpaceId)
                .append(trophyLeaderboardId)
                .append(status)
                .toString();
    }
}
