package com.yazino.platform.tournament;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

public class TournamentView implements Serializable {
    private static final long serialVersionUID = -316683861160458647L;

    private final TournamentViewDetails overview;

    private Map<BigDecimal, TournamentRankView> players;

    private final List<TournamentRankView> ranks;
    private final List<TournamentRoundView> rounds;
    private final long creationTime;

    public TournamentView(final TournamentViewDetails overview,
                          final Map<BigDecimal, TournamentRankView> players,
                          final List<TournamentRankView> ranks,
                          final List<TournamentRoundView> rounds,
                          final long creationTime) {
        notNull(overview, "overview is null");
        notNull(players, "players is null");
        notNull(ranks, "ranks is null");
        notNull(rounds, "rounds is null");
        this.overview = overview;
        this.ranks = ranks;
        this.rounds = rounds;
        this.players = players;
        this.creationTime = creationTime;
    }

    public TournamentViewDetails getOverview() {
        return overview;
    }

    public Map<BigDecimal, TournamentRankView> getPlayers() {
        return players;
    }

    public List<TournamentRankView> getRanks() {
        return ranks;
    }

    public List<TournamentRoundView> getRounds() {
        return rounds;
    }

    public long getCreationTime() {
        return creationTime;
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
        final TournamentView rhs = (TournamentView) obj;
        return new EqualsBuilder()
                .append(overview, rhs.overview)
                .append(players, rhs.players)
                .append(ranks, rhs.ranks)
                .append(rounds, rhs.rounds)
                .append(creationTime, rhs.creationTime)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(overview)
                .append(players)
                .append(ranks)
                .append(rounds)
                .append(creationTime)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(overview)
                .append(players)
                .append(ranks)
                .append(rounds)
                .append(creationTime)
                .toString();
    }
}
