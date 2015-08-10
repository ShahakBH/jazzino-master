package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.model.tournament.TournamentScheduleUpdateRequest;
import com.yazino.platform.tournament.*;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openspaces.core.GigaSpace;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TournamentScheduleChangeProcessorTest {

    private static final BigDecimal TOURNAMENT_ID = BigDecimal.valueOf(324);
    @Mock
    private GigaSpace localGigaSpace;
    @Mock
    private GigaSpace globalGigaSpace;

    private TournamentScheduleChangeProcessor underTest;

    @Before
    public void setUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(1000000);

        underTest = new TournamentScheduleChangeProcessor(localGigaSpace, globalGigaSpace);
    }

    @After
    public void resetTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test(expected = NullPointerException.class)
    public void processorCannotBeCreatedWithANullLocalSpace() {
        new TournamentScheduleChangeProcessor(null, globalGigaSpace);
    }

    @Test(expected = NullPointerException.class)
    public void processorCannotBeCreatedWithANullGlobalSpace() {
        new TournamentScheduleChangeProcessor(localGigaSpace, null);
    }

    @Test
    public void aTournamentChangeWritesAnUpdateRequestToTheGlobalSpace() {
        underTest.processTournamentChange(aTournamentWithId(TOURNAMENT_ID));

        verify(globalGigaSpace).write(aRequestFor(aTournamentWithId(TOURNAMENT_ID)));
    }

    @Test
    public void requestAreWrittenForAllTournamentsInTheLocalSpaceOnSpringInit() throws Exception {
        when(localGigaSpace.readMultiple(new Tournament(), Integer.MAX_VALUE))
                .thenReturn(new Tournament[]{aTournamentWithId(TOURNAMENT_ID), aTournamentWithId(BigDecimal.TEN)});

        underTest.initialiseTournamentSchedule();

        verify(globalGigaSpace).write(aRequestFor(aTournamentWithId(TOURNAMENT_ID)));
        verify(globalGigaSpace).write(aRequestFor(aTournamentWithId(BigDecimal.TEN)));
    }

    private TournamentScheduleUpdateRequest aRequestFor(final Tournament tournament) {
        final TournamentRegistrationInfo registrationInfo = new TournamentRegistrationInfo(tournament.getTournamentId(),
                tournament.getStartTimeStamp(), tournament.getTournamentVariationTemplate().getEntryFee(), BigDecimal.ZERO,
                "aName", "aDescription", "aTemplateName", Collections.<BigDecimal>emptySet());
        return new TournamentScheduleUpdateRequest(tournament.getTournamentVariationTemplate().getGameType(),
                tournament.getPartnerId(),
                registrationInfo,
                tournament.getTournamentStatus());
    }

    private Tournament aTournamentWithId(final BigDecimal tournamentId) {
        final Tournament tournament = new Tournament();
        tournament.setTournamentId(tournamentId);
        tournament.setStartTimeStamp(new DateTime());
        tournament.setName("aName");
        tournament.setDescription("aDescription");
        tournament.setTournamentVariationTemplate(new TournamentVariationTemplate(
                BigDecimal.ONE, TournamentType.PRESET, "aTemplateName", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.TEN, 1, 10, "aGameType", 0, "anAllocator",
                Collections.<TournamentVariationPayout>emptyList(), Collections.<TournamentVariationRound>emptyList()));
        tournament.setPartnerId("aPartner");
        tournament.setTournamentStatus(TournamentStatus.RUNNING);
        return tournament;
    }

}
