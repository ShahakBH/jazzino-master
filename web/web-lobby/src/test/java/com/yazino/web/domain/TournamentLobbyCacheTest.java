package com.yazino.web.domain;

import com.yazino.platform.Partner;
import com.yazino.platform.tournament.*;
import com.yazino.test.ThreadLocalDateTimeUtils;
import com.yazino.web.util.PlayerFriendsCache;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.*;

import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newTreeSet;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TournamentLobbyCacheTest {
    private static final String BLACKJACK = "BLACKJACK";
    private static final int MAX_TO_DISPLAY = 20;
    private static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    private static final Partner PARTNER = Partner.YAZINO;

    private TrophyLeaderboardView leaderBoard;
    private TournamentService tournamentService;
    private TrophyLeaderboardService trophyLeaderboardService;

    private TournamentLobbyCache underTest;

    @Before
    public void setUp() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime().getMillis());

        leaderBoard = new TrophyLeaderboardView(BigDecimal.valueOf(100),
                "aName",
                true,
                "aGameType",
                0L,
                new DateTime().minus(100),
                new DateTime().plus(100),
                new DateTime().plus(100),
                new Duration(1000),
                new HashMap<Integer, TrophyLeaderboardPosition>(),
                new TrophyLeaderboardPlayers());

        SiteConfiguration siteConfiguration = mock(SiteConfiguration.class);
        when(siteConfiguration.getPartnerId()).thenReturn(PARTNER);
        tournamentService = mock(TournamentService.class);
        trophyLeaderboardService = mock(TrophyLeaderboardService.class);
        PlayerFriendsCache playerFriendsCache = mock(PlayerFriendsCache.class);
        underTest = new TournamentLobbyCache(siteConfiguration, playerFriendsCache, tournamentService,
                trophyLeaderboardService);
    }

    @After
    public void resetJodaTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void shouldReturnTop20InLeaderBoard() throws Exception {
        createPlayers(MAX_TO_DISPLAY + 10);
        when(trophyLeaderboardService.findByGameType(BLACKJACK)).thenReturn(leaderBoard);
        List<TrophyLeaderboardPlayer> result = underTest.getTrophyLeaderBoard(BLACKJACK, MAX_TO_DISPLAY);
        assertEquals(leaderBoard.getOrderedByPosition().subList(0, MAX_TO_DISPLAY), result);
    }

    @Test
    public void shouldReturnNumberOfPlayersInList() throws Exception {
        createPlayers(MAX_TO_DISPLAY - 10);
        when(trophyLeaderboardService.findByGameType(BLACKJACK)).thenReturn(leaderBoard);
        List<TrophyLeaderboardPlayer> result = underTest.getTrophyLeaderBoard(BLACKJACK, MAX_TO_DISPLAY);
        assertEquals(leaderBoard.getOrderedByPosition(), result);
    }

    @Test
    public void shouldHandleUnavailableTrophyLeaderboard() throws Exception {
        createPlayers(MAX_TO_DISPLAY + 10);
        when(trophyLeaderboardService.findByGameType(BLACKJACK)).thenReturn(null);
        TrophyLeaderboardPlayer result = underTest.getTrophyLeaderBoardPlayer(BLACKJACK, PLAYER_ID);
        assertNull(result);
    }

    private void createPlayers(int numberOfPlayers) {
        for (int i = 0; i < numberOfPlayers; i++) {
            leaderBoard.getPlayers()
                    .addPlayer(new TrophyLeaderboardPlayer(BigDecimal.valueOf(i), "player" + i, "picture" + i));
        }
    }

    @Test
    public void shouldReturnFriendsInList() throws Exception {
        createPlayers(MAX_TO_DISPLAY - 10);
        Set<BigDecimal> friendIds = new HashSet<>(
                asList(BigDecimal.valueOf(0), BigDecimal.valueOf(10)));
        when(trophyLeaderboardService.findByGameType(BLACKJACK)).thenReturn(leaderBoard);
        List<TrophyLeaderboardPlayer> result = underTest.getTrophyLeaderBoardFriends(BLACKJACK, friendIds,
                MAX_TO_DISPLAY);
        assertEquals(leaderBoard.getOrderedByPosition().subList(0, 1), result);
    }

    @Test
    public void getNextTournamentsShouldLoopOverGameTypesAndCallGetNextTournament() {

        Schedule blackjackSchedule = new Schedule(new TreeSet<TournamentRegistrationInfo>(),
                Collections.<BigDecimal>emptySet());
        Schedule texasSchedule = new Schedule(new TreeSet<TournamentRegistrationInfo>(),
                Collections.<BigDecimal>emptySet());

        when(tournamentService.getTournamentSchedule("TEXAS_HOLDEM")).thenReturn(texasSchedule);
        when(tournamentService.getTournamentSchedule("BLACKJACK")).thenReturn(blackjackSchedule);


        Map<String, TournamentDetail> result = underTest.getNextTournaments(PLAYER_ID, "TEXAS_HOLDEM", "BLACKJACK");

        assertEquals(2, result.keySet().size());

        Set<String> keys = new HashSet<>();
        keys.add("TEXAS_HOLDEM");
        keys.add("BLACKJACK");
        assertEquals(keys, result.keySet());
    }

    @Test
    public void getNextTournamentHandlesNoScheduleAvailable() {
        when(tournamentService.getTournamentSchedule("TEXAS_HOLDEM")).thenReturn(null);
        when(tournamentService.getTournamentSchedule("BLACKJACK")).thenReturn(null);

        Map<String, TournamentDetail> result = underTest.getNextTournaments(PLAYER_ID, "TEXAS_HOLDEM", "BLACKJACK");

        Set<String> keys = new HashSet<>();
        keys.add("TEXAS_HOLDEM");
        keys.add("BLACKJACK");
        assertEquals(2, result.keySet().size());
        assertEquals(keys, result.keySet());
        for (String key : keys) {
            assertNull(result.get(key));
        }
    }

    @Test
    public void getNextTournamentsForEachVariationShouldLoopOverGameTypesAndReturnTheNextTournamentForEachUniqueVariation() {
        final Schedule schedule = new Schedule(newTreeSet(asList(aTournamentWithVariationStartingIn("variation1", 100),
                aTournamentWithVariationStartingIn("variation1", 200),
                aTournamentWithVariationStartingIn("variation2", 300),
                aTournamentWithVariationStartingIn("variation1", 400),
                aTournamentWithVariationStartingIn("variation3", 400))),
                Collections.<BigDecimal>emptySet());
        when(tournamentService.getTournamentSchedule("aGameType")).thenReturn(schedule);

        final Map<String, UpcomingTournamentSummary> result = underTest.getNextTournamentsForEachVariation(PLAYER_ID, "aGameType");

        assertThat(result.size(), is(equalTo(3)));
        assertThat(result.get("variation1"), is(equalTo(summaryOf(aTournamentWithVariationStartingIn("variation1", 100)))));
        assertThat(result.get("variation2"), is(equalTo(summaryOf(aTournamentWithVariationStartingIn("variation2", 300)))));
        assertThat(result.get("variation3"), is(equalTo(summaryOf(aTournamentWithVariationStartingIn("variation3", 400)))));
    }

    @Test
    public void getNextTournamentsForEachVariationShouldConsiderInProgressTournamentsWhereRegisteredAsMostRecent() {
        final Schedule schedule = new Schedule(newTreeSet(asList(
                aTournamentWithVariationStartingIn("variation1", -200),
                aTournamentWithVariationStartingIn("variation1", -100, newHashSet(PLAYER_ID)),
                aTournamentWithVariationStartingIn("variation1", 200),
                aTournamentWithVariationStartingIn("variation2", 300))),
                newHashSet(BigDecimal.valueOf("variation1".hashCode())));
        when(tournamentService.getTournamentSchedule("aGameType")).thenReturn(schedule);

        final Map<String, UpcomingTournamentSummary> result = underTest.getNextTournamentsForEachVariation(PLAYER_ID, "aGameType");

        assertThat(result.size(), is(equalTo(2)));
        assertThat(result.get("variation1"), is(equalTo(summaryOf(aTournamentWithVariationStartingIn("variation1", -100, newHashSet(PLAYER_ID))))));
        assertThat(result.get("variation2"), is(equalTo(summaryOf(aTournamentWithVariationStartingIn("variation2", 300)))));
    }

    @Test
    public void getNextTournamentsForEachVariationShouldReturnAnEmptyMapForAnEmptySchedule() {
        when(tournamentService.getTournamentSchedule("BLACKJACK")).thenReturn(
                new Schedule(new TreeSet<TournamentRegistrationInfo>(), new HashSet<BigDecimal>()));

        final Map<String, UpcomingTournamentSummary> result = underTest.getNextTournamentsForEachVariation(PLAYER_ID, "BLACKJACK");

        assertThat(result.isEmpty(), is(true));
    }

    @Test
    public void shouldReturnEmptyTrophyLeaderBoardSummaryWhenNoViewForGameType() {
        when(trophyLeaderboardService.findByGameType(BLACKJACK)).thenReturn(null);

        final TrophyLeaderBoardSummary trophyTrophyLeaderSummary =
                underTest.getTrophyLeaderBoardSummary(PLAYER_ID, BLACKJACK, MAX_TO_DISPLAY);

        TrophyLeaderBoardSummary expected = new TrophyLeaderBoardSummary(
                Collections.<TrophyLeaderboardPlayer>emptyList(),
                Collections.<TrophyLeaderboardPosition>emptyList(),
                null,
                0);

        assertThat(trophyTrophyLeaderSummary, TrophyLeaderBoardSummaryMatcher.is(expected));
    }

    @Test
    public void shouldReturnLeaderBoardView() {
        createPlayers(MAX_TO_DISPLAY + 10);
        leaderBoard.getPositionData().put(1, new TrophyLeaderboardPosition(1, 1000, 1000000));
        leaderBoard.getPositionData().put(2, new TrophyLeaderboardPosition(2, 100, 10000));
        leaderBoard.getPositionData().put(3, new TrophyLeaderboardPosition(3, 10, 1000));
        when(trophyLeaderboardService.findByGameType(BLACKJACK)).thenReturn(leaderBoard);

        final TrophyLeaderBoardSummary trophyTrophyLeaderSummary =
                underTest.getTrophyLeaderBoardSummary(BigDecimal.valueOf(MAX_TO_DISPLAY), BLACKJACK, MAX_TO_DISPLAY);

        TrophyLeaderBoardSummary expected = new TrophyLeaderBoardSummary(
                leaderBoard.getOrderedByPosition().subList(0, MAX_TO_DISPLAY),
                new ArrayList<>(leaderBoard.getPositionData().values()),
                leaderBoard.getOrderedByPosition().get(MAX_TO_DISPLAY), // not null since not in top 20
                new Duration(new DateTime(), leaderBoard.getCurrentCycleEnd()).getMillis());

        assertThat(trophyTrophyLeaderSummary, TrophyLeaderBoardSummaryMatcher.is(expected));
    }

    @Test
    public void shouldReturnLeaderBoardViewWithFriendsAndPlayerOnly() {
        createPlayers(MAX_TO_DISPLAY + 10);
        leaderBoard.getPositionData().put(1, new TrophyLeaderboardPosition(1, 1000, 1000000));
        leaderBoard.getPositionData().put(2, new TrophyLeaderboardPosition(2, 100, 10000));
        leaderBoard.getPositionData().put(3, new TrophyLeaderboardPosition(3, 10, 1000));
        when(trophyLeaderboardService.findByGameType(BLACKJACK)).thenReturn(leaderBoard);

        final Set<BigDecimal> friendIds = new HashSet<>();
        friendIds.add(BigDecimal.ZERO);
        friendIds.add(BigDecimal.TEN);
        final TrophyLeaderBoardSummary trophyTrophyLeaderSummary = underTest.getTrophyLeaderBoardSummaryForFriends(
                BigDecimal.valueOf(-1), BLACKJACK, friendIds, MAX_TO_DISPLAY);

        final List<TrophyLeaderboardPlayer> orderedByPosition = leaderBoard.getOrderedByPosition();
        TrophyLeaderBoardSummary expected = new TrophyLeaderBoardSummary(
                asList(orderedByPosition.get(0), orderedByPosition.get(10)),
                new ArrayList<>(leaderBoard.getPositionData().values()),
                null,
                new Duration(new DateTime(), leaderBoard.getCurrentCycleEnd()).getMillis());

        assertThat(trophyTrophyLeaderSummary, TrophyLeaderBoardSummaryMatcher.is(expected));
    }

    private UpcomingTournamentSummary summaryOf(final TournamentRegistrationInfo tournament) {
        return new UpcomingTournamentSummary(PLAYER_ID, Collections.<BigDecimal>emptySet(), tournament);
    }

    private TournamentRegistrationInfo aTournamentWithVariationStartingIn(final String variationName,
                                                                          final int startIn) {
        return aTournamentWithVariationStartingIn(variationName, startIn, Collections.<BigDecimal>emptySet());
    }

    private TournamentRegistrationInfo aTournamentWithVariationStartingIn(final String variationName,
                                                                          final int startIn,
                                                                          final Set<BigDecimal> players) {
        return new TournamentRegistrationInfo(BigDecimal.valueOf(variationName.hashCode()), new DateTime().plusMillis(startIn),
                BigDecimal.ZERO, BigDecimal.ZERO, "aName", "aDescription", variationName, players);
    }
}
