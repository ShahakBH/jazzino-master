package com.yazino.platform.tournament;

import com.google.common.collect.Sets;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

public class TournamentRegistrationInfo
        implements Serializable, Comparator<TournamentRegistrationInfo>, Comparable<TournamentRegistrationInfo> {
    private static final long serialVersionUID = 6803901852744651230L;

    private final Set<BigDecimal> players = new HashSet<>();
    private final BigDecimal tournamentId;
    private final DateTime startTimeStamp;
    private final BigDecimal entryFee;
    private final BigDecimal currentPrizePool;
    private final String name;
    private final String description;
    private final String variationTemplateName;

    public TournamentRegistrationInfo(final BigDecimal tournamentId,
                                      final DateTime startTimeStamp,
                                      final BigDecimal entryFee,
                                      final BigDecimal currentPrizePool,
                                      final String name,
                                      final String description,
                                      final String variationTemplateName,
                                      final Set<BigDecimal> playerIds) {
        notNull(tournamentId, "tournamentId may not be null");

        this.tournamentId = tournamentId;
        this.startTimeStamp = startTimeStamp;
        this.entryFee = entryFee;
        this.currentPrizePool = currentPrizePool;
        this.name = name;
        this.description = description;
        this.variationTemplateName = variationTemplateName;

        if (playerIds != null) {
            players.addAll(playerIds);
        }
    }

    public int countMatchingPlayersRegistered(final Set<BigDecimal> playerIds) {
        if (playerIds != null) {
            return Sets.intersection(players, playerIds).size();
        }
        return 0;
    }

    public boolean isRegistered(final BigDecimal playerId) {
        return playerId != null && players.contains(playerId);

    }

    public BigDecimal getTournamentId() {
        return tournamentId;
    }

    public DateTime getStartTimeStamp() {
        return startTimeStamp;
    }

    public BigDecimal getEntryFee() {
        return entryFee;
    }

    public BigDecimal getCurrentPrizePool() {
        return currentPrizePool;
    }

    public Set<BigDecimal> getPlayers() {
        return new HashSet<>(players);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getVariationTemplateName() {
        return variationTemplateName;
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
        final TournamentRegistrationInfo rhs = (TournamentRegistrationInfo) obj;
        return new EqualsBuilder()
                .append(currentPrizePool, rhs.currentPrizePool)
                .append(entryFee, rhs.entryFee)
                .append(players, rhs.players)
                .append(startTimeStamp, rhs.startTimeStamp)
                .append(name, rhs.name)
                .append(description, rhs.description)
                .append(variationTemplateName, rhs.variationTemplateName)
                .isEquals()
                && BigDecimals.equalByComparison(tournamentId, rhs.tournamentId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(currentPrizePool)
                .append(entryFee)
                .append(players)
                .append(startTimeStamp)
                .append(name)
                .append(description)
                .append(variationTemplateName)
                .append(BigDecimals.strip(tournamentId))
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(currentPrizePool)
                .append(entryFee)
                .append(players)
                .append(startTimeStamp)
                .append(name)
                .append(description)
                .append(variationTemplateName)
                .append(tournamentId)
                .toString();
    }

    @Override
    public int compareTo(final TournamentRegistrationInfo target) {
        if (target != null) {
            return compare(this, target);
        }
        return 1;
    }

    @Override
    public int compare(final TournamentRegistrationInfo info1, final TournamentRegistrationInfo info2) {
        final int compareTo = info1.getStartTimeStamp().compareTo(info2.getStartTimeStamp());
        if (compareTo == 0) {
            return info1.getTournamentId().compareTo(info2.getTournamentId());
        }
        return compareTo;
    }

    public int getNumberOfPlayers() {
        return players.size();
    }
}
