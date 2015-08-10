package com.yazino.web.domain;

import com.yazino.platform.tournament.TournamentRankView;
import com.yazino.platform.tournament.TournamentRoundView;
import com.yazino.platform.tournament.TournamentViewDetails;

import java.util.List;

public class TournamentDocumentBuilder {
    private TournamentViewDetails overview;
    private List<TournamentRankView> ranks;
    private Integer rankPages;
    private Integer rankPage;
    private List<TournamentRoundView> rounds;
    private String sent;
    private List<TournamentRankView> players;
    private Integer playerPages;
    private Integer playerPage;

    public TournamentDocumentBuilder withOverview(final TournamentViewDetails newOverview) {
        this.overview = newOverview;
        return this;
    }

    public TournamentDocumentBuilder withRanks(final List<TournamentRankView> newRanks) {
        this.ranks = newRanks;
        return this;
    }

    public TournamentDocumentBuilder withRankPages(final Integer newRankPages) {
        this.rankPages = newRankPages;
        return this;
    }

    public TournamentDocumentBuilder withRankPage(final Integer newRankPage) {
        this.rankPage = newRankPage;
        return this;
    }

    public TournamentDocumentBuilder withPlayerPages(final Integer newPlayerPages) {
        this.playerPages = newPlayerPages;
        return this;
    }

    public TournamentDocumentBuilder withPlayerPage(final Integer newPlayerPage) {
        this.playerPage = newPlayerPage;
        return this;
    }

    public TournamentDocumentBuilder withRounds(final List<TournamentRoundView> newRounds) {
        this.rounds = newRounds;
        return this;
    }

    public TournamentDocumentBuilder withSent(final String newSent) {
        this.sent = newSent;
        return this;
    }

    public TournamentDocumentBuilder withPlayers(final List<TournamentRankView> newPlayers) {
        this.players = newPlayers;
        return this;
    }

    public TournamentDocument build() {
        return new TournamentDocument(
                overview, ranks, rankPages, rankPage, rounds, players, playerPages, playerPage, sent);
    }
}
