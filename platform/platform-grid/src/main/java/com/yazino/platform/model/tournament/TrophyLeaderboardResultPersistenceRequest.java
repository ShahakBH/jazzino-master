package com.yazino.platform.model.tournament;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceIndex;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@SpaceClass(replicate = false)
public class TrophyLeaderboardResultPersistenceRequest implements Serializable {
    private static final long serialVersionUID = 4372527505824182635L;

    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_ERROR = "Error";

    private String status = STATUS_PENDING;
    private BigDecimal trophyLeaderboardId;
    private DateTime resultTime;
    private String spaceId;

    public TrophyLeaderboardResultPersistenceRequest() {
    }

    public TrophyLeaderboardResultPersistenceRequest(final String status) {
        notNull(status, "Status may not be null");

        this.status = status;
    }

    public TrophyLeaderboardResultPersistenceRequest(final BigDecimal trophyLeaderboardId,
                                                     final DateTime resultTime) {
        notNull(trophyLeaderboardId, "Trophy Leaderboard ID may not be null");
        notNull(resultTime, "Result Time may not be null");

        this.trophyLeaderboardId = trophyLeaderboardId;
        this.resultTime = resultTime;
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

    public DateTime getResultTime() {
        return resultTime;
    }

    public void setResultTime(final DateTime resultTime) {
        this.resultTime = resultTime;
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

        final TrophyLeaderboardResultPersistenceRequest rhs = (TrophyLeaderboardResultPersistenceRequest) obj;
        return new EqualsBuilder()
                .append(spaceId, rhs.spaceId)
                .append(trophyLeaderboardId, rhs.trophyLeaderboardId)
                .append(resultTime, rhs.resultTime)
                .append(status, rhs.status)
                .isEquals();
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(spaceId)
                .append(trophyLeaderboardId)
                .append(resultTime)
                .append(status)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
