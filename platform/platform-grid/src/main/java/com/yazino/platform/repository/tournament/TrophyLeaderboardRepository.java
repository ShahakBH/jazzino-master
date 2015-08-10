package com.yazino.platform.repository.tournament;


import com.yazino.platform.model.tournament.TrophyLeaderboard;
import org.joda.time.DateTime;
import com.yazino.game.api.time.TimeSource;

import java.math.BigDecimal;
import java.util.Set;

public interface TrophyLeaderboardRepository {

    /**
     * Finds all leaderboards for the given game type that are current (within their date band)
     * and active.
     *
     * @param currentDate the current date/time. Never null.
     * @param gameType    the game type. Never null.
     * @return all matching leaderboards. Never null.
     */

    Set<TrophyLeaderboard> findCurrentAndActiveWithGameType(DateTime currentDate, String gameType);

    /**
     * Find all leaderboards that are ready to have results calculated.
     *
     * @param timeSource the current time source.
     * @return all matching leaderboards. Never null.
     */

    Set<TrophyLeaderboard> findLocalResultingRequired(final TimeSource timeSource);

    /**
     * Save or update a leaderboard.
     *
     * @param trophyLeaderboard the leaderboard.
     */
    void save(TrophyLeaderboard trophyLeaderboard);

    /**
     * Archive a leaderboard, persisting its final state and removing from the space.
     *
     * @param trophyLeaderboard the leaderboard.
     */
    void archive(TrophyLeaderboard trophyLeaderboard);

    /**
     * Remove a leaderboard from the space.
     *
     * @param trophyLeaderboardId the leaderboard ID.
     */
    void clear(final BigDecimal trophyLeaderboardId);

    /**
     * Find all leaderboards
     *
     * @return all leaderboards. Never null
     */

    TrophyLeaderboard[] findAll();

    /**
     * Find an individual trophy leaderboard by ID.
     *
     * @param trophyLeaderboardId the ID.
     * @return the leaderboard, or null if none.
     */
    TrophyLeaderboard findById(BigDecimal trophyLeaderboardId);

    /**
     * Lock an individual trophy leaderboard by ID.
     *
     * @param trophyLeaderboardId the ID.
     * @return the locked leaderboard.
     * @throws java.util.ConcurrentModificationException
     *          if the leaderboard does not exist or is already locked.
     */
    TrophyLeaderboard lock(BigDecimal trophyLeaderboardId);

    TrophyLeaderboard findByGameType(String gameType);

    void requestUpdate(BigDecimal id,
                       BigDecimal tournamentId,
                       BigDecimal playerId,
                       String name,
                       String pictureUrl,
                       int leaderBoardPosition, final int size);
}
