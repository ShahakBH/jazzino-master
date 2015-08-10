package com.yazino.platform.repository.tournament;


import com.yazino.platform.model.tournament.TournamentSummary;

import java.math.BigDecimal;

public interface TournamentSummaryRepository {

    /**
     * Get the summary of the most recently finished tournament of a specific game type.
     *
     * @param gameType the game type of the tournament to find.
     * @return the most recently finished tournament, or null if none.
     */
    TournamentSummary findMostRecent(final String gameType);

    /**
     * Find a summary by tournament ID.
     *
     * @param tournamentId the ID of the tournament.
     * @return the summary, or null if none for the given tournament.
     */
    TournamentSummary findByTournamentId(BigDecimal tournamentId);

    /**
     * Save the given summary.
     *
     * @param summary the summary to save.
     */
    void save(TournamentSummary summary);

    /**
     * Delete a summary.
     *
     * @param tournamentId the ID of the tournament for which the summary should be deleted.
     */
    void delete(BigDecimal tournamentId);

}
