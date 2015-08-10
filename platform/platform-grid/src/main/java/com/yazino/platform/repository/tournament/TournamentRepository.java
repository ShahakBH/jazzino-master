package com.yazino.platform.repository.tournament;


import com.yazino.platform.model.PagedData;
import com.yazino.platform.model.tournament.RecurringTournamentDefinition;
import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.tournament.TournamentStatus;

import java.math.BigDecimal;
import java.util.Set;

public interface TournamentRepository {
    /**
     * Do a non blocking read on the Tournament.
     *
     * @param tournamentId the ID of the tournament to read.
     * @return the tournament, or null if it doesn't exist.
     */
    Tournament findById(BigDecimal tournamentId);

    /**
     * Find all loaded tournaments with the given status.
     *
     * @param status the status of the tournaments to read. Not null.
     * @param page the page to fetch.
     * @return the tournaments. Never null.
     */
    PagedData<Tournament> findByStatus(TournamentStatus status, final int page);

    /**
     * Find all loaded tournaments where the given player is registered.
     *
     * @param playerId the player ID. Never null.
     * @return the tournaments. Never null.
     */
    Set<Tournament> findByPlayer(BigDecimal playerId);

    /**
     * Find all loaded tournaments.
     *
     * @param page the page to fetch.
     * @return all loaded tournaments.
     */
    PagedData<Tournament> findAll(final int page);

    /**
     * Find all tournaments that require leaderboard updates.
     *
     * @return all tournaments requiring leaderboard updates.
     */
    Set<Tournament> findLocalForLeaderboardUpdates();

    /**
     * A blocking read on the Tournament.
     *
     * @param tournamentId the ID of the tournament to lock.
     * @return the tournment. Never null.
     * @throws java.util.ConcurrentModificationException
     *          if the tournmanet lock cannot be obtained.
     */
    Tournament lock(BigDecimal tournamentId);

    /**
     * Create or update a tournament.
     *
     * @param tournament the tournament to create/update. Not null.
     * @param persist
     */
    void save(Tournament tournament, boolean persist);

    /**
     * Saves a tournament to the space without any persistence request.
     *
     * @param tournament the tournament. Not null.
     */
    void nonPersistentSave(Tournament tournament);

    void save(RecurringTournamentDefinition recurringTournamentDefinition);

    /**
     * Load all non-closed tournaments from the persisted storage into the space.
     */
    void loadNonClosedTournamentsIntoSpace();

    /**
     * Remove all tournaments from the space.
     */
    void clear();

    /**
     * Remove the tournament and supporting objects from the space.
     *
     * @param tournament the tournament.
     */
    void remove(Tournament tournament);

    /**
     * Write an elimination req
     *
     * @param tournamentId the ID of the tournament the player was eliminated from.
     * @param playerId the ID of the eliminated player.
     * @param gameType
     * @param numberOfPlayers
     * @param leaderboardPosition the position they were eliminated at.
     */
    void playerEliminatedFrom(BigDecimal tournamentId,
                              BigDecimal playerId,
                              final String gameType, final int numberOfPlayers, int leaderboardPosition);
}
