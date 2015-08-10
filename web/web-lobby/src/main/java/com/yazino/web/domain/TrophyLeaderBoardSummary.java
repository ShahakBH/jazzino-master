package com.yazino.web.domain;

import com.yazino.platform.tournament.TrophyLeaderboardPlayer;
import com.yazino.platform.tournament.TrophyLeaderboardPosition;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;

/**
 * Summary of current leader board. Contains, top players (ordered by position), positions paying out (ordered by
 * position), player (if not in top players (of world or top players of friends, including player)
 * or not in tournament) and the number of milli seconds before the current
 * leader board ends.
 */
public class TrophyLeaderBoardSummary {
    private final List<TrophyLeaderboardPlayer> players;
    private final List<TrophyLeaderboardPosition> positions;
    private final long millisToCycleEnd;
    private final TrophyLeaderboardPlayer player;

    public TrophyLeaderBoardSummary(final List<TrophyLeaderboardPlayer> players,
                                    final List<TrophyLeaderboardPosition> positions,
                                    final TrophyLeaderboardPlayer player,
                                    final long millisToCycleEnd) {
        this.players = players;
        this.positions = positions;
        this.player = player;
        this.millisToCycleEnd = millisToCycleEnd;
    }

    /**
     * @return players ordered by position, empty if no players
     */
    public List<TrophyLeaderboardPlayer> getPlayers() {
        return players;
    }

    /**
     * @return positions ordered by position, empty if no position, no none paying positions
     */
    public List<TrophyLeaderboardPosition> getPositions() {
        return positions;
    }

    /**
     * @return millisecs to end of current cycle, 0 if current cycle ended
     */
    public long getMillisToCycleEnd() {
        return millisToCycleEnd;
    }

    public TrophyLeaderboardPlayer getPlayer() {
        return player;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE);
    }
}
