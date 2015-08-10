package com.yazino.platform.tournament;

import com.google.common.base.Predicate;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Comparator;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

public class TrophyLeaderboardPlayer implements Serializable {
    private static final long serialVersionUID = 5143986790435879425L;

    public static final Comparator<TrophyLeaderboardPlayer> SORT_BY_POINTS = new PointsComparator();

    private final BigDecimal playerId;
    private final String playerName;
    private final String pictureUrl;
    private long points;
    private int leaderboardPosition;
    private BigDecimal finalPayout;

    public TrophyLeaderboardPlayer(final BigDecimal playerId,
                                   final String playerName,
                                   final String pictureUrl) {
        notNull(playerId, "Player ID may not be null");
        notBlank(playerName, "Player Name may not be null/blank");

        this.playerId = playerId;
        this.playerName = playerName;
        this.pictureUrl = pictureUrl;
    }

    public TrophyLeaderboardPlayer(final int leaderboardPosition,
                                   final BigDecimal playerId,
                                   final String playerName,
                                   final long points,
                                   final String pictureUrl) {
        this(playerId, playerName, pictureUrl);
        this.leaderboardPosition = leaderboardPosition;
        this.points = points;
    }

    public void incrementPoints(final long incrementBy) {
        if (incrementBy < 0) {
            throw new IllegalArgumentException("You may not increment point by a negative number: " + incrementBy);
        }

        points += incrementBy;
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

    public int getLeaderboardPosition() {
        return leaderboardPosition;
    }

    public void setLeaderboardPosition(final int leaderboardPosition) {
        this.leaderboardPosition = leaderboardPosition;
    }

    public String getPictureUrl() {
        return pictureUrl;
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

        final TrophyLeaderboardPlayer rhs = (TrophyLeaderboardPlayer) obj;
        return new EqualsBuilder()
                .append(playerName, rhs.playerName)
                .append(points, rhs.points)
                .append(leaderboardPosition, rhs.leaderboardPosition)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(playerName)
                .append(points)
                .append(leaderboardPosition)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public void setFinalPayout(final BigDecimal finalPayout) {
        this.finalPayout = finalPayout;
    }

    public BigDecimal getFinalPayout() {
        return finalPayout;
    }

    public BigDecimal getFinalPayoutWithDefault(final BigDecimal defaultValue) {
        if (finalPayout == null) {
            return defaultValue;
        } else {
            return finalPayout;
        }
    }

    public static final class PointsComparator
            implements Comparator<TrophyLeaderboardPlayer>, Serializable {
        private static final long serialVersionUID = -7316432030232786878L;

        private PointsComparator() {

        }

        @Override
        public int compare(final TrophyLeaderboardPlayer o1,
                           final TrophyLeaderboardPlayer o2) {
            if (o1 == null && o2 == null) {
                return 0;
            } else if (o1 == null) {
                return -1;
            } else if (o2 == null) {
                return 1;
            }

            if (o1.getPoints() < o2.getPoints()) {
                return 1;
            } else if (o1.getPoints() > o2.getPoints()) {
                return -1;
            }
            return 0;
        }
    }

    public static class MatchPlayerIdPredicate implements Predicate<TrophyLeaderboardPlayer> {
        private final BigDecimal playerId;

        public MatchPlayerIdPredicate(final BigDecimal playerId) {
            notNull(playerId, "Player ID may not be null");

            this.playerId = playerId;
        }

        @Override
        public boolean apply(final TrophyLeaderboardPlayer trophyLeaderboardPlayer) {
            return trophyLeaderboardPlayer != null
                    && trophyLeaderboardPlayer.getPlayerId().compareTo(playerId) == 0;
        }
    }
}
