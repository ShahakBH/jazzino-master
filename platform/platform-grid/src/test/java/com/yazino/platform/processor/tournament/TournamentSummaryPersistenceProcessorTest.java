package com.yazino.platform.processor.tournament;

import com.yazino.platform.event.message.TournamentSummaryEvent;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.model.tournament.TournamentSummary;
import com.yazino.platform.model.tournament.TournamentSummaryPersistenceRequest;
import com.yazino.platform.persistence.tournament.TournamentSummaryDao;
import com.yazino.platform.repository.tournament.TournamentSummaryRepository;
import com.yazino.platform.tournament.TournamentPlayerSummary;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openspaces.core.GigaSpace;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

public class TournamentSummaryPersistenceProcessorTest {

    private static final BigDecimal TOURNAMENT_ID = BigDecimal.valueOf(4524);
    private static final Date START_DATE_TIME = new Date();
    private static final Date FINISH_DATE_TIME = new Date();
    private static final TournamentSummaryPersistenceRequest[] EMPTY_LIST = new TournamentSummaryPersistenceRequest[0];
    private static final String GAME_TYPE = "aGameType";
    private static final String TOURNAMENT_NAME = "aName";
    private static final List<TournamentPlayerSummary> PLAYERS = Collections.emptyList();

    @Mock
    private TournamentSummaryRepository tournamentSummaryRepository;
    @Mock
    private TournamentSummaryDao tournamentSummaryDao;
    @Mock
    private GigaSpace tournamentGigaSpace;
    @Mock
    private QueuePublishingService<TournamentSummaryEvent> eventService;

    private TournamentSummaryPersistenceRequest request;
    private TournamentSummary tournamentSummary;

    private TournamentSummaryPersistenceProcessor underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        underTest = new TournamentSummaryPersistenceProcessor(
                tournamentSummaryRepository, tournamentSummaryDao, tournamentGigaSpace, eventService);

        request = new TournamentSummaryPersistenceRequest(TOURNAMENT_ID);
        tournamentSummary = new TournamentSummary();
        tournamentSummary.setTournamentId(TOURNAMENT_ID);
        tournamentSummary.setVariationId(BigDecimal.ONE);
        tournamentSummary.setTournamentName(TOURNAMENT_NAME);
        tournamentSummary.setStartDateTime(START_DATE_TIME);
        tournamentSummary.setFinishDateTime(FINISH_DATE_TIME);
        tournamentSummary.setGameType(GAME_TYPE);
        tournamentSummary.setPlayers(PLAYERS);
    }

    @Test
    public void successPathShouldPersistTournamentSummaryandReturnNull() {
        when(tournamentSummaryRepository.findByTournamentId(TOURNAMENT_ID)).thenReturn(tournamentSummary);
        expectRemovalOfMatchingRequests(request);

        assertNull(underTest.processRequest(request));

        verify(tournamentSummaryDao).save(tournamentSummary);
    }

    @Test
    public void shouldSwallowExceptionOnPersistenceAndReturnRequestInErrorState() {
        when(tournamentSummaryRepository.findByTournamentId(request.getTournamentId())).thenReturn(tournamentSummary);
        doThrow(new IllegalStateException("test")).when(tournamentSummaryDao).save(eq(tournamentSummary));

        assertEquals(getCopyOfRequestInErrorState(request), underTest.processRequest(request));

        verify(tournamentSummaryDao).save(eq(tournamentSummary));
    }

    @Test
    public void shouldRemoveDuplicatesAndReturnRequestInErrorStateIftournamentSummaryNotFound() {
        when(tournamentSummaryRepository.findByTournamentId(request.getTournamentId())).thenReturn(null);
        expectRemovalOfMatchingRequests(request);

        assertEquals(getCopyOfRequestInErrorState(request), underTest.processRequest(request));
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionOnNullRequest() {
        underTest.processRequest(null);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionOnUninitialisedObject() {
        underTest = new TournamentSummaryPersistenceProcessor();
        final TournamentSummaryPersistenceRequest request = new TournamentSummaryPersistenceRequest(TOURNAMENT_ID);

        underTest.processRequest(request);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionOnNullTournamentSummary() {
        final TournamentSummaryPersistenceRequest request = new TournamentSummaryPersistenceRequest();

        underTest.processRequest(request);
    }

    @Test
    public void templateShouldBeEmptyObjectWithPendingStatus() {
        final TournamentSummaryPersistenceRequest template = underTest.template();

        assertNull(template.getTournamentId());
        assertNull(template.getTournamentId());
        assertNull(template.getSpaceId());
        assertEquals(TournamentSummaryPersistenceRequest.STATUS_PENDING, template.getStatus());
    }

    private void expectRemovalOfMatchingRequests(TournamentSummaryPersistenceRequest request) {
        TournamentSummaryPersistenceRequest template = getCopyOfRequest(request);
        when(tournamentGigaSpace.takeMultiple(template, Integer.MAX_VALUE)).thenReturn(EMPTY_LIST);
        TournamentSummaryPersistenceRequest errtemplate = getCopyOfRequestInErrorState(request);
        when(tournamentGigaSpace.takeMultiple(errtemplate, Integer.MAX_VALUE)).thenReturn(EMPTY_LIST);
    }

    private TournamentSummaryPersistenceRequest getCopyOfRequestInErrorState(TournamentSummaryPersistenceRequest request) {
        final TournamentSummaryPersistenceRequest result = getCopyOfRequest(request);
        result.setStatus(TournamentSummaryPersistenceRequest.STATUS_ERROR);
        return result;
    }

    private TournamentSummaryPersistenceRequest getCopyOfRequest(TournamentSummaryPersistenceRequest request) {
        return new TournamentSummaryPersistenceRequest(request.getTournamentId());
    }

}
