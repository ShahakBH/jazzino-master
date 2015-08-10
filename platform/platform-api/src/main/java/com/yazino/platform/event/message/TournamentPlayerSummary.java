package com.yazino.platform.event.message;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TournamentPlayerSummary implements Serializable, Comparable<TournamentPlayerSummary> {
    private static final long serialVersionUID = 6459110923678404397L;

    @JsonProperty("id")
    private BigDecimal id;
    @JsonProperty("pos")
    private int leaderboardPosition;
    @JsonProperty("prz")
    private BigDecimal prize;

    private TournamentPlayerSummary() {
    }

    public TournamentPlayerSummary(final BigDecimal id, final int leaderboardPosition, final BigDecimal prize) {
        notNull(id, "ID may not be null");
        notNull(leaderboardPosition, "leaderboardPosition may not be null");
        notNull(prize, "prize may not be null");
        this.id = id;
        this.leaderboardPosition = leaderboardPosition;
        this.prize = prize;
    }

    public BigDecimal getId() {
        return id;
    }

    private void setId(final BigDecimal id) {
        this.id = id;
    }

    public int getLeaderboardPosition() {
        return leaderboardPosition;
    }

    private void setLeaderboardPosition(final int leaderboardPosition) {
        this.leaderboardPosition = leaderboardPosition;
    }

    public BigDecimal getPrize() {
        return prize;
    }

    private void setPrize(final BigDecimal prize) {
        this.prize = prize;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final TournamentPlayerSummary rhs = (TournamentPlayerSummary) obj;
        return new EqualsBuilder()
                .append(leaderboardPosition, rhs.leaderboardPosition)
                .append(id, rhs.id)
                .append(prize, rhs.prize)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(leaderboardPosition)
                .append(id)
                .append(prize)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "TournamentPlayerSummary{"
                + "id=" + id
                + ", leaderboardPosition=" + leaderboardPosition
                + ", prize=" + prize
                + '}';
    }

    @Override
    public int compareTo(final TournamentPlayerSummary o) {
        return new Integer(leaderboardPosition).compareTo(o.leaderboardPosition);
    }
}
