package com.yazino.platform.model.tournament;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.UUID;

import static org.apache.commons.lang3.Validate.notNull;

@SpaceClass(replicate = false)
public class TrophyLeaderboardCreationRequest implements Serializable {
    private static final long serialVersionUID = -3659902502037104350L;

    private String spaceId;
    private TrophyLeaderboard trophyLeaderboard;

    public TrophyLeaderboardCreationRequest() {
    }

    public TrophyLeaderboardCreationRequest(final TrophyLeaderboard trophyLeaderboard) {
        notNull(trophyLeaderboard, "Trophy Leaderboard may not be null");

        this.spaceId = UUID.randomUUID().toString();
        this.trophyLeaderboard = trophyLeaderboard;
    }

    @SpaceId
    @SpaceRouting
    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(final String spaceId) {
        this.spaceId = spaceId;
    }

    public TrophyLeaderboard getTrophyLeaderboard() {
        return trophyLeaderboard;
    }

    public void setTrophyLeaderboard(final TrophyLeaderboard trophyLeaderboard) {
        this.trophyLeaderboard = trophyLeaderboard;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(spaceId)
                .append(trophyLeaderboard)
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

        final TrophyLeaderboardCreationRequest rhs = (TrophyLeaderboardCreationRequest) obj;
        return new EqualsBuilder()
                .append(spaceId, rhs.spaceId)
                .append(trophyLeaderboard, rhs.trophyLeaderboard)
                .isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(spaceId)
                .append(trophyLeaderboard)
                .toString();
    }
}
