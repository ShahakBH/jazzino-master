package senet.server.tournament;

import com.yazino.platform.model.PagedData;
import com.yazino.platform.model.tournament.RecurringTournamentDefinition;
import com.yazino.platform.tournament.TournamentStatus;
import org.joda.time.DateTime;
import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.repository.tournament.TournamentRepository;

import java.math.BigDecimal;
import java.util.Set;

public class FitTournamentRepository implements TournamentRepository {

    public Tournament findById(final BigDecimal tournamentId) {
        return TournamentFixture.getTournamentById(tournamentId);
    }

    public Tournament lock(final BigDecimal tournamentId) {
        return TournamentFixture.getTournamentById(tournamentId);
    }

    public PagedData<Tournament> findByStatus(final TournamentStatus status, final int page) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @SuppressWarnings({"ConstantConditions"})

    @Override
    public Set<Tournament> findByPlayer(final BigDecimal playerId) {
        return null;
    }

    public void save(final Tournament tournament, boolean persist) {
        if (tournament.getSignupStartTimeStamp() == null) {
            tournament.setSignupStartTimeStamp(new DateTime().plusMinutes(5));
        }
        TournamentFixture.saveTournament(tournament);
    }

    @Override
    public void nonPersistentSave(final Tournament tournament) {
        TournamentFixture.saveTournament(tournament);
    }

    @Override
    public void save(final RecurringTournamentDefinition recurringTournamentDefinition) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    public Set<Tournament> findLocalForLeaderboardUpdates() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public PagedData<Tournament> findAll(final int page) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void loadNonClosedTournamentsIntoSpace() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void clear() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void remove(final Tournament tournament) {
    }

    @Override
    public void playerEliminatedFrom(final BigDecimal tournamentId,
                                     final BigDecimal playerId,
                                     final String gameType, final int numberOfPlayers, final int leaderboardPosition) {

    }
}
