package com.yazino.platform.model.tournament;

import java.math.BigDecimal;

public class PlayerBuilder {

    private BigDecimal id;
    private String name;
    private Integer leaderboardPosition;

    public PlayerBuilder() {

    }

    public PlayerBuilder(final TournamentPlayer player) {
        withId(player.getPlayerId());
        withName(player.getName());
        withLeaderboardPosition(player.getLeaderboardPosition());
    }

    public PlayerBuilder withId(final BigDecimal newId) {
        this.id = newId;
        return this;
    }

    public PlayerBuilder withName(final String newName) {
        this.name = newName;
        return this;
    }

    public PlayerBuilder withLeaderboardPosition(final Integer newLeaderboardPosition) {
        this.leaderboardPosition = newLeaderboardPosition;
        return this;
    }

    public TournamentPlayer build() {
        final TournamentPlayer player = new TournamentPlayer(id, name);
        player.setLeaderboardPosition(leaderboardPosition);
        return player;
    }
}
