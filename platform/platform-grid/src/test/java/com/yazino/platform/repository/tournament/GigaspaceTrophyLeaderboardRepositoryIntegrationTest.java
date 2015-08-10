package com.yazino.platform.repository.tournament;

import com.yazino.platform.grid.Routing;
import com.yazino.platform.model.tournament.TrophyLeaderboard;
import com.yazino.platform.model.tournament.TrophyLeaderboardPersistenceRequest;
import com.yazino.platform.model.tournament.TrophyLeaderboardPlayerUpdateRequest;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.joda.time.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;
import com.yazino.game.api.time.SettableTimeSource;

import java.math.BigDecimal;
import java.util.ConcurrentModificationException;
import java.util.Set;

import static com.yazino.platform.model.tournament.TrophyLeaderboardPersistenceRequest.Operation;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
@Transactional
public class GigaspaceTrophyLeaderboardRepositoryIntegrationTest {
    private static final BigDecimal LEADERBOARD_ID = BigDecimal.valueOf(4534);
    private static final DateTime START_TIME = new DateTime(2010, 1, 1, 1, 1, 1, 0);
    private static final DateTime END_TIME = new DateTime(2010, 10, 1, 1, 1, 1, 0);
    private static final Duration PERIOD = new Period(Days.days(3)).toStandardDuration();
    private static final String GAME_TYPE = "aGameType";

    @Autowired
    private GigaSpace gigaSpace;
    @Autowired
    private Routing routing;

    private GigaspaceTrophyLeaderboardRepository underTest;
    private TrophyLeaderboard trophyLeaderboard;
    private SettableTimeSource timeSource;

    @Before
    public void setUp() {
        gigaSpace.clear(null);

        trophyLeaderboard = new TrophyLeaderboard(LEADERBOARD_ID, "aLeaderboardName", GAME_TYPE,
                new Interval(START_TIME, END_TIME), PERIOD);

        timeSource = new SettableTimeSource();
        timeSource.setMillis(END_TIME.minusDays(10).getMillis());

        underTest = new GigaspaceTrophyLeaderboardRepository(gigaSpace, gigaSpace, routing);
    }

    @Test
    public void findResultingRequiredMatchesActiveObjectsWithCurrenctCycleEndEitherCurrentOrInThePast() {
        final TrophyLeaderboard activeAndResultingRequired = new TrophyLeaderboard(
                BigDecimal.valueOf(1), "anActiveResultingLeaderboard", GAME_TYPE, new Interval(START_TIME, END_TIME), PERIOD);
        activeAndResultingRequired.setCurrentCycleEnd(new DateTime(timeSource.getCurrentTimeStamp() - 10));

        final TrophyLeaderboard inactiveAndResultingRequired = new TrophyLeaderboard(
                BigDecimal.valueOf(2), "anInactiveResultingLeaderboard", GAME_TYPE, new Interval(START_TIME, END_TIME), PERIOD);
        inactiveAndResultingRequired.setActive(false);
        inactiveAndResultingRequired.setCurrentCycleEnd(new DateTime(timeSource.getCurrentTimeStamp() - 10));

        final TrophyLeaderboard activeAndResultingNotRequired = new TrophyLeaderboard(
                BigDecimal.valueOf(3), "anActiveNotResultingLeaderboard", GAME_TYPE, new Interval(START_TIME, END_TIME), PERIOD);
        activeAndResultingNotRequired.setCurrentCycleEnd(new DateTime(timeSource.getCurrentTimeStamp() + 10));

        underTest.save(activeAndResultingRequired);
        underTest.save(inactiveAndResultingRequired);
        underTest.save(activeAndResultingNotRequired);

        final Set<TrophyLeaderboard> leaderboards = underTest.findLocalResultingRequired(timeSource);
        assertThat(leaderboards, is(not(nullValue())));

        assertThat(leaderboards.size(), is(equalTo(1)));
        assertThat(leaderboards, hasItem(activeAndResultingRequired));
    }

    @Test
    public void findCurrentWithGameTypeReturnsActiveObjectsWithinDateInterval() {
        final TrophyLeaderboard activeAndMatching = new TrophyLeaderboard(
                BigDecimal.valueOf(1), "anActiveMatchingLeaderboard", GAME_TYPE, new Interval(START_TIME, END_TIME), PERIOD);
        final TrophyLeaderboard activeAndNotMatchingGameType = new TrophyLeaderboard(
                BigDecimal.valueOf(2), "anActiveMatchingGameTypeLeaderboard", "anotherGameType", new Interval(START_TIME, END_TIME), PERIOD);
        final TrophyLeaderboard inactiveAndMatching = new TrophyLeaderboard(
                BigDecimal.valueOf(3), "anActiveMatchingLeaderboard", GAME_TYPE, new Interval(START_TIME, END_TIME), PERIOD);
        inactiveAndMatching.setActive(false);
        final TrophyLeaderboard inactiveAndNotMatchingInterval = new TrophyLeaderboard(
                BigDecimal.valueOf(4), "anInactiveNotMatchingIntervalLeaderboard", GAME_TYPE, new Interval(START_TIME, START_TIME.plus(PERIOD)), PERIOD);
        inactiveAndNotMatchingInterval.setActive(false);

        gigaSpace.write(activeAndMatching);
        gigaSpace.write(activeAndNotMatchingGameType);
        gigaSpace.write(inactiveAndMatching);
        gigaSpace.write(inactiveAndNotMatchingInterval);

        final Set<TrophyLeaderboard> leaderboards = underTest.findCurrentAndActiveWithGameType(
                new DateTime(timeSource.getCurrentTimeStamp()), GAME_TYPE);
        assertThat(leaderboards, is(not(nullValue())));

        assertThat(leaderboards.size(), is(equalTo(1)));
        assertThat(leaderboards, hasItem(activeAndMatching));
    }

