package com.yazino.platform.persistence.tournament;

import com.yazino.platform.model.tournament.TournamentPlayer;

import java.math.BigDecimal;
import java.util.Set;

public interface TournamentPlayerDao {
    void save(BigDecimal tournamentId, TournamentPlayer tournamentPlayer);

    void remove(BigDecimal tournamentId, TournamentPlayer tournamentPlayer);

    Set<TournamentPlayer> findByTournamentId(BigDecimal tournamentId);
}
