package com.yazino.platform.tournament;


import com.yazino.platform.model.PagedData;
import org.openspaces.remoting.Routing;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Public interface to tournament operations.
 */
public interface TournamentService {

    /**
     * Create a tournament.
     *
     * @param tournamentDefinition the populated source data for the tournament. Not null.
     * @return the ID of the tournament. Never null.
     * @throws TournamentException if the tournament creation fails.
     */
    BigDecimal createTournament(@Routing("getId") TournamentDefinition tournamentDefinition)
            throws TournamentException;

    /**
     * Find all tournaments in the space.
     *
     * @param page
     * @return all tournaments loaded into the space.
     */
    PagedData<TournamentMonitorView> findAll(final int page);

    /**
     * Find all tournaments in the space with the given status.
     *
     * @param status the status. Not null.
     * @param page
     * @return all tournaments loaded into the space with the given status. Never null.
     */
    PagedData<TournamentMonitorView> findByStatus(TournamentStatus status, final int page);

    /**
     * Cancel the specified tournament
     *
     * @param toCancel Tournament id, never null, may be empty
     * @return true if the request to cancel the tournament was successful.
     */
    boolean cancelTournament(@Routing BigDecimal toCancel);

    /**
     * Load all non-closed tournaments and their players from the persisted storage into the space.
     */
    void populateSpaceWithNonClosedTournaments();

    /**
     * Remove all tournaments and their players from the space.
     */
    void clearSpace();

    void saveRecurringTournamentDefinition(RecurringTournament definition);

    TournamentView findViewById(@Routing BigDecimal tournamentId);

    TournamentDetail findDetailById(@Routing BigDecimal tournamentId);

    Summary findLastTournamentSummary(String gameType);

    Schedule getTournamentSchedule(@Routing String gameType);

    TournamentOperationResult register(@Routing BigDecimal tournamentId,
                                       BigDecimal playerId,
                                       boolean async);

    TournamentOperationResult deregister(@Routing BigDecimal tournamentId,
                                         BigDecimal playerId,
                                         boolean async);

    Set<BigDecimal> findTableIdsFor(@Routing BigDecimal tournamentId);
}
