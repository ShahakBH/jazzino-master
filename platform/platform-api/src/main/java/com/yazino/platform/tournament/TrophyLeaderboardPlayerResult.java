package com.yazino.platform.tournament;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Comparator;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

public class TrophyLeaderboardPlayerResult implements Serializable {
    private static final long serialVersionUID = 5143980380435879425L;

    public static final Comparator<TrophyLeaderboardPlayerResult> SORT_BY_POSITION = new PositionComparator();

    private final BigDecimal playerId;
    private final String playerName;
    private long points;
    private BigDecimal payout;
    private int position;

    public TrophyLeaderboardPlayerResult(final BigDecimal playerId,
                                         final String playerName,
                                         final long points,
                                         final BigDecimal payout,
                                         final int position) {
        notNull(playerId, "Player ID may not be null");
        notBlank(playerName, "Player Name may not be null/blank");

        this.playerId = playerId;
        this.playerName = playerName;
        this.points = points;
        this.payout = payout;
        this.position = position;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public long getPoints() {
        return points;
    }

    public BigDecimal getPayout() {
        return payout;
    }

    public int getPosition() {
        return position;
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

        final TrophyLeaderboardPlayerResult rhs = (TrophyLeaderboardPlayerResult) obj;
        return new EqualsBuilder()
                .append(playerName, rhs.playerName)
                .append(points, rhs.points)
                .append(payout, rhs.payout)
                .append(position, rhs.position)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(playerName)
                .append(points)
                .append(payout)
                .append(position)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public static final class PositionComparator implements Comparator<TrophyLeaderboardPlayerResult>, Serializable {
        private static final long serialVersionUID = -7316432030232786878L;

        private PositionComparator() {

        }

        @Override
        public int compare(final TrophyLeaderboardPlayerResult o1,
                           final TrophyLeaderboardPlayerResult o2) {
            if (o1 == null && o2 == null) {
                return 0;
            } else if (o1 == null) {
                return -1;
            } else if (o2 == null) {
                return 1;
            }

            return Integer.valueOf(o1.getPosition()).compareTo(o2.getPosition());
        }
    }
}
