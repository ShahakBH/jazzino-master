package com.yazino.platform.model.tournament;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceIndex;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@SpaceClass(replicate = false)
public class TrophyLeaderboardPersistenceRequest implements Serializable {
    private static final long serialVersionUID = 4372527505824182635L;

    public enum Operation {
        SAVE,
        ARCHIVE
    }

    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_ERROR = "Error";

    private String status = STATUS_PENDING;
    private BigDecimal trophyLeaderboardId;
    private String spaceId;
    private Operation operation;

    public TrophyLeaderboardPersistenceRequest() {
    }

    public TrophyLeaderboardPersistenceRequest(final BigDecimal trophyLeaderboardId) {
        notNull(trophyLeaderboardId, "Trophy Leaderboard ID may not be null");

        this.trophyLeaderboardId = trophyLeaderboardId;
        this.operation = Operation.SAVE;
    }

    public TrophyLeaderboardPersistenceRequest(final BigDecimal trophyLeaderboardId,
                                               final Operation operation) {
        notNull(trophyLeaderboardId, "Trophy Leaderboard ID may not be null");
        notNull(operation, "Operation may not be null");

        this.trophyLeaderboardId = trophyLeaderboardId;
        this.operation = operation;
    }

    @SpaceId(autoGenerate = true)
    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(final String spaceId) {
        this.spaceId = spaceId;
    }

    @SpaceRouting
    public BigDecimal getTrophyLeaderboardId() {
        return trophyLeaderboardId;
    }

    public void setTrophyLeaderboardId(final BigDecimal trophyLeaderboardId) {
        this.trophyLeaderboardId = trophyLeaderboardId;
    }

    @SpaceIndex
    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(final Operation operation) {
        this.operation = operation;
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

        final TrophyLeaderboardPersistenceRequest rhs = (TrophyLeaderboardPersistenceRequest) obj;
        return new EqualsBuilder()
                .append(spaceId, rhs.spaceId)
                .append(trophyLeaderboardId, rhs.trophyLeaderboardId)
                .append(status, rhs.status)
                .append(operation, rhs.operation)
                .isEquals();
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(spaceId)
                .append(trophyLeaderboardId)
                .append(status)
                .append(operation)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
