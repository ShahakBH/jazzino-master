package com.yazino.platform.repository.tournament;


import com.yazino.platform.model.tournament.TrophyLeaderboardResult;
import org.joda.time.DateTime;
import com.yazino.game.api.time.TimeSource;

import java.math.BigDecimal;
import java.util.Set;

public interface TrophyLeaderboardResultRepository {

    /**
     * Save a leaderboard result.
     *
     * @param trophyLeaderboardResult the result. Not null.
     */
    void save(TrophyLeaderboardResult trophyLeaderboardResult);

    TrophyLeaderboardResult findByIdAndTime(BigDecimal trophyLeaderboardId,
                                            DateTime resultTime);

    Set<TrophyLeaderboardResult> findExpired(TimeSource timeSource);

    void remove(final BigDecimal trophyLeaderboardResultId,
                final DateTime resultTime);
}
