package com.yazino.platform.tournament;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

public class Schedule implements Serializable {
    private static final long serialVersionUID = 864977029532142817L;

    private final SortedSet<TournamentRegistrationInfo> sortedTournaments = new TreeSet<>();
    private final Set<BigDecimal> inProgress = new HashSet<>();

    public Schedule(final SortedSet<TournamentRegistrationInfo> sortedTournaments,
                    final Set<BigDecimal> inProgress) {
        if (sortedTournaments != null) {
            this.sortedTournaments.addAll(sortedTournaments);
        }

        if (inProgress != null) {
            this.inProgress.addAll(inProgress);
        }
    }

    public Set<TournamentRegistrationInfo> getChronologicalTournamentsForPlayer(final BigDecimal playerId) {
        if (playerId == null) {
            return Collections.emptySet();
        }

        return Sets.filter(sortedTournaments, new Predicate<TournamentRegistrationInfo>() {
            @Override
            public boolean apply(final TournamentRegistrationInfo tournamentRegistrationInfo) {
                return tournamentRegistrationInfo != null
                        && (!inProgress.contains(tournamentRegistrationInfo.getTournamentId()) || tournamentRegistrationInfo.isRegistered(playerId));
            }
        });
    }

    public Set<TournamentRegistrationInfo> getChronologicalTournaments() {
        return Sets.filter(sortedTournaments, new Predicate<TournamentRegistrationInfo>() {
            @Override
            public boolean apply(final TournamentRegistrationInfo tournamentRegistrationInfo) {
                return tournamentRegistrationInfo != null
                        && !inProgress.contains(tournamentRegistrationInfo.getTournamentId());
            }
        });
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
        final Schedule rhs = (Schedule) obj;
        return new EqualsBuilder()
                .append(sortedTournaments, rhs.sortedTournaments)
                .append(inProgress, rhs.inProgress)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(sortedTournaments)
                .append(inProgress)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(sortedTournaments)
                .append(inProgress)
                .toString();
    }
}
