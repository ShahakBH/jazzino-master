package com.yazino.web.domain;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.yazino.platform.tournament.TrophyLeaderboardPlayer;
import com.yazino.platform.tournament.TrophyLeaderboardPosition;
import com.yazino.platform.tournament.TrophyLeaderboardView;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.math.BigDecimal;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;


/**
 * Transforms a TrophyLeaderboardView into a TrophyLeaderBoardSummary. If the view is null an empty summary is returned.
 * Players returned are the top N ordered by position. Position data is ordered by position, non paying positions are
 * not included.
 *
 * @see TrophyLeaderBoardSummary
 */
public final class TrophyLeaderBoardSummaryTransformer
        implements Function<TrophyLeaderboardView, TrophyLeaderBoardSummary> {
    private final BigDecimal playerId;
    private int maxPositions;
    private Set<BigDecimal> idsToInclude;

    /**
     * Limit number of players returned
     *
     * @param playerId     id of player
     * @param maxPositions maximum number of players to return
     */
    public TrophyLeaderBoardSummaryTransformer(final BigDecimal playerId, final int maxPositions) {
        this(playerId, maxPositions, Collections.<BigDecimal>emptySet());
    }

    /**
     * Filter out ids not in idsToInclude and then limit resulting players to maxPositions
     *
     * @param maxPositions maximum number of players to return
     * @param idsToInclude only consider these players
     */
    public TrophyLeaderBoardSummaryTransformer(final BigDecimal playerId,
                                               final int maxPositions,
                                               final Set<BigDecimal> idsToInclude) {
        this.playerId = playerId;
        this.maxPositions = maxPositions;
        this.idsToInclude = idsToInclude;
    }

    @Override
    public TrophyLeaderBoardSummary apply(final TrophyLeaderboardView trophyLeaderboardView) {
        if (trophyLeaderboardView == null) {
            return new TrophyLeaderBoardSummary(Collections.<TrophyLeaderboardPlayer>emptyList(),
                    Collections.<TrophyLeaderboardPosition>emptyList(), null, 0);
        }
        final List<TrophyLeaderboardPosition> positions = getLeaderBoardPositions(trophyLeaderboardView);
        final long millisToCycleEnd = new Duration(new DateTime(),
                trophyLeaderboardView.getCurrentCycleEnd()).getMillis();
        final List<TrophyLeaderboardPlayer> playersOrderedByPosition = getTopPlayers(trophyLeaderboardView);

        TrophyLeaderboardPlayer player = null;
        if (idsToInclude.isEmpty()) {
            player = getPlayer(playersOrderedByPosition);
            // if player in top positions don't return
            if (player != null) {
                player = null;
            } else {
                //not in top positions so find player if in leader board
                player = getPlayer(trophyLeaderboardView.getOrderedByPosition());
            }
        }
        return new TrophyLeaderBoardSummary(playersOrderedByPosition, positions, player, Math.max(0, millisToCycleEnd));
    }

    private List<TrophyLeaderboardPlayer> getTopPlayers(final TrophyLeaderboardView trophyLeaderboardView) {
        List<TrophyLeaderboardPlayer> orderedByPosition;
        if (idsToInclude.isEmpty()) {
            orderedByPosition = trophyLeaderboardView.getOrderedByPosition();
        } else {
            orderedByPosition = trophyLeaderboardView.getFilteredOrderedByPosition(idsToInclude);
        }
        if (orderedByPosition.isEmpty()) {
            return orderedByPosition;
        } else {
            return orderedByPosition.subList(0, Math.min(orderedByPosition.size(), maxPositions));
        }
    }

    private List<TrophyLeaderboardPosition> getLeaderBoardPositions(final TrophyLeaderboardView trophyLeaderboardView) {
        final Map<Integer, TrophyLeaderboardPosition> positionData = trophyLeaderboardView.getPositionData();
        List<TrophyLeaderboardPosition> positions;
        if (positionData == null || positionData.isEmpty()) {
            positions = Collections.emptyList();
        } else {
            positions = newArrayList(Iterables.filter(positionData.values(),
                    new Predicate<TrophyLeaderboardPosition>() {
                        @Override
                        public boolean apply(final TrophyLeaderboardPosition trophyLeaderboardPosition) {
                            return trophyLeaderboardPosition.getAwardPayout() > 0;
                        }
                    }));
            Collections.sort(positions, new Comparator<TrophyLeaderboardPosition>() {
                @Override
                public int compare(final TrophyLeaderboardPosition p1, final TrophyLeaderboardPosition p2) {
                    return p1.getPosition() - p2.getPosition();
                }
            });
        }
        return positions;
    }

    private TrophyLeaderboardPlayer getPlayer(final List<TrophyLeaderboardPlayer> orderedByPosition) {
        for (TrophyLeaderboardPlayer player : orderedByPosition) {
            if (player.getPlayerId().equals(playerId)) {
                return player;
            }
        }
        return null;
    }
}
