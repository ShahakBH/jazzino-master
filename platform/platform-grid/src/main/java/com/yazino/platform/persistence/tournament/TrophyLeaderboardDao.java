package com.yazino.platform.persistence.tournament;


import com.yazino.platform.model.tournament.TrophyLeaderboard;

public interface TrophyLeaderboardDao {
    void save(TrophyLeaderboard trophyLeaderboard);
}
