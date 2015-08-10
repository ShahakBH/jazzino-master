package com.yazino.platform.persistence.tournament;

import com.yazino.platform.model.tournament.TournamentSummary;

import java.math.BigDecimal;

public interface TournamentSummaryDao {
    void save(TournamentSummary summary);

    void delete(BigDecimal tournamentId);
}
