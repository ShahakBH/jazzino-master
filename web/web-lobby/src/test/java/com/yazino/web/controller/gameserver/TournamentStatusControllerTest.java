package com.yazino.web.controller.gameserver;


import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.tournament.TournamentRankView;
import com.yazino.platform.tournament.TournamentRoundView;
import com.yazino.platform.tournament.TournamentView;
import com.yazino.platform.tournament.TournamentViewDetails;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.domain.TournamentDocumentRequest;
import com.yazino.web.service.TournamentViewDocumentWorker;
import com.yazino.web.data.TournamentViewRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static com.yazino.web.controller.gameserver.TournamentStatusController.DEFAULT_PAGE_NUMBER;
import static com.yazino.web.controller.gameserver.TournamentStatusController.DEFAULT_PAGE_SIZE;
import static com.yazino.web.service.TournamentViewDocumentWorker.DocumentType.*;

public class TournamentStatusControllerTest {
    private String REQUEST_TOURNAMENT_ID = "17";
    private BigDecimal TOURNAMENT_ID = BigDecimal.valueOf(17L);
    private BigDecimal PLAYER_ID = BigDecimal.valueOf(19L);

    @Mock
    private LobbySessionCache lobbySessionCache;
    @Mock
    private TournamentViewDocumentWorker tournamentViewDocumentWorker;
    @Mock
    private TournamentViewRepository tournamentViewRepository;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private TournamentView tournamentView;

    private TournamentStatusController underTest;

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        final LobbySession lobbySession = new LobbySession(BigDecimal.valueOf(3141592), PLAYER_ID, "Dummy", "0001", Partner.YAZINO, null, "email",
                null, false, Platform.WEB, AuthProvider.YAZINO);
        when(lobbySessionCache.getActiveSession(request)).thenReturn(lobbySession);

        tournamentView = new TournamentView(new TournamentViewDetails.Builder().build(),
                new HashMap<BigDecimal, TournamentRankView>(),
                new ArrayList<TournamentRankView>(),
                new ArrayList<TournamentRoundView>(),
                new Date().getTime());
        when(tournamentViewRepository.getTournamentView(TOURNAMENT_ID)).thenReturn(tournamentView);

