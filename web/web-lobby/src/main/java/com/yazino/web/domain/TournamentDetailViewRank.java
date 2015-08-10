package com.yazino.web.domain;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

public class TournamentDetailViewRank implements Serializable {
    private static final long serialVersionUID = -7629222177794964670L;

    private final long rank;
    private final BigDecimal prize;

    public TournamentDetailViewRank(final long rank,
                                    final BigDecimal prize) {
        this.rank = rank;
        this.prize = prize;
    }

    public long getRank() {
        return rank;
    }

    public BigDecimal getPrize() {
        return prize;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        TournamentDetailViewRank rhs = (TournamentDetailViewRank) obj;
        return new EqualsBuilder()
                .append(this.rank, rhs.rank)
                .append(this.prize, rhs.prize)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(rank)
                .append(prize)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("rank", rank)
                .append("prize", prize)
                .toString();
    }
}
