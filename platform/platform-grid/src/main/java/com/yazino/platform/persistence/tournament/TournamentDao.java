package com.yazino.platform.persistence.tournament;

import com.yazino.platform.model.tournament.Tournament;

import java.util.List;

public interface TournamentDao {
    void save(Tournament tournament);

    List<Tournament> findNonClosedTournaments();
}
