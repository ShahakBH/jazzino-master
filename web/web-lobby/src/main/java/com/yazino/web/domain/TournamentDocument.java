package com.yazino.web.domain;

import com.yazino.platform.tournament.TournamentRankView;
import com.yazino.platform.tournament.TournamentRoundView;
import com.yazino.platform.tournament.TournamentViewDetails;

import java.io.Serializable;
import java.util.List;

/**
 * Java representation of tournament document (json)
 */
public class TournamentDocument implements Serializable {
    private static final long serialVersionUID = -3732745146632311219L;

    private final TournamentViewDetails overview;
    private final List<TournamentRankView> ranks;
    private final Integer rankPages;
    private final Integer rankPage;
    private final List<TournamentRoundView> rounds;
    private final String sent;
    private final List<TournamentRankView> players;
    private final Integer playerPages;
    private final Integer playerPage;

    public TournamentDocument(final TournamentViewDetails overview,
                              final List<TournamentRankView> ranks,
                              final Integer rankPages,
                              final Integer rankPage,
                              final List<TournamentRoundView> rounds,
                              final List<TournamentRankView> players,
                              final Integer playerPages,
                              final Integer playerPage,
                              final String sent) {
        this.overview = overview;
        this.players = players;
        this.ranks = ranks;
        this.rankPages = rankPages;
        this.rankPage = rankPage;
        this.playerPages = playerPages;
        this.playerPage = playerPage;
        this.rounds = rounds;
        this.sent = sent;
    }

    public TournamentViewDetails getOverview() {
        return overview;
    }

    public List<TournamentRankView> getPlayers() {
        return players;
    }

    public List<TournamentRankView> getRanks() {
        return ranks;
    }

    public Integer getRankPages() {
        return rankPages;
    }

    public Integer getRankPage() {
        return rankPage;
    }

    public Integer getPlayerPages() {
        return playerPages;
    }

    public Integer getPlayerPage() {
        return playerPage;
    }

    public List<TournamentRoundView> getRounds() {
        return rounds;
    }

    public String getSent() {
        return sent;
    }
}
