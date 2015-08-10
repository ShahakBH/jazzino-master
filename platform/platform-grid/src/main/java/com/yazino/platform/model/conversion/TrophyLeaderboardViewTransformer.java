package com.yazino.platform.model.conversion;

import com.google.common.base.Function;
import com.yazino.platform.model.tournament.TrophyLeaderboard;
import com.yazino.platform.tournament.TrophyLeaderboardView;

public class TrophyLeaderboardViewTransformer implements Function<TrophyLeaderboard, TrophyLeaderboardView> {

    @Override
    public TrophyLeaderboardView apply(final TrophyLeaderboard trophyLeaderboard) {
        if (trophyLeaderboard == null) {
            return null;
        }

        return new TrophyLeaderboardView(trophyLeaderboard.getId(),
                trophyLeaderboard.getName(),
                trophyLeaderboard.getActive(),
                trophyLeaderboard.getGameType(),
                trophyLeaderboard.getPointBonusPerPlayer(),
                trophyLeaderboard.getStartTime(),
                trophyLeaderboard.getEndTime(),
                trophyLeaderboard.getCurrentCycleEnd(),
                trophyLeaderboard.getCycle(),
                trophyLeaderboard.getPositionData(),
                trophyLeaderboard.getPlayers());
    }
}
