package com.yazino.platform.model.tournament;

import com.yazino.platform.model.SerializationTestHelper;
import com.yazino.platform.tournament.TournamentRegistrationInfo;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

/**
 * Tests the {@link com.yazino.platform.model.tournament.TournamentSchedule} class.
 */
public class TournamentScheduleTest {

    private static final DateTime NOW = new DateTime();
    private static final int TOURNAMENT_REMOVAL_TIME_OUT = 10;
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(10234);

    private final TournamentSchedule schedule = new TournamentSchedule();
    private final TournamentRegistrationInfo regInfo1 = createTournamentRegistrationInfo(BigDecimal.valueOf(1), NOW, BigDecimal.ZERO);
    private final TournamentRegistrationInfo regInfo2 = createTournamentRegistrationInfo(BigDecimal.valueOf(2), NOW.plusMillis(TOURNAMENT_REMOVAL_TIME_OUT), BigDecimal.ZERO);
    private final TournamentRegistrationInfo regInfo3 = createTournamentRegistrationInfo(BigDecimal.valueOf(3), NOW, BigDecimal.ZERO);

    @Before
    public void setUp() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(NOW.minusMinutes(1).getMillis());

        schedule.setTournamentRemovalTimeOut(TOURNAMENT_REMOVAL_TIME_OUT);
    }

    @After
    public void resetJodaTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void shouldOrderTournamentsBasedOnStartTime() throws Exception {
        schedule.addRegistrationInfo(regInfo1);
        schedule.addRegistrationInfo(regInfo2);
        Set<TournamentRegistrationInfo> infos = schedule.getChronologicalTournaments();
        Iterator<TournamentRegistrationInfo> iterator = infos.iterator();
        Assert.assertEquals(regInfo1, iterator.next());
        Assert.assertEquals(regInfo2, iterator.next());
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void shouldContainTournamentsStartingAtSameTime() throws Exception {
        schedule.addRegistrationInfo(regInfo1);
        schedule.addRegistrationInfo(regInfo3);
        Set<TournamentRegistrationInfo> infos = schedule.getChronologicalTournaments();
        Iterator<TournamentRegistrationInfo> iterator = infos.iterator();
        Assert.assertEquals(regInfo1, iterator.next());
        Assert.assertEquals(regInfo3, iterator.next());
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void shouldRemoveTournamentInfoByTournamentId() throws Exception {
        schedule.addRegistrationInfo(regInfo1);
        schedule.addRegistrationInfo(regInfo2);
        schedule.removeRegistrationInfo(regInfo1.getTournamentId());
        Set<TournamentRegistrationInfo> infos = schedule.getChronologicalTournaments();
        Iterator<TournamentRegistrationInfo> iterator = infos.iterator();
        Assert.assertEquals(regInfo2, iterator.next());
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void shouldBeSerializable() throws Exception {
        TournamentSchedule underTest = new TournamentSchedule();
        underTest.setGameType("foo");
        TournamentRegistrationInfo registrationInfo = createTournamentRegistrationInfo(BigDecimal.ONE, new DateTime(), BigDecimal.ONE);
        underTest.addRegistrationInfo(registrationInfo);
        SerializationTestHelper.testSerializationRoundTrip(underTest);
    }


    @Test
    public void shouldRemovePastTournaments() throws Exception {
        final TournamentRegistrationInfo pastTournament = createTournamentRegistrationInfo(BigDecimal.valueOf(2), NOW.plusMillis(1000), BigDecimal.ZERO);
        schedule.addRegistrationInfo(regInfo1);
        schedule.addRegistrationInfo(pastTournament);
        Assert.assertEquals(2, schedule.getChronologicalTournaments().size());
        final long time = NOW.plusMillis(TOURNAMENT_REMOVAL_TIME_OUT).getMillis();
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(time);
        Assert.assertEquals(2, schedule.getChronologicalTournaments().size());
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(time + 1);
        Set<TournamentRegistrationInfo> infos = schedule.getChronologicalTournaments();
        Iterator<TournamentRegistrationInfo> iterator = infos.iterator();
        Assert.assertEquals(pastTournament, iterator.next());
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void shouldFilterTournamentsInProgress() throws Exception {
        schedule.addInProgressTournament(regInfo1);
        schedule.addRegistrationInfo(regInfo2);
        Assert.assertEquals(1, schedule.getChronologicalTournaments().size());
    }

    @Test
    public void theScheduleTournamentsShouldMatchTheBeansTournaments() throws Exception {
        schedule.addInProgressTournament(regInfo1);
        schedule.addRegistrationInfo(regInfo2);

        Assert.assertEquals(schedule.getChronologicalTournaments(),
                schedule.asSchedule().getChronologicalTournaments());
    }

    @Test
    public void shouldKeepTournamentsInProgressForPlayer() throws Exception {
        final TournamentRegistrationInfo tournamentWithPlayer = createTournamentRegistrationInfoWithOnePlayer(BigDecimal.valueOf(1), NOW, BigDecimal.ZERO);
        schedule.addInProgressTournament(tournamentWithPlayer);
        schedule.addRegistrationInfo(regInfo2);
        Assert.assertEquals(2, schedule.getChronologicalTournaments(PLAYER_ID).size());
    }

    @Test
    public void theScheduleTournamentsForPlayerShouldMatchTheBeansTournamentsForPlayer() throws Exception {
        final TournamentRegistrationInfo tournamentWithPlayer = createTournamentRegistrationInfoWithOnePlayer(BigDecimal.valueOf(1), NOW, BigDecimal.ZERO);
        schedule.addInProgressTournament(tournamentWithPlayer);
        schedule.addRegistrationInfo(regInfo2);

        Assert.assertEquals(schedule.getChronologicalTournaments(PLAYER_ID),
                schedule.asSchedule().getChronologicalTournamentsForPlayer(PLAYER_ID));
    }

    private static TournamentRegistrationInfo createTournamentRegistrationInfo(BigDecimal id,
                                                                               DateTime startTime,
                                                                               BigDecimal entryFee) {
        return new TournamentRegistrationInfo(id, startTime, entryFee, BigDecimal.ZERO,
                "aName", "aDescription", "aVariationTemplateName", Collections.<BigDecimal>emptySet());
    }

    private static TournamentRegistrationInfo createTournamentRegistrationInfoWithOnePlayer(BigDecimal id,
                                                                               DateTime startTime,
                                                                               BigDecimal entryFee) {
        return new TournamentRegistrationInfo(id, startTime, entryFee, BigDecimal.ZERO,
                "aName", "aDescription", "aVariationTemplateName", newHashSet(PLAYER_ID));
    }
}
