package com.yazino.platform.processor.tournament;


import com.yazino.platform.model.PagedData;
import com.yazino.platform.model.tournament.RecurringTournamentDefinition;
import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.tournament.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openspaces.core.GigaSpace;
import org.openspaces.remoting.Routing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import com.yazino.game.api.time.SettableTimeSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Random;
import java.util.Set;

import static org.joda.time.DateTimeConstants.*;
import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link com.yazino.platform.processor.tournament.RecurringTournamentPoller} class.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class RecurringTournamentPollerIntegrationTest {

    private static final long THIRTY_MINUTES_MILLIS = 30L * MILLIS_PER_MINUTE;
    private static final long ONE_DAY_MILLIS = 1L * MILLIS_PER_DAY;

    private final DateTime now = new DateTime();
    @Autowired
    private GigaSpace gigaSpace;
    @Autowired
    private GigaSpace globalGigaSpace;

    private RecurringTournamentPoller underTest;

    @Before
    public void setup() throws Exception {
        gigaSpace.clear(null);

        TournamentService tournamentService = new MockTournamentService();
        underTest = new RecurringTournamentPoller(gigaSpace, globalGigaSpace, tournamentService);
        underTest.setTimeSource(new SettableTimeSource(now.getMillis()));
    }

    @Test
    public void shouldNotModifyTheSpaceIfThereAreNoDefinitions() throws Exception {
        gigaSpace.clear(null);
        underTest.poll();
        assertEquals(0, gigaSpace.count(new RecurringTournamentDefinition()));
        assertEquals(0, gigaSpace.count(new Tournament()));
    }

    @Test
    public void shouldNotCreateTournamentsWhenDefinitionIsDisabled() throws Exception {
        RecurringTournamentDefinition definition = new RecurringTournamentDefinition();
        definition.setId(BigInteger.ONE);
        definition.setSignupPeriod((long) MILLIS_PER_MINUTE);
        definition.setEnabled(false);
        definition.setInitialSignupTime(now);
        definition.setFrequency((long) MILLIS_PER_HOUR);
        gigaSpace.write(definition);

        underTest.poll();

        assertEquals(0, gigaSpace.count(new Tournament()));
    }

    @Test
    public void shouldNotCreateTournamentsIfSignupPeriodOutsideLookAheadPeriod() throws Exception {
        RecurringTournamentDefinition definition = new RecurringTournamentDefinition();
        definition.setId(BigInteger.ONE);
        definition.setEnabled(true);
        definition.setInitialSignupTime(now.minusDays(1));
        definition.setFrequency(1L * MILLIS_PER_WEEK);
        definition.setSignupPeriod(10L * MILLIS_PER_MINUTE);
        gigaSpace.write(definition);

        underTest.setLookAheadPeriod(MILLIS_PER_DAY);
        underTest.poll();
        assertEquals(0, gigaSpace.count(new Tournament()));
    }

    @Test
    public void shouldCreateTournamentWithDefinitionStartDateInFuture() throws Exception {
        DateTime signupTime = now.plusHours(23);

        RecurringTournamentDefinition definition = new RecurringTournamentDefinition();
        definition.setId(BigInteger.ONE);
        definition.setInitialSignupTime(signupTime);
        definition.setFrequency(ONE_DAY_MILLIS);
        definition.setSignupPeriod(THIRTY_MINUTES_MILLIS);
        definition.setEnabled(true);

        gigaSpace.write(definition);
        underTest.setLookAheadPeriod(MILLIS_PER_DAY);
        underTest.poll();

        assertEquals(1, gigaSpace.count(new Tournament()));
        Tournament retrieved = gigaSpace.take(new Tournament());

        assertEquals(definition.getTournamentVariationTemplate(), retrieved.getTournamentVariationTemplate());
        assertEquals(signupTime.plus(definition.getSignupPeriod()), retrieved.getStartTimeStamp());
        assertEquals(signupTime.plus(definition.getSignupPeriod()), retrieved.getSignupEndTimeStamp());
        assertEquals(signupTime, retrieved.getSignupStartTimeStamp());
    }

    @Test
    public void shouldCreateTournamentWithDefinitionStartDateInPastButOnlyOneRecurranceInLookAheadPeriod() throws Exception {
        DateTime sixHoursAgo = now.minusHours(6);

        RecurringTournamentDefinition definition = newEnabledDefinition();
        definition.setInitialSignupTime(sixHoursAgo);
        definition.setFrequency(ONE_DAY_MILLIS);
        definition.setSignupPeriod(THIRTY_MINUTES_MILLIS);

        gigaSpace.write(definition);

        underTest.setLookAheadPeriod(ONE_DAY_MILLIS);
        underTest.poll();

        assertEquals(1, gigaSpace.count(new Tournament()));
        Tournament retrieved = gigaSpace.take(new Tournament());

        verifyTournament(retrieved, definition, sixHoursAgo.plus(definition.getFrequency()));
    }

    @Test
    public void shouldCreateTournamentWithDefinitionStartDateInPastWithSeveralRecurrancesInLookAheadPeriod() throws Exception {
        long MON_6_DEC_2010_12_00_PM = 1291636800000L;
        underTest.setTimeSource(new SettableTimeSource(MON_6_DEC_2010_12_00_PM));

        DateTime signupTime = new DateTime(MON_6_DEC_2010_12_00_PM).minusHours(2);

        RecurringTournamentDefinition definition = new RecurringTournamentDefinition();
        definition.setId(BigInteger.ONE);
        definition.setInitialSignupTime(signupTime);
        definition.setFrequency((long) MILLIS_PER_HOUR);
        definition.setSignupPeriod(30 * (long) MILLIS_PER_MINUTE);
        definition.setEnabled(true);
        gigaSpace.write(definition);

        underTest.setLookAheadPeriod(MILLIS_PER_DAY);
        underTest.poll();
        assertEquals(24, gigaSpace.count(new Tournament()));
    }

    @Test
    public void shouldCreateTournamentWithDefinitionStartDateInPastWithSeveralRecurrancesInLookAheadPeriodWithExclusions() throws Exception {

        DateTime now = new DateTime(2010, DateTimeConstants.DECEMBER, 5, 12, 0, 0, 0);
        DateTime signupTime = new DateTime(2010, DateTimeConstants.DECEMBER, 2, 15, 0, 0, 0);

        RecurringTournamentDefinition definition = new RecurringTournamentDefinition();
        definition.setId(BigInteger.ONE);
        definition.setInitialSignupTime(signupTime);
        definition.setFrequency((long) MILLIS_PER_HOUR);
        definition.setSignupPeriod(30 * (long) MILLIS_PER_MINUTE);
        definition.setEnabled(true);
        definition.setExclusionPeriods(new DayPeriod("Sunday@15:00-16:00"));
        gigaSpace.write(definition);

        underTest.setTimeSource(new SettableTimeSource(now.getMillis()));
        underTest.setLookAheadPeriod(5 * MILLIS_PER_HOUR);

        underTest.poll();
        assertEquals(4, gigaSpace.count(new Tournament()));

    }

    @Test
    public void shouldNotCreateTournamentIfAlreadyExistsInSpace() throws Exception {
        DateTime twentyThreeHoursFromNow = now.plusHours(23);
        long thirtyMinutes = THIRTY_MINUTES_MILLIS;
        long oneDay = ONE_DAY_MILLIS;

        RecurringTournamentDefinition definition = newEnabledDefinition();
        definition.setInitialSignupTime(twentyThreeHoursFromNow);
        definition.setFrequency(oneDay);
        definition.setSignupPeriod(thirtyMinutes);

        gigaSpace.write(definition);
        underTest.setLookAheadPeriod(oneDay);
        underTest.poll();
        assertEquals(1, gigaSpace.count(new Tournament()));
        underTest.poll();
        assertEquals(1, gigaSpace.count(new Tournament()));
    }

    private static RecurringTournamentDefinition newEnabledDefinition() {
        RecurringTournamentDefinition definition = new RecurringTournamentDefinition();
        definition.setId(BigInteger.ONE);
        definition.setEnabled(true);
        return definition;
    }

    private static void verifyTournament(Tournament actual, RecurringTournamentDefinition definition, DateTime expectedSignupStartTime) {
        DateTime expectedSignupEndTime = expectedSignupStartTime.plus(definition.getSignupPeriod());

        assertEquals(definition.getTournamentVariationTemplate(), actual.getTournamentVariationTemplate());
        assertEquals(expectedSignupStartTime, actual.getSignupStartTimeStamp());
        assertEquals(expectedSignupEndTime, actual.getSignupEndTimeStamp());
        assertEquals(expectedSignupEndTime, actual.getStartTimeStamp());
        assertEquals(definition.getTournamentName(), actual.getName());
        assertEquals(definition.getTournamentDescription(), actual.getDescription());
    }


    private class MockTournamentService implements TournamentService {

        @Override
        public BigDecimal createTournament(TournamentDefinition tournamentDefinition) throws TournamentException {
            final Tournament tournament = new Tournament(tournamentDefinition);
            tournament.setTournamentId(BigDecimal.valueOf(new Random().nextInt()));
            gigaSpace.write(tournament);
            return tournament.getTournamentId();
        }


        @Override
        public PagedData<TournamentMonitorView> findAll(final int page) {
            return null;
        }

        @Override
        public PagedData<TournamentMonitorView> findByStatus(TournamentStatus status, final int page) {
            return null;
        }

        @Override
        public boolean cancelTournament(BigDecimal toCancel) {
            return true;
        }

        @Override
        public void populateSpaceWithNonClosedTournaments() {

        }

        @Override
        public void clearSpace() {

        }

        @Override
        public void saveRecurringTournamentDefinition(RecurringTournament definition) {

        }

        @Override
        public TournamentView findViewById(BigDecimal tournamentId) {
            return null;
        }

        @Override
        public TournamentDetail findDetailById(final BigDecimal tournamentId) {
            throw new UnsupportedOperationException("Unimplemented");
        }

        @Override
        public Summary findLastTournamentSummary(final String gameType) {
            throw new UnsupportedOperationException("Unimplemented");
        }

        @Override
        public Schedule getTournamentSchedule(final String gameType) {
            throw new UnsupportedOperationException("Unimplemented");
        }

        @Override
        public TournamentOperationResult register(@Routing final BigDecimal tournamentId, final BigDecimal playerId, final boolean async) {
            return null;
        }

        @Override
        public TournamentOperationResult deregister(@Routing final BigDecimal tournamentId, final BigDecimal playerId, final boolean async) {
            return null;
        }

        @Override
        public Set<BigDecimal> findTableIdsFor(@Routing final BigDecimal tournamentId) {
            return null;
        }
    }
}
