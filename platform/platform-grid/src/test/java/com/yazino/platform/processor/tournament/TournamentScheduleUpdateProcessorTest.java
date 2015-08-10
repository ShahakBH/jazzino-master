package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.TournamentSchedule;
import com.yazino.platform.model.tournament.TournamentScheduleUpdateRequest;
import com.yazino.platform.repository.tournament.TournamentScheduleRepository;
import com.yazino.platform.tournament.TournamentRegistrationInfo;
import com.yazino.platform.tournament.TournamentStatus;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Collections;

import static com.yazino.platform.Partner.YAZINO;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TournamentScheduleUpdateProcessorTest {
    private static final BigDecimal TOURNAMENT_ID = BigDecimal.valueOf(2342);

    @Mock
    private TournamentScheduleRepository tournamentScheduleRepository;
    @Mock
    private TournamentSchedule tournamentSchedule;

    private TournamentScheduleUpdateProcessor underTest;

    @Before
    public void setUp() {
        when(tournamentScheduleRepository.findByGameType("aGameType")).thenReturn(tournamentSchedule);

        underTest = new TournamentScheduleUpdateProcessor(tournamentScheduleRepository);
    }

    @Test(expected = NullPointerException.class)
    public void processorCannotBeCreatedWithANullRepository() {
        new TournamentScheduleUpdateProcessor(null);
    }

    @Test
    public void theProcessorTemplateIsABlankUpdateRequest() {
        assertThat(underTest.template(), is(equalTo(new TournamentScheduleUpdateRequest())));
    }

    @Test
    public void aNullRequestIsIgnored() {
        underTest.process(null);

        verifyZeroInteractions(tournamentScheduleRepository);
    }

    @Test
    public void aRequestForAGameTypeWithNoScheduleResultsInANewScheduleBeingSaved() {
        reset(tournamentScheduleRepository);

        underTest.process(aRequestWith(TournamentStatus.REGISTERING));

        final TournamentSchedule expectedSchedule = aSchedule();
        expectedSchedule.addRegistrationInfo(aRegistrationInfo());
        verify(tournamentScheduleRepository).save(expectedSchedule);
    }

    @Test
    public void aRequestForARegisteringTournamentUpdatesTheSchedule() {
        underTest.process(aRequestWith(TournamentStatus.REGISTERING));

        verify(tournamentSchedule).addRegistrationInfo(aRegistrationInfo());
        verify(tournamentScheduleRepository).save(tournamentSchedule);
    }

    @Test
    public void aRequestForARunningTournamentUpdatesTheScheduleAsInProgress() {
        underTest.process(aRequestWith(TournamentStatus.RUNNING));

        verify(tournamentSchedule).addInProgressTournament(aRegistrationInfo());
        verify(tournamentScheduleRepository).save(tournamentSchedule);
    }

    @Test
    public void aRequestForAWaitingForClientsTournamentUpdatesTheScheduleAsInProgress() {
        underTest.process(aRequestWith(TournamentStatus.RUNNING));

        verify(tournamentSchedule).addInProgressTournament(aRegistrationInfo());
        verify(tournamentScheduleRepository).save(tournamentSchedule);
    }

    @Test
    public void aRequestForAOnBreakTournamentUpdatesTheScheduleAsInProgress() {
        underTest.process(aRequestWith(TournamentStatus.ON_BREAK));

        verify(tournamentSchedule).addInProgressTournament(aRegistrationInfo());
        verify(tournamentScheduleRepository).save(tournamentSchedule);
    }

    @Test
    public void aRequestForACancelledTournamentRemovesItFromTheSchedule() {
        underTest.process(aRequestWith(TournamentStatus.CANCELLED));

        verify(tournamentSchedule).removeRegistrationInfo(TOURNAMENT_ID);
        verify(tournamentScheduleRepository).save(tournamentSchedule);
    }

    @Test
    public void aRequestForAFinishedTournamentRemovesItFromTheSchedule() {
        underTest.process(aRequestWith(TournamentStatus.FINISHED));

        verify(tournamentSchedule).removeRegistrationInfo(TOURNAMENT_ID);
        verify(tournamentScheduleRepository).save(tournamentSchedule);
    }

    @Test
    public void aRequestForAErrorTournamentRemovesItFromTheSchedule() {
        underTest.process(aRequestWith(TournamentStatus.ERROR));

        verify(tournamentSchedule).removeRegistrationInfo(TOURNAMENT_ID);
        verify(tournamentScheduleRepository).save(tournamentSchedule);
    }

    private TournamentScheduleUpdateRequest aRequestWith(final TournamentStatus status) {
        return new TournamentScheduleUpdateRequest("aGameType", "aPartner", aRegistrationInfo(), status);
    }

    private TournamentRegistrationInfo aRegistrationInfo() {
        return new TournamentRegistrationInfo(TOURNAMENT_ID, new DateTime(10000000000L), BigDecimal.ONE,
                BigDecimal.ONE, "aName", "aDescription", "aVariation", Collections.<BigDecimal>emptySet());
    }

    private TournamentSchedule aSchedule() {
        final TournamentSchedule schedule = new TournamentSchedule();
        schedule.setGameType("aGameType");
        return schedule;
    }

}
