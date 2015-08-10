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
public class TrophyLeaderboardResultingRequest implements Serializable {
    private static final long serialVersionUID = -3659902502037104350L;

    private String spaceId;
    private BigDecimal trophyLeaderboardId;

    public TrophyLeaderboardResultingRequest() {
    }

    public TrophyLeaderboardResultingRequest(final BigDecimal trophyLeaderboardId) {
        notNull(trophyLeaderboardId, "Trophy Leaderboard ID may not be null");

        this.trophyLeaderboardId = trophyLeaderboardId;
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

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(spaceId)
                .append(trophyLeaderboardId)
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

        final TrophyLeaderboardResultingRequest rhs = (TrophyLeaderboardResultingRequest) obj;
        return new EqualsBuilder()
                .append(spaceId, rhs.spaceId)
                .append(trophyLeaderboardId, rhs.trophyLeaderboardId)
                .isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(spaceId)
                .append(trophyLeaderboardId)
                .toString();
    }
}