        underTest = new TournamentStatusController(lobbySessionCache, tournamentViewDocumentWorker,
                tournamentViewRepository);
    }

    @Test
    public void shouldSendErrorWithStatusOkWhenNoLobbySession() throws Exception {
        // given no active lobby session
        when(lobbySessionCache.getActiveSession(request)).thenReturn(null);

        // when requesting the tournament status
        underTest.tournamentStatus(request, response, REQUEST_TOURNAMENT_ID, TOURNAMENT_OVERVIEW, 0, 12);

        // then the response should have session expired error message
        assertThat(response.getErrorMessage(), is("ERROR|Session Expired"));
        assertThat(response.getStatus(), is(HttpStatus.OK.value()));
    }

    @Test
    public void shouldSendErrorWithStatusOkWhenTournamentNotFound() throws Exception {
        // given no tournament
        when(tournamentViewRepository.getTournamentView(TOURNAMENT_ID)).thenReturn(null);

        // when requesting the tournament status
        underTest.tournamentStatus(request, response, REQUEST_TOURNAMENT_ID, TOURNAMENT_OVERVIEW, 0, 100);

        // then the response should have tournament not found error message
        assertThat(response.getErrorMessage(), is("ERROR|Tournament not found"));
        assertThat(response.getStatus(), is(HttpStatus.OK.value()));
    }

    @Test
    public void shouldSendErrorWithStatusOkWhenTournamentIdIsMissing() throws Exception {
        // when requesting the tournament status
        underTest.tournamentStatus(request, response, "", TOURNAMENT_OVERVIEW, 0, 100);

        // then the response should have tournament not found error message
        assertThat(response.getErrorMessage(), is("ERROR|Tournament invalid"));
        assertThat(response.getStatus(), is(HttpStatus.OK.value()));
    }

    @Test
    public void shouldIgnorePageNumberForOverviewRequests() throws Exception {
        // when requesting the tournament overview
        underTest.tournamentStatus(request, response, REQUEST_TOURNAMENT_ID, TOURNAMENT_OVERVIEW, null, null);

        // then the view document should be built for page 1
        verify(tournamentViewDocumentWorker).buildDocument(tournamentView,
                new TournamentDocumentRequest(TOURNAMENT_OVERVIEW, PLAYER_ID, DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE));
    }

    @Test
    public void shouldIgnorePageNumberForPlayerRequests() throws Exception {
        // when requesting a player status
        underTest.tournamentStatus(request, response, REQUEST_TOURNAMENT_ID, TOURNAMENT_PLAYER, null, null);

        // then document should be built for page 1
        verify(tournamentViewDocumentWorker).buildDocument(
                tournamentView,
                new TournamentDocumentRequest(TOURNAMENT_PLAYER, PLAYER_ID, DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE));
    }

    @Test
    public void shouldBuildDocumentWithPage1OfRanksStatusRequestWithNoPageNumber() throws Exception {
        // when requesting the tournament status with no page number
        underTest.tournamentStatus(request, response, REQUEST_TOURNAMENT_ID, TOURNAMENT_STATUS, null, null);

        // then the view should be built for page 1 of players/ranks
        verify(tournamentViewDocumentWorker).buildDocument(
                tournamentView,
                new TournamentDocumentRequest(TOURNAMENT_STATUS, PLAYER_ID, DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE));
    }

    @Test
    public void shouldBuildDocumentWithDefaultPageSizeAndNumberForRanksRequestsWithNoPageNumberNorPageSize() throws Exception {
        // when requesting the tournament/player ranks
        underTest.tournamentStatus(request, response, REQUEST_TOURNAMENT_ID, TOURNAMENT_RANKS, null, null);

        // then the view should be built for page 1 of ranks
        verify(tournamentViewDocumentWorker).buildDocument(
                tournamentView,
                new TournamentDocumentRequest(TOURNAMENT_RANKS, PLAYER_ID, DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE));
    }

    @Test
    public void shouldBuildDocumentForGivenPageAndDefaultPageSizeForStatusRequests() throws Exception {
        // given a page number of
        int pageNumber = 897;

        // when requesting the tournament status for this page
        underTest.tournamentStatus(request, response, REQUEST_TOURNAMENT_ID, TOURNAMENT_STATUS, pageNumber, null);

        // then the view should be built for page 1 of ranks/players and default page size
        verify(tournamentViewDocumentWorker).buildDocument(
                tournamentView,
                new TournamentDocumentRequest(TOURNAMENT_STATUS, PLAYER_ID, pageNumber, DEFAULT_PAGE_SIZE));
    }

    @Test
    public void shouldBuildDocumentForGivenPageAndDefaultPageSizeForRanksRequests() throws Exception {
        //given a page number of
        int pageNumber = 45;

        // when requesting the tournament/player ranks
        underTest.tournamentStatus(request, response, REQUEST_TOURNAMENT_ID, TOURNAMENT_RANKS, pageNumber, null);

        // then the view should be built for page 1 of ranks and default page size
        verify(tournamentViewDocumentWorker).buildDocument(tournamentView, new TournamentDocumentRequest(
                TOURNAMENT_RANKS, PLAYER_ID, pageNumber, DEFAULT_PAGE_SIZE));
    }

    @Test
    public void shouldBuildDocumentForGivenPageAndDefaultPageSizeForPlayersRequests() throws Exception {
        // given this page number
        int pageNumber = 12;

        // when requesting the tournament players
        underTest.tournamentStatus(request, response, REQUEST_TOURNAMENT_ID, TOURNAMENT_PLAYERS, pageNumber, null);

        // then the view should be built for page 1 of players and default page size
        verify(tournamentViewDocumentWorker).buildDocument(
                tournamentView,
                new TournamentDocumentRequest(TOURNAMENT_PLAYERS, PLAYER_ID, pageNumber, DEFAULT_PAGE_SIZE));
    }

    @Test
    public void shouldBuildDocumentForGivenPageAndPageSize() throws Exception {
        // given
        int pageSize = 111;
        int pageNumber = 9;

        // when requesting the tournament status with
        underTest.tournamentStatus(request, response, REQUEST_TOURNAMENT_ID, TOURNAMENT_STATUS, pageNumber, pageSize);

        // then the view should be built for given page and page size of players/ranks
        verify(tournamentViewDocumentWorker).buildDocument(
                tournamentView,
                new TournamentDocumentRequest(TOURNAMENT_STATUS, PLAYER_ID, pageNumber, pageSize));
    }

    @Test
    public void whenPageSizeIsLessThan1ShouldUseDefaultPageSize() throws Exception {
        // given incorrect page size
        int pageSize = 0;

        // when requesting the tournament status with page size
        underTest.tournamentStatus(request, response, REQUEST_TOURNAMENT_ID, TOURNAMENT_STATUS, null, pageSize);

        // then the view should be built for default page size
        verify(tournamentViewDocumentWorker).buildDocument(
                tournamentView,
                new TournamentDocumentRequest(TOURNAMENT_STATUS, PLAYER_ID, DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE));
    }

    @Test
    public void whenPageNumberIsLessThan1ShouldUseDefaultPageSize() throws Exception {
        // given incorrect page number
        int pageNumber = 0;

        // when requesting the tournament status for page
        underTest.tournamentStatus(request, response, REQUEST_TOURNAMENT_ID, TOURNAMENT_STATUS, pageNumber, null);

        // then the view should be built for default page number
        verify(tournamentViewDocumentWorker).buildDocument(
                tournamentView,
                new TournamentDocumentRequest(TOURNAMENT_STATUS, PLAYER_ID, DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE));
    }
}
