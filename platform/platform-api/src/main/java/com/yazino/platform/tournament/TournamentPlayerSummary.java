package com.yazino.platform.tournament;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

public class TournamentPlayerSummary implements Serializable, Comparable<TournamentPlayerSummary> {
    private static final long serialVersionUID = 7529824187641761196L;

    private final BigDecimal id;
    private final int leaderboardPosition;
    private final String name;
    private final BigDecimal prize;
    private final String pictureUrl;

    public TournamentPlayerSummary(final BigDecimal id,
                                   final int leaderboardPosition,
                                   final String name,
                                   final BigDecimal prize,
                                   final String pictureUrl) {
        notNull(id, "ID may not be null");
        notNull(name, "Name may not be null");
        notNull(prize, "Prize may not be null");

        this.id = id;
        this.leaderboardPosition = leaderboardPosition;
        this.name = name;
        this.prize = prize;
        this.pictureUrl = pictureUrl;
    }

    public BigDecimal getId() {
        return id;
    }

    public int getLeaderboardPosition() {
        return leaderboardPosition;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrize() {
        return prize;
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

        final TournamentPlayerSummary rhs = (TournamentPlayerSummary) obj;
        return new EqualsBuilder()
                .append(id, rhs.id)
                .append(leaderboardPosition, rhs.leaderboardPosition)
                .append(name, rhs.name)
                .append(prize, rhs.prize)
                .append(pictureUrl, rhs.pictureUrl)
                .isEquals()
                && BigDecimals.equalByComparison(id, rhs.id);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(id))
                .append(leaderboardPosition)
                .append(name)
                .append(prize)
                .append(pictureUrl)
                .toHashCode();
    }

    @Override
    public String toString() {
        final StringBuilder out = new StringBuilder();

        out.append(id).append(":");
        out.append(leaderboardPosition).append(":");
        out.append(name).append(":");
        out.append(prize).append(":");
        out.append(pictureUrl);

        return out.toString();
    }

    @Override
    public int compareTo(final TournamentPlayerSummary o) {
        final int compare = new Integer(leaderboardPosition).compareTo(o.leaderboardPosition);
        if (compare == 0) {
            return ObjectUtils.compare(id, o.id);
        }
        return compare;
    }
}