    @Test
    public void saveAddsObjectToGigaSpaceAndGeneratesPersistenceRequest() {
        underTest.save(trophyLeaderboard);

        assertThat(trophyLeaderboard, isPresentInGigaSpace());

        final TrophyLeaderboardPersistenceRequest expectedPersistenceRequest
                = new TrophyLeaderboardPersistenceRequest(trophyLeaderboard.getId(), Operation.SAVE);
        assertThat(expectedPersistenceRequest, isPresentInGigaSpace());
    }

    @Test
    public void archiveWritesObjectToGigaSpaceAndGeneratesPersistenceRequest() {
        underTest.archive(trophyLeaderboard);

        assertThat(trophyLeaderboard, isPresentInGigaSpace());

        final TrophyLeaderboardPersistenceRequest expectedPersistenceRequest
                = new TrophyLeaderboardPersistenceRequest(LEADERBOARD_ID, Operation.ARCHIVE);
        assertThat(expectedPersistenceRequest, isPresentInGigaSpace());
    }

    @Test
    public void clearRemovesObjectFromSpace() {
        gigaSpace.write(trophyLeaderboard);

        underTest.clear(LEADERBOARD_ID);

        assertThat(trophyLeaderboard, isNotPresentInGigaSpace());
    }

    @Test
    public void findByIdMatchesObjectById() {
        gigaSpace.write(trophyLeaderboard);

        final TrophyLeaderboard queriedObject = underTest.findById(LEADERBOARD_ID);

        assertThat(queriedObject, is(equalTo(trophyLeaderboard)));
    }

    @Test
    public void findByIdReturnsNullIfObjectIsNotPresentInSpace() {
        final TrophyLeaderboard queriedObject = underTest.findById(LEADERBOARD_ID);

        assertThat(queriedObject, is(nullValue()));
    }

    @Test
    public void lockMatchesObjectById() {
        gigaSpace.write(trophyLeaderboard);

        final TrophyLeaderboard queriedObject = underTest.lock(LEADERBOARD_ID);

        assertThat(queriedObject, is(equalTo(trophyLeaderboard)));
    }

    @Test(expected = ConcurrentModificationException.class)
    public void lockThrowsExceptionIfObjectIsLockedOrNotPresentInSpace() {
        underTest.lock(LEADERBOARD_ID);
    }

    @Test
    public void shouldReturnTrophyLeaderBoardMatchingGameType() throws Exception {
        TrophyLeaderboard expected = new TrophyLeaderboard();
        expected.setId(BigDecimal.TEN);
        expected.setGameType("BLACKJACK");
        TrophyLeaderboard other = new TrophyLeaderboard();
        other.setId(BigDecimal.ONE);
        other.setGameType("foo");
        gigaSpace.writeMultiple(new TrophyLeaderboard[]{expected, other});
        assertEquals(expected, underTest.findByGameType("BLACKJACK"));
    }

    @Test
    public void shouldReturnTemplateLeaderBoardIfNoneFound() throws Exception {
        TrophyLeaderboard expected = new TrophyLeaderboard();
        expected.setGameType("BLACKJACK");
        assertEquals(expected, underTest.findByGameType("BLACKJACK"));
    }

    @Test
    public void requestingAnUpdateForAPlayerWritesARequestToTheSpace() {
        underTest.requestUpdate(LEADERBOARD_ID, BigDecimal.valueOf(123), BigDecimal.valueOf(1000), "aName", "aPicture", 22, 99);

        final TrophyLeaderboardPlayerUpdateRequest request = gigaSpace.readById(TrophyLeaderboardPlayerUpdateRequest.class, LEADERBOARD_ID + "-123-1000");
        assertThat(request, is(equalTo(new TrophyLeaderboardPlayerUpdateRequest(LEADERBOARD_ID, BigDecimal.valueOf(123),
                BigDecimal.valueOf(1000), "aName", "aPicture", 22, 99))));
    }

    @SuppressWarnings({"unchecked"})
    private <T> Matcher<T> isPresentInGigaSpace() {
        return new BaseMatcher() {
            @Override
            public boolean matches(final Object item) {
                return gigaSpace.read(item) != null;
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("is in GigaSpace");
            }
        };
    }

    @SuppressWarnings({"unchecked"})
    private <T> Matcher<T> isNotPresentInGigaSpace() {
        return new BaseMatcher() {
            @Override
            public boolean matches(final Object item) {
                return gigaSpace.read(item) == null;
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("is not in GigaSpace");
            }
        };
    }

}
