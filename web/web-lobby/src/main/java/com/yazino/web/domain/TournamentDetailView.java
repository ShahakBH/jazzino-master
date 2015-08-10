package com.yazino.web.domain;

import com.yazino.platform.tournament.TournamentRankView;
import com.yazino.platform.tournament.TournamentView;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

public class TournamentDetailView implements Serializable {
    private static final long serialVersionUID = 1671600641706545654L;

    private final List<TournamentDetailViewPlayer> players = new ArrayList<>();
    private final List<TournamentDetailViewRank> ranks = new ArrayList<>();

    private final BigDecimal tournamentId;
    private final String name;
    private final String gameType;
    private final String variationName;
    private final String description;
    private final long millisToStart;
    private final boolean registered;
    private final BigDecimal prizePool;
    private final BigDecimal entryFee;

    public TournamentDetailView(final TournamentView tournamentView,
                                final BigDecimal playerId,
                                final Collection<BigDecimal> friendIds) {
        notNull(tournamentView, "tournamentView may not be null");
        notNull(playerId, "playerId may not be null");
        notNull(friendIds, "friendIds may not be null");

        this.tournamentId = tournamentView.getOverview().getTournamentId();
        this.name = tournamentView.getOverview().getName();
        this.gameType = tournamentView.getOverview().getGameType();
        this.variationName = tournamentView.getOverview().getVariationTemplateName();
        this.description = tournamentView.getOverview().getDescription();
        this.millisToStart = tournamentView.getOverview().getMillisTillStart();
        this.registered = tournamentView.getPlayers().containsKey(playerId);
        this.prizePool = tournamentView.getOverview().getPrizePool();
        this.entryFee = tournamentView.getOverview().getEntryFee();

        for (BigDecimal tournamentPlayerId : tournamentView.getPlayers().keySet()) {
            players.add(new TournamentDetailViewPlayer(tournamentPlayerId,
                    tournamentView.getPlayers().get(tournamentPlayerId).getPlayerName(),
                    friendIds.contains(tournamentPlayerId)));
        }

        for (TournamentRankView rank : tournamentView.getRanks()) {
            ranks.add(new TournamentDetailViewRank(rank.getRank(), rank.getPrize()));
        }
    }

    public boolean isInProgress() {
        return millisToStart < 0;
    }

    public int getRegisteredPlayers() {
        return players.size();
    }

    public int getRegisteredFriends() {
        int registeredFriends = 0;

        for (TournamentDetailViewPlayer player : players) {
            if (player.isFriend()) {
                ++registeredFriends;
            }
        }

        return registeredFriends;
    }

    public List<TournamentDetailViewPlayer> getPlayers() {
        return players;
    }

    public List<TournamentDetailViewRank> getRanks() {
        return ranks;
    }

    public BigDecimal getTournamentId() {
        return tournamentId;
    }

    public String getName() {
        return name;
    }

    public String getGameType() {
        return gameType;
    }

    public String getVariationName() {
        return variationName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isRegistered() {
        return registered;
    }

    public BigDecimal getPrizePool() {
        return prizePool;
    }

    public BigDecimal getEntryFee() {
        return entryFee;
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
        TournamentDetailView rhs = (TournamentDetailView) obj;
        return new EqualsBuilder()
                .append(this.players, rhs.players)
                .append(this.ranks, rhs.ranks)
                .append(this.tournamentId, rhs.tournamentId)
                .append(this.name, rhs.name)
                .append(this.gameType, rhs.gameType)
                .append(this.variationName, rhs.variationName)
                .append(this.description, rhs.description)
                .append(this.millisToStart, rhs.millisToStart)
                .append(this.registered, rhs.registered)
                .append(this.prizePool, rhs.prizePool)
                .append(this.entryFee, rhs.entryFee)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(players)
                .append(ranks)
                .append(tournamentId)
                .append(name)
                .append(gameType)
                .append(variationName)
                .append(description)
                .append(millisToStart)
                .append(registered)
                .append(prizePool)
                .append(entryFee)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("players", players)
                .append("ranks", ranks)
                .append("tournamentId", tournamentId)
                .append("name", name)
                .append("gameType", gameType)
                .append("variationName", variationName)
                .append("description", description)
                .append("millisToStart", millisToStart)
                .append("registered", registered)
                .append("prizePool", prizePool)
                .append("entryFee", entryFee)
                .toString();
    }
}
