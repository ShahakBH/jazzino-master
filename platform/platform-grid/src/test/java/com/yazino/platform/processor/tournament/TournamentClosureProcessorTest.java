package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.model.tournament.TournamentEventRequest;
import com.yazino.platform.repository.tournament.TournamentRepository;
import com.yazino.platform.service.tournament.TournamentTableService;
import com.yazino.platform.tournament.TournamentStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openspaces.core.GigaSpace;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class TournamentClosureProcessorTest {
    private static final BigDecimal TOURNAMENT_ID = BigDecimal.valueOf(4554L);
    private static final BigDecimal TABLE1_ID = BigDecimal.valueOf(10001L);
    private static final BigDecimal TABLE2_ID = BigDecimal.valueOf(10002L);

    @Mock
    private TournamentTableService tournamentTableService;
    @Mock
    private TournamentRepository tournamentRepository;
    @Mock
    private GigaSpace gigaSpace;

    private TournamentClosureProcessor unit;
    private Tournament tournament;

    @Before
    public void setUp() {
        tournament = new Tournament(TOURNAMENT_ID);
        tournament.setStartingTables(Arrays.asList(TABLE1_ID, TABLE2_ID));

        unit = new TournamentClosureProcessor(tournamentTableService, tournamentRepository, gigaSpace);
    }

    @Test
    public void templateSwipesClosedTournaments() {
        assertEquals(TournamentStatus.CLOSED, unit.eventTemplate().getTournamentStatus());
    }

    @Test
    public void processShouldRemoveTournamentFromSpace() {
        unit.process(tournament);

        verify(gigaSpace).takeMultiple(new TournamentEventRequest(TOURNAMENT_ID), Integer.MAX_VALUE);
        verify(tournamentRepository).remove(tournament);
    }


    @Test
    public void processShouldUnloadTables() {
        unit.process(tournament);

        verify(tournamentTableService).removeTables(Arrays.asList(TABLE1_ID, TABLE2_ID));
    }
}
