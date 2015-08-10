package com.yazino.platform.model.tournament;

import com.yazino.game.api.NewsEvent;
import com.yazino.game.api.ParameterisedMessage;
import com.yazino.game.api.time.SettableTimeSource;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.Trophy;
import com.yazino.platform.model.SerializationTestHelper;
import com.yazino.platform.model.session.InboxMessage;
import com.yazino.platform.processor.tournament.TrophyLeaderboardPayoutCalculator;
import com.yazino.platform.processor.tournament.TrophyLeaderboardResultContext;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.community.TrophyRepository;
import com.yazino.platform.repository.session.InboxMessageRepository;
import com.yazino.platform.repository.tournament.TrophyLeaderboardResultRepository;
import com.yazino.platform.service.account.InternalWalletService;
import com.yazino.platform.service.audit.AuditLabelFactory;
import com.yazino.platform.service.tournament.AwardTrophyService;
import com.yazino.platform.tournament.TrophyLeaderboardPlayer;
import com.yazino.platform.tournament.TrophyLeaderboardPlayerResult;
import com.yazino.platform.tournament.TrophyLeaderboardPosition;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.joda.time.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class TrophyLeaderboardTest {
    private static final long POINT_BONUS_PER_PLAYER = 1;
    private static final Duration LEADERBOARD_PERIOD = new Period(Days.days(3)).toStandardDuration();
    private static final DateTime START_TIME = new DateTime(2009, 1, 23, 12, 6, 0, 0);
    private static final DateTime END_TIME = new DateTime().plus(LEADERBOARD_PERIOD).plus(LEADERBOARD_PERIOD);
    private static final String LEADERBOARD_NAME = "Test Leaderboard";
    private static final BigDecimal LEADERBOARD_ID = BigDecimal.valueOf(17);
    private static final String GAME_TYPE = "BLACKJACK";
    private static final BigDecimal TOURNAMENT_ID = BigDecimal.valueOf(100000);

    private final TrophyLeaderboardResultRepository trophyLeaderboardResultRepository = mock(TrophyLeaderboardResultRepository.class);
    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final InternalWalletService internalWalletService = mock(InternalWalletService.class);
    private final AwardTrophyService awardTrophyService = mock(AwardTrophyService.class);
    private final TrophyRepository trophyRepository = mock(TrophyRepository.class);
    private final InboxMessageRepository inboxMessageRepository = mock(InboxMessageRepository.class);
    private final AuditLabelFactory auditor = mock(AuditLabelFactory.class);
    private final TrophyLeaderboardPayoutCalculator calculator = mock(TrophyLeaderboardPayoutCalculator.class);

    private SettableTimeSource timeSource;
    private List<TrophyLeaderboardPlayerResult> leaderboardPlayerResults;

    private TrophyLeaderboard underTest;

    @Before
    public void setUp() throws WalletServiceException {
        underTest = new TrophyLeaderboard(LEADERBOARD_ID, LEADERBOARD_NAME, GAME_TYPE, new Interval(START_TIME, END_TIME), LEADERBOARD_PERIOD);
        underTest.setPointBonusPerPlayer(POINT_BONUS_PER_PLAYER);
        ReflectionTestUtils.setField(underTest, "trophyLeaderboardPayoutCalculator", calculator);

        final int[] positionPoints = new int[] {25, 18, 15, 12, 10, 8, 6, 4, 2, 1};
        for (int position = 1; position <= positionPoints.length; ++position) {
            final TrophyLeaderboardPosition leaderboardPosition = new TrophyLeaderboardPosition(
                    position, positionPoints[position - 1], payoutForPosition(position, positionPoints.length));
            underTest.addPosition(leaderboardPosition);
        }

        leaderboardPlayerResults = new ArrayList<>();
        leaderboardPlayerResults.add(new TrophyLeaderboardPlayerResult(playerId(20), "aName20",
                tournamentPointsForPosition(1, 4) + bonusPointsForPosition(1, 4), BigDecimal.ZERO, 1));
        leaderboardPlayerResults.add(new TrophyLeaderboardPlayerResult(playerId(40), "aName40",
                tournamentPointsForPosition(2, 4) + bonusPointsForPosition(2, 4), BigDecimal.ZERO, 2));
        leaderboardPlayerResults.add(new TrophyLeaderboardPlayerResult(playerId(30), "aName30",
                tournamentPointsForPosition(3, 4) + bonusPointsForPosition(3, 4), BigDecimal.ZERO, 3));
        leaderboardPlayerResults.add(new TrophyLeaderboardPlayerResult(playerId(10), "aName10",
                tournamentPointsForPosition(4, 4) + bonusPointsForPosition(4, 4), BigDecimal.ZERO, 4));

        timeSource = new SettableTimeSource();
    }

    @Test
    public void leaderboardCreationSetsNextCurrentCycleEndTime() {
        assertThat(underTest.getCurrentCycleEnd(), is(equalTo(START_TIME.plus(LEADERBOARD_PERIOD))));
    }

    @Test
    public void shouldBeSerializable() throws Exception {
        SerializationTestHelper.testSerializationRoundTrip(underTest);
    }

    @Test
    public void leaderboardCreationSetsActiveToTrue() {
        assertThat(underTest.getActive(), is(true));
    }

    @Test
    public void leaderboardCreationValidatesThatEndDateIsAtLeastOnePeriodAfterStartDate() {
        new TrophyLeaderboard(LEADERBOARD_ID, LEADERBOARD_NAME, GAME_TYPE,
                new Interval(START_TIME, START_TIME.plus(LEADERBOARD_PERIOD)), LEADERBOARD_PERIOD);
    }

    @Test(expected = IllegalArgumentException.class)
    public void leaderboardCreationFailsIfEndDateIsLessThanOnePeriodAfterStartDate() {
        new TrophyLeaderboard(LEADERBOARD_ID, LEADERBOARD_NAME, GAME_TYPE,
                new Interval(START_TIME, START_TIME.plus(LEADERBOARD_PERIOD).minus(1)), LEADERBOARD_PERIOD);
    }

    @Test
    public void leaderboardUpdateAddsANewPlayerToTheLeaderboard() {
        underTest.update(anUpdateRequestFor(10, 4, 4));
        underTest.update(anUpdateRequestFor(20, 1, 4));

        assertThat(underTest, containsPlayerWithPoints(playerId(10), tournamentPointsForPosition(4, 4) + bonusPointsForPosition(4, 4)));
        assertThat(underTest, containsPlayerWithPoints(playerId(20), tournamentPointsForPosition(1, 4) + bonusPointsForPosition(1, 4)));
    }

    @Test
    public void leaderboardUpdateReturnsAnUpdateResultForANewPlayer() {
        final TrophyLeaderboardPlayerUpdateResult result = underTest.update(anUpdateRequestFor(10, 4, 4));

        assertThat(result, is(equalTo(new TrophyLeaderboardPlayerUpdateResult(LEADERBOARD_ID, TOURNAMENT_ID,
                0, 1, 0, tournamentPointsForPosition(4, 4), bonusPointsForPosition(4, 4)))));
    }

    @Test
    public void leaderboardUpdateReturnsAnUpdateResultForAnExistingPlayer() {
        underTest.update(anUpdateRequestFor(10, 4, 4));
        underTest.update(anUpdateRequestFor(20, 1, 4));

        final TrophyLeaderboardPlayerUpdateResult result = underTest.update(anUpdateRequestFor(10, 3, 6));

        assertThat(result, is(equalTo(new TrophyLeaderboardPlayerUpdateResult(LEADERBOARD_ID, TOURNAMENT_ID,
                2, 1, tournamentPointsForPosition(4, 4) + bonusPointsForPosition(4, 4),  tournamentPointsForPosition(3, 6), bonusPointsForPosition(3, 6)))));
    }

    @Test
    public void leaderboardUpdateIncrementsPointForExistingPlayersInLeaderboard() {
        underTest.update(anUpdateRequestFor(10, 4, 4));

        underTest.update(anUpdateRequestFor(10, 1, 6));

        assertThat(underTest, containsPlayerWithPoints(playerId(10),
                tournamentPointsForPosition(4, 4) + bonusPointsForPosition(4, 4) + tournamentPointsForPosition(1, 6) + bonusPointsForPosition(1, 6)));
    }

    @Test
    public void leaderboardUpdateOrdersItemsCorrectly() {
        prepareStartingLeaderboardState();

        assertThat(underTest, containsPlayerWithPointsAtPosition(playerId(10), tournamentPointsForPosition(4, 4) + bonusPointsForPosition(4, 4), 4));
        assertThat(underTest, containsPlayerWithPointsAtPosition(playerId(20), tournamentPointsForPosition(1, 4) + bonusPointsForPosition(1, 4), 1));
        assertThat(underTest, containsPlayerWithPointsAtPosition(playerId(30), tournamentPointsForPosition(3, 4) + bonusPointsForPosition(3, 4), 3));
        assertThat(underTest, containsPlayerWithPointsAtPosition(playerId(40), tournamentPointsForPosition(2, 4) + bonusPointsForPosition(2, 4), 2));
    }

    @Test
    public void leaderboardUpdateReordersItemsCorrectlyAfterUpdate() {
        prepareStartingLeaderboardState();

        underTest.update(anUpdateRequestFor(10, 4, 4));
        underTest.update(anUpdateRequestFor(20, 1, 4));
        underTest.update(anUpdateRequestFor(30, 2, 4));
        underTest.update(anUpdateRequestFor(40, 3, 4));

        assertThat(underTest, containsPlayerWithPointsAtPosition(
                playerId(10), 2 * tournamentPointsForPosition(4, 4) + 2 * bonusPointsForPosition(4, 4), 4));
        assertThat(underTest, containsPlayerWithPointsAtPosition(
                playerId(30), tournamentPointsForPosition(3, 4) + tournamentPointsForPosition(2, 4) + bonusPointsForPosition(3, 4) + bonusPointsForPosition(2, 4), 2));
        assertThat(underTest, containsPlayerWithPointsAtPosition(
                playerId(40), tournamentPointsForPosition(2, 4) + tournamentPointsForPosition(3, 4) + bonusPointsForPosition(2, 4) + bonusPointsForPosition(3, 4), 2));
        assertThat(underTest, containsPlayerWithPointsAtPosition(
                playerId(20), 2 * tournamentPointsForPosition(1, 4) + 2 * bonusPointsForPosition(1, 4), 1));
    }

    @Test
    public void resultingSavesCorrectResultObjectToRepository() throws WalletServiceException {
        prepareStartingLeaderboardState();

        timeSource.setMillis(START_TIME.plus(LEADERBOARD_PERIOD).plus(1).getMillis());
        final DateTime now = new DateTime(timeSource.getCurrentTimeStamp());
        final TrophyLeaderboardResult leaderboardResult = new TrophyLeaderboardResult(
                LEADERBOARD_ID, now, now.plus(underTest.getCycle()), leaderboardPlayerResults);

        underTest.result(aResultContext());

        verify(trophyLeaderboardResultRepository).save(leaderboardResult);
    }

    @Test
    public void resultingResetsLeaderboardAndIncrementsNextEndDateIfLeaderboardIsValidForAnotherCycle() throws WalletServiceException {
        prepareStartingLeaderboardState();

        timeSource.setMillis(START_TIME.plus(LEADERBOARD_PERIOD).plus(1).getMillis());

        underTest.result(aResultContext());

        assertThat(underTest.getOrderedByPosition(), is(empty()));
        assertThat(underTest.getActive(), is(true));

        final DateTime endTimeForSecondPeriod = START_TIME.plus(LEADERBOARD_PERIOD).plus(LEADERBOARD_PERIOD);
        assertThat(underTest.getCurrentCycleEnd(), is(equalTo(endTimeForSecondPeriod)));
    }

    @Test
    public void resultingResetsLeaderboardAndSetsCurrentCycleEndToNullAndSuspendsIfNoMorePeriodsAreRequired() throws WalletServiceException {
        prepareStartingLeaderboardState();

        timeSource.setMillis(END_TIME.plus(1).getMillis());

        underTest.result(aResultContext());

        assertThat(underTest.getOrderedByPosition(), is(empty()));
        assertThat(underTest.getCurrentCycleEnd(), is(nullValue()));
        assertThat(underTest.getActive(), is(false));
    }

    @Test
    public void ensurePlayerIsAwardedTrophy() throws Exception {
        prepareStartingLeaderboardState();

        TrophyLeaderboardPosition position1 = new TrophyLeaderboardPosition(1, 500, 200, BigDecimal.valueOf(9));
        underTest.getPositionData().put(1, position1);
        timeSource.setMillis(DateTimeUtils.currentTimeMillis());

        underTest.result(aResultContext());

        verify(awardTrophyService).awardTrophy(BigDecimal.valueOf(20), BigDecimal.valueOf(9));
    }

    @Test
    public void ensurePlayersAreAwardedCorrectTrophiesForPosition() throws Exception {
        prepareStartingLeaderboardState();

        TrophyLeaderboardPosition position1 = new TrophyLeaderboardPosition(1, 500, 200, BigDecimal.valueOf(9));
        underTest.getPositionData().put(1, position1);
        TrophyLeaderboardPosition position4 = new TrophyLeaderboardPosition(4, 1000, 300, BigDecimal.valueOf(6));
        underTest.getPositionData().put(4, position4);

        timeSource.setMillis(DateTimeUtils.currentTimeMillis());

        underTest.result(aResultContext());

        verify(awardTrophyService).awardTrophy(BigDecimal.valueOf(20), BigDecimal.valueOf(9));
        verify(awardTrophyService).awardTrophy(BigDecimal.valueOf(10), BigDecimal.valueOf(6));
    }

    @Test
    public void ensureSomePlayersAreAwardedCorrectTrophiesForPosition() throws Exception {
        prepareStartingLeaderboardState();

        TrophyLeaderboardPosition position1 = new TrophyLeaderboardPosition(1, 500, 200, BigDecimal.valueOf(9));
        underTest.getPositionData().put(1, position1);
        TrophyLeaderboardPosition position2 = new TrophyLeaderboardPosition(2, 600, 150);
        underTest.getPositionData().put(2, position2);
        TrophyLeaderboardPosition position4 = new TrophyLeaderboardPosition(4, 1000, 300, BigDecimal.valueOf(6));
        underTest.getPositionData().put(4, position4);

        timeSource.setMillis(DateTimeUtils.currentTimeMillis());

        underTest.result(aResultContext());

        verify(awardTrophyService).awardTrophy(BigDecimal.valueOf(20), BigDecimal.valueOf(9));
        verify(awardTrophyService).awardTrophy(BigDecimal.valueOf(10), BigDecimal.valueOf(6));
        verifyNoMoreInteractions(awardTrophyService);
    }

    @Test
    public void ensureNoTrophyGivenWhenNoTrophyAssociatedWithLeaderboardPosition() throws Exception {
        prepareStartingLeaderboardState();

        underTest.result(aResultContext());

        verifyZeroInteractions(awardTrophyService);
    }

    @Test
    public void ensureNotSavingEmptyResult() throws Exception {
        underTest.result(aResultContext());
        verifyZeroInteractions(trophyLeaderboardResultRepository);
    }

    @Test
    public void ensurePlayerIsNotifiedOfTrophyAward() throws Exception {
        prepareStartingLeaderboardState();
        TrophyLeaderboardPosition position1 = new TrophyLeaderboardPosition(1, 500, 200, BigDecimal.TEN);
        underTest.getPositionData().put(1, position1);

        Trophy trophy = new Trophy(BigDecimal.TEN, "name", "gameType", "image");
        trophy.setMessage("message");
        trophy.setShortDescription("shortDescription");
        when(trophyRepository.findById(trophy.getId())).thenReturn(trophy);
        underTest.result(aResultContext());
        ArgumentCaptor<InboxMessage> messageCaptor = ArgumentCaptor.forClass(InboxMessage.class);
        verify(inboxMessageRepository).send(messageCaptor.capture());
        NewsEvent newsEvent = messageCaptor.getValue().getNewsEvent();
        assertEquals(trophy.getMessage(), newsEvent.getNews().getMessage());
        assertEquals(trophy.getShortDescription(), newsEvent.getShortDescription().getMessage());
    }

    @Test
    public void ensurePlayerNotificationHasPlayerNameInParameterisedMessage() throws Exception {
        prepareStartingLeaderboardState();
        TrophyLeaderboardPosition position1 = new TrophyLeaderboardPosition(1, 500, 200, BigDecimal.TEN);
        underTest.getPositionData().put(1, position1);

        Trophy trophy = new Trophy(BigDecimal.TEN, "name", "gameType", "image");
        trophy.setMessage("message");
        trophy.setShortDescription("shortDescription");
        when(trophyRepository.findById(trophy.getId())).thenReturn(trophy);
        underTest.result(aResultContext());
        ArgumentCaptor<InboxMessage> messageCaptor = ArgumentCaptor.forClass(InboxMessage.class);
        verify(inboxMessageRepository).send(messageCaptor.capture());
        NewsEvent newsEvent = messageCaptor.getValue().getNewsEvent();
        ParameterisedMessage news = newsEvent.getNews();
        ParameterisedMessage shortDescription = newsEvent.getShortDescription();
        assertEquals("aName20", news.getParameters()[0]);
        assertEquals("aName20", shortDescription.getParameters()[0]);
    }

    private void prepareStartingLeaderboardState() {
        underTest.update(anUpdateRequestFor(10, 4, 4));
        underTest.update(anUpdateRequestFor(20, 1, 4));
        underTest.update(anUpdateRequestFor(30, 3, 4));
        underTest.update(anUpdateRequestFor(40, 2, 4));
    }

    private TrophyLeaderboardResultContext aResultContext() {
        return new TrophyLeaderboardResultContext(trophyLeaderboardResultRepository, internalWalletService, playerRepository,
                awardTrophyService, inboxMessageRepository, trophyRepository, auditor, timeSource);
    }

    private TrophyLeaderboardPlayerUpdateRequest anUpdateRequestFor(final int playerId, final int position, final int tournamentPlayers) {
        return new TrophyLeaderboardPlayerUpdateRequest(LEADERBOARD_ID, TOURNAMENT_ID, BigDecimal.valueOf(playerId),
                "aName" + playerId, "aPicture" + playerId, position, tournamentPlayers);
    }

    private long bonusPointsForPosition(final int position, final int players) {
        final int[] positionPoints = new int[] {25, 18, 15, 12, 10, 8, 6, 4, 2, 1};

        if (position <= positionPoints.length) {
            final int playersToNearestHundred = ((players + 99) / 100) * 100;
            return positionPoints[position - 1] * (playersToNearestHundred / 100);
        }
        return 0;
    }

    private long tournamentPointsForPosition(final int position,
                                             final int players) {
        return players - position;
    }

    private long payoutForPosition(final int position,
                                   final int maxPosition) {
        return (maxPosition - (position - 1)) * 1000;
    }

    private Matcher<TrophyLeaderboard> containsPlayerWithPoints(final BigDecimal playerId,
                                                                final long points) {
        return new TypeSafeMatcher<TrophyLeaderboard>() {
            @Override
            public void describeTo(final Description description) {
                description.appendText("contains player ")
                        .appendValue(playerId)
                        .appendText(" with points ")
                        .appendValue(points);
            }

            @Override
            public boolean matchesSafely(final TrophyLeaderboard item) {
                final List<TrophyLeaderboardPlayer> players = item.getOrderedByPosition();
                if (players != null) {
                    for (final TrophyLeaderboardPlayer player : players) {
                        if (player.getPlayerId().equals(playerId) && player.getPoints() == points) {
                            return true;
                        }
                    }
                }

                return false;
            }
        };
    }

    private Matcher<TrophyLeaderboard> containsPlayerWithPointsAtPosition(final BigDecimal playerId,
                                                                          final long points,
                                                                          final int position) {
        return new TypeSafeMatcher<TrophyLeaderboard>() {
            @Override
            public void describeTo(final Description description) {
                description.appendText("contains player ")
                        .appendValue(playerId)
                        .appendText(" with points ")
                        .appendValue(points)
                        .appendText(" at position ")
                        .appendValue(position);
            }

            @Override
            public boolean matchesSafely(final TrophyLeaderboard item) {
                final List<TrophyLeaderboardPlayer> players = item.getOrderedByPosition();
                if (players != null) {
                    for (TrophyLeaderboardPlayer potentialMatch : players) {
                        if (potentialMatch.getPlayerId().equals(playerId) && potentialMatch.getPoints() == points && potentialMatch.getLeaderboardPosition() == position) {
                            return true;
                        }
                    }
                }

                return false;
            }
        };
    }

    private Matcher<Collection> empty() {
        return new TypeSafeMatcher<Collection>() {
            @Override
            public boolean matchesSafely(final Collection item) {
                return item.isEmpty();
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("is empty");
            }
        };
    }

    private BigDecimal playerId(final int val) {
        return BigDecimal.valueOf(val);
    }

}
