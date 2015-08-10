package senet.server.tournament;


import com.yazino.platform.model.tournament.TournamentSummary;
import com.yazino.platform.repository.tournament.TournamentSummaryRepository;

import java.math.BigDecimal;

public class FitTournamentSummaryRepository implements TournamentSummaryRepository {

    @Override
    public TournamentSummary findMostRecent(final String gameType) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public TournamentSummary findByTournamentId(final BigDecimal tournamentId) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public void save(final TournamentSummary summary) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public void delete(final BigDecimal tournamentId) {
        throw new UnsupportedOperationException("Unimplemented");
    }
}
