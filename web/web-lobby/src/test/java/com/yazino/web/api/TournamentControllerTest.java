package com.yazino.web.api;

import com.yazino.platform.AuthProvider;
import com.yazino.platform.Platform;
import com.yazino.platform.tournament.*;
import com.yazino.test.ThreadLocalDateTimeUtils;
import com.yazino.web.data.TournamentViewRepository;
import com.yazino.web.domain.TournamentDetailView;
import com.yazino.web.domain.TournamentLobbyCache;
import com.yazino.web.domain.UpcomingTournamentSummary;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.PlayerFriendsCache;
import com.yazino.web.util.WebApiResponses;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static com.google.common.collect.Sets.newHashSet;
import static com.yazino.platform.Partner.YAZINO;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TournamentControllerTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(1231);
    private static final BigDecimal TOURNAMENT_ID = BigDecimal.valueOf(2342354L);
    private static final BigDecimal FRIEND_ONE = BigDecimal.valueOf(234234);
    private static final BigDecimal FRIEND_TWO = BigDecimal.valueOf(89798);
    private static final BigDecimal NOT_FRIEND = BigDecimal.valueOf(2342890);
    private static final BigDecimal ENTRY_FEE = BigDecimal.valueOf(1000);
    private static final BigDecimal PRIZE_POOL = BigDecimal.valueOf(2000);

    @Mock
    private LobbySessionCache lobbySessionCache;
    @Mock
    private TournamentLobbyCache tournamentLobbyCache;
    @Mock
    private TournamentViewRepository tournamentViewRepository;
    @Mock
    private PlayerFriendsCache playerFriendsCache;
    @Mock
    private WebApiResponses webApiResponses;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private TournamentController underTest;

    @Before
    public void setUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(DateTimeUtils.currentTimeMillis());

        underTest = new TournamentController(lobbySessionCache, tournamentLobbyCache, tournamentViewRepository, playerFriendsCache, webApiResponses);

        when(lobbySessionCache.getActiveSession(request)).thenReturn(aSession());
    }

    @After
    public void resetJodaTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test(expected = NullPointerException.class)
    public void theControllerCannotBeCreatedWithANullLobbySessionCache() {
        new TournamentController(null, tournamentLobbyCache, tournamentViewRepository, playerFriendsCache, webApiResponses);
    }

    @Test(expected = NullPointerException.class)
    public void theControllerCannotBeCreatedWithANullTournamentLobbyCache() {
        new TournamentController(lobbySessionCache, null, tournamentViewRepository, playerFriendsCache, webApiResponses);
    }

    @Test(expected = NullPointerException.class)
    public void theControllerCannotBeCreatedWithANullTournamentViewRepository() {
        new TournamentController(lobbySessionCache, tournamentLobbyCache, null, playerFriendsCache, webApiResponses);
    }

    @Test(expected = NullPointerException.class)
    public void theControllerCannotBeCreatedWithANullPlayerFriendsCache() {
        new TournamentController(lobbySessionCache, tournamentLobbyCache, tournamentViewRepository, null, webApiResponses);
    }

    @Test(expected = NullPointerException.class)
    public void theControllerCannotBeCreatedWithANullWebApiResponses() {
        new TournamentController(lobbySessionCache, tournamentLobbyCache, tournamentViewRepository, playerFriendsCache, null);
    }

    @Test
    public void nextTournamentsForAllVariationsShouldReturnTheResponseOfTheTournamentLobbyCache() throws IOException {
        when(tournamentLobbyCache.getNextTournamentsForEachVariation(PLAYER_ID, "aGameType")).thenReturn(aMapOfTournamentSummaries());

        underTest.nextTournamentsForAllVariations("aGameType", request, response);

        verify(webApiResponses).writeOk(response, aMapOfTournamentSummaries());
    }

    @Test
    public void nextTournamentsForAllVariationsShouldReturnUnauthorisedWhenNoLobbySessionIsPresent() throws IOException {
        reset(lobbySessionCache);

        underTest.nextTournamentsForAllVariations("aGameType", request, response);

        verify(webApiResponses).writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "No session");
    }

    @Test
    public void nextTournamentsForAllVariationsShouldReturnInternalServerErrorOnAnException() throws IOException {
        when(tournamentLobbyCache.getNextTournamentsForEachVariation(PLAYER_ID, "aGameType"))
                .thenThrow(new IllegalArgumentException("aTestException"));

        underTest.nextTournamentsForAllVariations("aGameType", request, response);

        verify(webApiResponses).writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Failed to get next tournaments for game type aGameType and player " + PLAYER_ID);
    }

    @Test
    public void tournamentDetailShouldReturnUnauthorisedWhenNoLobbySessionIsPresent() throws IOException {
        reset(lobbySessionCache);

        underTest.tournamentDetailFor(TOURNAMENT_ID, request, response);

        verify(webApiResponses).writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "No session");
    }

    @Test
    public void tournamentDetailShouldReturnNotFoundWhenTheTournamentIdIsNotFound() throws IOException {
        underTest.tournamentDetailFor(TOURNAMENT_ID, request, response);

        verify(webApiResponses).writeNoContent(response, HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    public void tournamentDetailShouldReturnTheTournamentDetailViewForTheMatchingRecord() throws IOException {
        when(tournamentViewRepository.getTournamentView(TOURNAMENT_ID)).thenReturn(aTournamentView());
        when(playerFriendsCache.getFriendIds(PLAYER_ID)).thenReturn(newHashSet(FRIEND_ONE, FRIEND_TWO));

        underTest.tournamentDetailFor(TOURNAMENT_ID, request, response);

        verify(webApiResponses).writeOk(response, new TournamentDetailView(aTournamentView(), PLAYER_ID, newHashSet(FRIEND_ONE, FRIEND_TWO)));
    }

    @Test
    public void tournamentDetailShouldReturnInternalServerErrorOnAnException() throws IOException {
        when(tournamentViewRepository.getTournamentView(TOURNAMENT_ID)).thenThrow(new IllegalStateException("aTestException"));

        underTest.tournamentDetailFor(TOURNAMENT_ID, request, response);

        verify(webApiResponses).writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                String.format("Failed to get tournament detail for %s with player %s", TOURNAMENT_ID, PLAYER_ID));
    }

    private TournamentView aTournamentView() {
        final TournamentViewDetails details = new TournamentViewDetails.Builder()
                .tournamentId(TOURNAMENT_ID)
                .name("aName")
                .description("aDescription")
                .gameType("aGameType")
                .variationTemplateName("aVariationTemplateName")
                .entryFee(ENTRY_FEE)
                .prizePool(PRIZE_POOL)
                .millisTillStart(60000L)
                .build();

        final Map<BigDecimal, TournamentRankView> players = new HashMap<>();
        players.put(FRIEND_ONE, new TournamentRankView.Builder().playerName("friendOne").build());
        players.put(FRIEND_TWO, new TournamentRankView.Builder().playerName("friendTwo").build());
        players.put(NOT_FRIEND, new TournamentRankView.Builder().playerName("notFriend").build());

        final List<TournamentRankView> ranks = new ArrayList<>();
        ranks.add(new TournamentRankView.Builder().rank(1).prize(BigDecimal.valueOf(100)).build());
        ranks.add(new TournamentRankView.Builder().rank(2).prize(BigDecimal.valueOf(200)).build());
        ranks.add(new TournamentRankView.Builder().rank(3).prize(BigDecimal.valueOf(300)).build());

        return new TournamentView(details, players, ranks, Collections.<TournamentRoundView>emptyList(), 2354325435L);
    }

    private Map<String, UpcomingTournamentSummary> aMapOfTournamentSummaries() {
        final Map<String, UpcomingTournamentSummary> summaries = new HashMap<>();
        summaries.put("aVariation", new UpcomingTournamentSummary(PLAYER_ID, Collections.<BigDecimal>emptySet(),
                new TournamentRegistrationInfo(BigDecimal.valueOf(2342), new DateTime(324345435L), BigDecimal.ZERO,
                        BigDecimal.ZERO, "aName", "aDescription", "aVariationName", Collections.<BigDecimal>emptySet())
        ));
        return summaries;
    }

    private LobbySession aSession() {
        return new LobbySession(BigDecimal.ONE, PLAYER_ID, "aPlayerName", "aSessionKey", YAZINO,
                "aPictureUrl", "anEmail", null, false, Platform.WEB, AuthProvider.YAZINO);
    }

}
