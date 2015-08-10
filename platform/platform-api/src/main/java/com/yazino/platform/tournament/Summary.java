package com.yazino.platform.tournament;

import com.google.common.base.Predicate;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang3.Validate.notNull;

public class Summary implements Serializable {
    private static final long serialVersionUID = -8193105096852055049L;

    private final List<TournamentPlayerSummary> players = new ArrayList<>();
    private final BigDecimal tournamentId;
    private final String tournamentName;
    private final Date finishDateTime;
    private final String gameType;

    public Summary(final BigDecimal tournamentId,
                   final String tournamentName,
                   final Date finishDateTime,
                   final String gameType,
                   final List<TournamentPlayerSummary> players) {
        notNull(tournamentId, "tournamentId may not be null");
        notNull(gameType, "gameType may not be null");

        this.tournamentId = tournamentId;
        this.tournamentName = tournamentName;
        this.finishDateTime = finishDateTime;
        this.gameType = gameType;

        if (players != null) {
            this.players.addAll(players);
            Collections.sort(this.players);
        }
    }

    public BigDecimal getTournamentId() {
        return tournamentId;
    }

    public String getTournamentName() {
        return tournamentName;
    }

    public Date getFinishDateTime() {
        return finishDateTime;
    }

    public String getGameType() {
        return gameType;
    }

    public List<TournamentPlayerSummary> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public List<TournamentPlayerSummary> getPlayersWithIds(final Set<BigDecimal> filterIds) {
        final ArrayList<TournamentPlayerSummary> result = newArrayList(
                filter(players, new Predicate<TournamentPlayerSummary>() {
                    @Override
                    public boolean apply(final TournamentPlayerSummary playerSummary) {
                        return playerSummary != null && filterIds.contains(playerSummary.getId());
                    }
                }));
        Collections.sort(result);
        return result;
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

        final Summary rhs = (Summary) obj;
        return new EqualsBuilder()
                .append(tournamentName, rhs.tournamentName)
                .append(finishDateTime, rhs.finishDateTime)
                .append(players, rhs.players)
                .append(gameType, rhs.gameType)
                .isEquals()
                && BigDecimals.equalByComparison(tournamentId, rhs.tournamentId);
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(tournamentId))
                .append(tournamentName)
                .append(finishDateTime)
                .append(players)
                .append(gameType)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(tournamentId)
                .append(tournamentName)
                .append(finishDateTime)
                .append(players)
                .append(gameType)
                .toString();
    }
}
