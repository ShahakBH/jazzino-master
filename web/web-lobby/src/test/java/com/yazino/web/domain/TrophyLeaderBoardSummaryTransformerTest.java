package com.yazino.web.domain;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.yazino.platform.tournament.TrophyLeaderboardPlayer;
import com.yazino.platform.tournament.TrophyLeaderboardPlayers;
import com.yazino.platform.tournament.TrophyLeaderboardPosition;
import com.yazino.platform.tournament.TrophyLeaderboardView;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static com.yazino.web.domain.TrophyLeaderBoardSummaryMatcher.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TrophyLeaderBoardSummaryTransformerTest {

    public static final BigDecimal LEADER_BOARD_ID = BigDecimal.TEN;
    public static final String NAME = "Jack's Race to the Top";
    public static final String GAME_TYPE = "BLACKJACK";
    public static final long POINT_BONUS_PER_PLAYER = 10L;
    public static final DateTime START_TIME = new DateTime().minusWeeks(1);
    public static final DateTime END_TIME = START_TIME.plusYears(1);
    public static final Duration CYCLE = Duration.standardDays(7);
    public static final BigDecimal PLAYER_ID_NOT_IN_TOP_POSITIONS = BigDecimal.valueOf(-1);

    @Before
    public void initialiseJodaTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime().getMillis());
    }

    @After
    public void resetJodaTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void shouldTransformView() {
        // given a view with 10 players
        TrophyLeaderboardPlayers leaderboardPlayers = createPlayers(10);
        // and 3 positions
        Map<Integer, TrophyLeaderboardPosition> positions = createPositions(3, 1);
        // and cycle end of
        DateTime currentCycleEnd = new DateTime().plusHours(2);
        TrophyLeaderboardView trophyLeaderboardView = new TrophyLeaderboardView(LEADER_BOARD_ID, NAME, Boolean.TRUE,
                GAME_TYPE, POINT_BONUS_PER_PLAYER, START_TIME, END_TIME, currentCycleEnd, CYCLE,
                positions, leaderboardPlayers);

        // when transforming view to summary, for top 5 players
        final TrophyLeaderBoardSummary summary = new TrophyLeaderBoardSummaryTransformer(PLAYER_ID_NOT_IN_TOP_POSITIONS,
                5).apply(trophyLeaderboardView);

        // then summary should have: top 5 players ordered by position, position data (ordered by position)
        // and millis to current cycle end
        List<TrophyLeaderboardPlayer> expectedPlayers = new ArrayList<TrophyLeaderboardPlayer>
                (leaderboardPlayers.getOrderedByPosition().subList(0, 5));
        final List<TrophyLeaderboardPosition> expectedPositions = filterAndSortPositionData(
                new ArrayList<TrophyLeaderboardPosition>(positions.values()));
        // millis to currentCycleEnd
        long millisToCycleEnd = new Duration(new DateTime(), currentCycleEnd).getMillis();
        TrophyLeaderBoardSummary expected = new TrophyLeaderBoardSummary(expectedPlayers, expectedPositions,
                null, millisToCycleEnd);
        assertThat(summary, is(expected));
    }

    @Test
    public void summaryShouldHaveIncludedPlayersOnly() {
        // given a view with 10 players
        TrophyLeaderboardPlayers leaderboardPlayers = createPlayers(10);
        // and cycle end of
        DateTime currentCycleEnd = new DateTime().plusHours(2);
        TrophyLeaderboardView trophyLeaderboardView = new TrophyLeaderboardView(LEADER_BOARD_ID, NAME, Boolean.TRUE,
                GAME_TYPE, POINT_BONUS_PER_PLAYER, START_TIME, END_TIME, currentCycleEnd, CYCLE,
                null, leaderboardPlayers);

        // when transforming view to summary, for top 5 players with ids of {1, 5}
        final HashSet<BigDecimal> idsToInclude = new HashSet<BigDecimal>(
                Arrays.asList(BigDecimal.valueOf(1), BigDecimal.valueOf(5)));
        final TrophyLeaderBoardSummary summary = new TrophyLeaderBoardSummaryTransformer(PLAYER_ID_NOT_IN_TOP_POSITIONS,
                5, idsToInclude).apply(trophyLeaderboardView);

        // then summary should have: top 2 players ordered by position and millis to current cycle end
        final List<TrophyLeaderboardPlayer> orderedByPosition = leaderboardPlayers.getOrderedByPosition();
        List<TrophyLeaderboardPlayer> expectedPlayers = Arrays.asList(orderedByPosition.get(0),
                orderedByPosition.get(4));
        // millis to currentCycleEnd
        long millisToCycleEnd = new Duration(new DateTime(), currentCycleEnd).getMillis();
        TrophyLeaderBoardSummary expected = new TrophyLeaderBoardSummary(expectedPlayers,
                Collections.<TrophyLeaderboardPosition>emptyList(),
                null,
                millisToCycleEnd);
        assertThat(summary, is(expected));
    }

    @Test
    public void shouldTransformViewWithAllPlayersWhenTotalPlayersIsLessThanRequestedPlayers() {
        // given a view with 2 players
        TrophyLeaderboardPlayers leaderboardPlayers = createPlayers(2);
        // and cycle end of
        DateTime currentCycleEnd = new DateTime().plusHours(2);
        TrophyLeaderboardView trophyLeaderboardView = new TrophyLeaderboardView(LEADER_BOARD_ID, NAME, Boolean.TRUE,
                GAME_TYPE, POINT_BONUS_PER_PLAYER, START_TIME, END_TIME, currentCycleEnd, CYCLE,
                null, leaderboardPlayers);

        // when transforming view to summary, for top 5 players
        final TrophyLeaderBoardSummary summary = new TrophyLeaderBoardSummaryTransformer(PLAYER_ID_NOT_IN_TOP_POSITIONS,
                5).apply(trophyLeaderboardView);

        // then summary should have: all (2) players ordered by position and millis to current cycle end
        List<TrophyLeaderboardPlayer> expectedPlayers = new ArrayList<TrophyLeaderboardPlayer>(
                leaderboardPlayers.getOrderedByPosition());
        // millis to currentCycleEnd
        long millisToCycleEnd = new Duration(new DateTime(), currentCycleEnd).getMillis();
        TrophyLeaderBoardSummary expected = new TrophyLeaderBoardSummary(expectedPlayers,
                Collections.<TrophyLeaderboardPosition>emptyList(),
                null,
                millisToCycleEnd);
        assertThat(summary, is(expected));
    }

    @Test
    public void shouldOnlyHavePositionDataForPayingPositions() {
        // Given 3 positions
        Map<Integer, TrophyLeaderboardPosition> positions = createPositions(3, 4);
        // and cycle end of
        DateTime currentCycleEnd = new DateTime().plusHours(2);
        TrophyLeaderboardView trophyLeaderboardView = new TrophyLeaderboardView(LEADER_BOARD_ID, NAME, Boolean.TRUE,
                GAME_TYPE, POINT_BONUS_PER_PLAYER, START_TIME, END_TIME, currentCycleEnd, CYCLE,
                positions, null);

        // when transforming view to summary
        final TrophyLeaderBoardSummary summary = new TrophyLeaderBoardSummaryTransformer(PLAYER_ID_NOT_IN_TOP_POSITIONS,
                5).apply(trophyLeaderboardView);

        // then summary should have: position data ordered by position, excluding positions with no payout
        final List<TrophyLeaderboardPosition> expectedPositions = filterAndSortPositionData(
                new ArrayList<TrophyLeaderboardPosition>(positions.values()));
        // millis to currentCycleEnd
        long millisToCycleEnd = new Duration(new DateTime(), currentCycleEnd).getMillis();
        TrophyLeaderBoardSummary expected = new TrophyLeaderBoardSummary(
                Collections.<TrophyLeaderboardPlayer>emptyList(),
                expectedPositions,
                null,
                millisToCycleEnd);
        assertThat(summary, is(expected));
    }

    private List<TrophyLeaderboardPosition> filterAndSortPositionData(List<TrophyLeaderboardPosition> expectedPositions) {
        final List<TrophyLeaderboardPosition> filteredPositions = newArrayList(Iterables.filter(expectedPositions,
                new Predicate<TrophyLeaderboardPosition>() {
                    @Override
                    public boolean apply(TrophyLeaderboardPosition trophyLeaderboardPosition) {
                        return trophyLeaderboardPosition.getAwardPayout() > 0;
                    }
                }));
        Collections.sort(filteredPositions, new Comparator<TrophyLeaderboardPosition>() {
            @Override
            public int compare(TrophyLeaderboardPosition p1, TrophyLeaderboardPosition p2) {
                return p1.getPosition() - p2.getPosition();
            }
        });
        return filteredPositions;
    }

    @Test
    public void minimumMillisToCycleEndShouldBeZero() {
        // GIVEN a leaderboard that already has reached cycle end
        DateTime currentCycleEnd = new DateTime().minusHours(2);
        TrophyLeaderboardView trophyLeaderboardView = new TrophyLeaderboardView(LEADER_BOARD_ID, NAME, Boolean.TRUE,
                GAME_TYPE, POINT_BONUS_PER_PLAYER, START_TIME, END_TIME, currentCycleEnd, CYCLE,
                null, null);

        // when transforming view to summary
        final TrophyLeaderBoardSummary summary = new TrophyLeaderBoardSummaryTransformer(PLAYER_ID_NOT_IN_TOP_POSITIONS,
                5).apply(trophyLeaderboardView);

        // then summary should have: 0 millis
        TrophyLeaderBoardSummary expected = new TrophyLeaderBoardSummary(
                Collections.<TrophyLeaderboardPlayer>emptyList(),
                Collections.<TrophyLeaderboardPosition>emptyList(),
                null,
                0);
        assertThat(summary, is(expected));
    }

    @Test
    public void shouldNotIncludePlayerWhenPlayerInTopPositions() {
        // given a view with 10 players
        TrophyLeaderboardPlayers leaderboardPlayers = createPlayers(10);
        // and player in top players
        BigDecimal playerId = BigDecimal.ONE;
        // and cycle end of
        DateTime currentCycleEnd = new DateTime().plusHours(2);
        TrophyLeaderboardView trophyLeaderboardView = new TrophyLeaderboardView(LEADER_BOARD_ID, NAME, Boolean.TRUE,
                GAME_TYPE, POINT_BONUS_PER_PLAYER, START_TIME, END_TIME, currentCycleEnd, CYCLE,
                null, leaderboardPlayers);

        // when transforming view to summary, for top 5 players
        final TrophyLeaderBoardSummary summary = new TrophyLeaderBoardSummaryTransformer(playerId, 5).apply(
                trophyLeaderboardView);

        // then summary should have: top 5 players ordered by position, position data (ordered by position)
        // and millis to current cycle end
        List<TrophyLeaderboardPlayer> expectedPlayers = new ArrayList<TrophyLeaderboardPlayer>
                (leaderboardPlayers.getOrderedByPosition().subList(0, 5));
        // millis to currentCycleEnd
        long millisToCycleEnd = new Duration(new DateTime(), currentCycleEnd).getMillis();
        TrophyLeaderBoardSummary expected = new TrophyLeaderBoardSummary(expectedPlayers,
                Collections.<TrophyLeaderboardPosition>emptyList(),
                null,
                millisToCycleEnd);
        assertThat(summary, is(expected));
    }

    @Test
    public void shouldIncludePlayerWhenPlayerNotInTopPositions() {
        // given a view with 10 players
        TrophyLeaderboardPlayers leaderboardPlayers = createPlayers(10);
        // and player outside top 5
        BigDecimal playerId = BigDecimal.valueOf(7);
        // and cycle end of
        DateTime currentCycleEnd = new DateTime().plusHours(2);
        TrophyLeaderboardView trophyLeaderboardView = new TrophyLeaderboardView(LEADER_BOARD_ID, NAME, Boolean.TRUE,
                GAME_TYPE, POINT_BONUS_PER_PLAYER, START_TIME, END_TIME, currentCycleEnd, CYCLE,
                null, leaderboardPlayers);

        // when transforming view to summary, for top 5 players
        final TrophyLeaderBoardSummary summary = new TrophyLeaderBoardSummaryTransformer(playerId, 5).apply(
                trophyLeaderboardView);

        // then summary should have: top 5 players ordered by position, position data (ordered by position)
        // and millis to current cycle end
        List<TrophyLeaderboardPlayer> expectedPlayers = new ArrayList<TrophyLeaderboardPlayer>
                (leaderboardPlayers.getOrderedByPosition().subList(0, 5));
        // millis to currentCycleEnd
        long millisToCycleEnd = new Duration(new DateTime(), currentCycleEnd).getMillis();
        TrophyLeaderBoardSummary expected = new TrophyLeaderBoardSummary(expectedPlayers,
                Collections.<TrophyLeaderboardPosition>emptyList(),
                leaderboardPlayers.getOrderedByPosition().get(6),
                millisToCycleEnd);
        assertThat(summary, is(expected));
    }

    private Map<Integer, TrophyLeaderboardPosition> createPositions(int numberOfPositions,
                                                                    int numberOfPositionsNoPayout) {
        Map<Integer, TrophyLeaderboardPosition> positions = new HashMap<Integer, TrophyLeaderboardPosition>();
        int positionWithPayout = 0;
        for (; positionWithPayout < numberOfPositions; positionWithPayout++) {
            positions.put(numberOfPositions - positionWithPayout,
                    new TrophyLeaderboardPosition(numberOfPositions - positionWithPayout,
                            (positionWithPayout + 1) * 1000,
                            (positionWithPayout + 1) * 100,
                            BigDecimal.valueOf(positionWithPayout + 1)));
        }
        for (int noPayout = 0; noPayout < numberOfPositionsNoPayout; noPayout++) {
            int position = positionWithPayout + noPayout + 1;
            positions.put(position, new TrophyLeaderboardPosition(position, position, 0, null));
        }
        return positions;
    }

    // creates N players (N..1) where player with id n has position n
    private TrophyLeaderboardPlayers createPlayers(int numberOfPlayers) {
        TrophyLeaderboardPlayers leaderboardPlayers = new TrophyLeaderboardPlayers();
        for (int i = 0; i < numberOfPlayers; i++) {
            TrophyLeaderboardPlayer player = new TrophyLeaderboardPlayer(i + 1, BigDecimal.valueOf(i + 1),
                    "i am " + i, (numberOfPlayers - 1) * 1000, "url " + i);
            leaderboardPlayers.addPlayer(player);
        }
        return leaderboardPlayers;
    }
}
