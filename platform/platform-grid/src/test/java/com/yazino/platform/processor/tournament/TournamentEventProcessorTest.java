package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.model.tournament.TournamentEventRequest;
import com.yazino.platform.repository.tournament.TournamentRepository;
import com.yazino.platform.tournament.TournamentStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openspaces.core.GigaSpace;

import java.math.BigDecimal;

import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class TournamentEventProcessorTest {

    @Mock
    private TournamentHost tournamentHost;
    @Mock
    private GigaSpace gigaSpace;
    @Mock
    private TournamentRepository tournamentRepository;
    @Mock
    private Tournament tournament;

    private TournamentEventProcessor underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(tournamentHost.getTournamentRepository()).thenReturn(tournamentRepository);

        underTest = new TournamentEventProcessor(tournamentHost, gigaSpace);
    }

    @Test
    public void templateShouldMatchAllRequest() {
        final TournamentEventRequest template = underTest.eventTemplate();

        assertNull(template.getSpaceId());
        assertNull(template.getTournamentId());
    }

    @Test
    public void processLocksProcessesAndSavesTournament() {
        final BigDecimal tournamentId = BigDecimal.valueOf(17L);

        when(gigaSpace.takeMultiple(eq(new TournamentEventRequest(tournamentId)), eq(Integer.MAX_VALUE))).thenReturn(null);
        when(tournamentRepository.findById(eq(tournamentId))).thenReturn(tournament);
        when(tournamentRepository.lock(eq(tournamentId))).thenReturn(tournament);
        when(tournament.calculateShouldSendWarningOfImpendingStart(tournamentHost)).thenReturn(false);

        final TournamentEventRequest request = new TournamentEventRequest(tournamentId);

        underTest.process(request);

        verify(tournamentRepository).save(eq(tournament), anyBoolean());
        verify(tournament).processEvent(tournamentHost);
    }

    @Test
    public void processSwallowsInvalidTournaments() {
        final BigDecimal tournamentId = BigDecimal.valueOf(17L);
        when(tournamentRepository.findById(tournamentId)).thenReturn(null);
        final TournamentEventRequest request = new TournamentEventRequest(tournamentId);

        underTest.process(request);

        verify(tournamentRepository).findById(tournamentId);
        verifyZeroInteractions(tournamentRepository);
    }

    @Test
    public void processSwallowsDomainErrors() {
        final BigDecimal tournamentId = BigDecimal.valueOf(17L);

        doThrow(new RuntimeException("Oh my eye")).when(tournament).processEvent(eq(tournamentHost));
        when(tournament.calculateShouldSendWarningOfImpendingStart(tournamentHost)).thenReturn(false);
        when(gigaSpace.takeMultiple(eq(new TournamentEventRequest(tournamentId)), eq(Integer.MAX_VALUE))).thenReturn(null);
        when(tournamentRepository.findById(eq(tournamentId))).thenReturn(tournament);
        when(tournamentRepository.lock(eq(tournamentId))).thenReturn(tournament);
        final TournamentEventRequest request = new TournamentEventRequest(tournamentId);

        underTest.process(request);

        verify(tournament).setTournamentStatus(TournamentStatus.ERROR);
        verify(tournament).setMonitoringMessage(startsWith("Error: java.lang.RuntimeException: Oh my eye"));
        verify(tournament).setNextEvent(null);
        verify(tournamentRepository).save(eq(tournament), anyBoolean());
    }

}
