package com.yazino.platform.tournament;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Comparator;

import static org.apache.commons.lang3.Validate.notNull;

public final class TournamentVariationPayout implements Serializable {

    private static final long serialVersionUID = 7368332422027797246L;

    private final int rank;
    private final BigDecimal payout;

    public TournamentVariationPayout(final int rank,
                                     final BigDecimal payout) {
        notNull(payout, "Payout must not be NULL.");
        this.rank = rank;
        this.payout = payout;
    }

    public int getRank() {
        return rank;
    }

    public BigDecimal getPayout() {
        return payout;
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
        final TournamentVariationPayout rhs = (TournamentVariationPayout) obj;
        return new EqualsBuilder()
                .append(rank, rhs.rank)
                .append(payout, rhs.payout)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(rank)
                .append(payout)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public static class RankComparator implements Comparator<TournamentVariationPayout> {

        @Override
        public int compare(final TournamentVariationPayout instance1,
                           final TournamentVariationPayout instance2) {
            return new Integer(instance1.getRank()).compareTo(instance2.getRank());
        }
    }

}
