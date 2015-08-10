package senet.server.tournament;


import org.joda.time.DateTime;
import com.yazino.game.api.time.TimeSource;
import com.yazino.platform.model.tournament.TrophyLeaderboard;
import com.yazino.platform.repository.tournament.TrophyLeaderboardRepository;

import java.math.BigDecimal;
import java.util.Set;

public class FitTournamentLeaderboardRepository implements TrophyLeaderboardRepository {


    @Override
    public Set<TrophyLeaderboard> findCurrentAndActiveWithGameType(final DateTime currentDate, final String gameType) {
        throw new UnsupportedOperationException("Unimplemented");
    }


    @Override
    public Set<TrophyLeaderboard> findLocalResultingRequired(final TimeSource timeSource) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public void save(final TrophyLeaderboard trophyLeaderboard) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public void archive(final TrophyLeaderboard trophyLeaderboard) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public void clear(final BigDecimal trophyLeaderboardId) {
        throw new UnsupportedOperationException("Unimplemented");
    }


    @Override
    public TrophyLeaderboard[] findAll() {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public TrophyLeaderboard findById(final BigDecimal trophyLeaderboardId) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public TrophyLeaderboard lock(final BigDecimal trophyLeaderboardId) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public TrophyLeaderboard findByGameType(final String gameType) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public void requestUpdate(final BigDecimal id,
                              final BigDecimal tournamentId,
                              final BigDecimal playerId,
                              final String name,
                              final String pictureUrl,
                              final int leaderBoardPosition, final int size) {
        throw new UnsupportedOperationException("Unimplemented");
    }
}
