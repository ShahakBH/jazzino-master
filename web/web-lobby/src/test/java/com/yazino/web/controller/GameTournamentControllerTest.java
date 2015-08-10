package com.yazino.web.controller;

import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.community.PlayerTrophyService;
import com.yazino.platform.tournament.*;
import com.yazino.web.domain.TournamentLobbyCache;
import com.yazino.web.domain.TrophyLeaderBoardSummary;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.JsonHelper;
import com.yazino.web.util.WebApiResponses;
import com.yazino.web.util.PlayerFriendsCache;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static com.yazino.web.controller.GameTournamentController.LeaderBoardType.FRIENDS;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class GameTournamentControllerTest {
    private static final String GAME_TYPE = "BLACKJACK";
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(100);

    @Mock
    private LobbySessionCache lobbySessionCache;
    @Mock
    private TournamentLobbyCache tournamentLobbyCache;
    @Mock
    private TournamentService tournamentService;
    @Mock
    private PlayerTrophyService playerTrophyService;
    @Mock
    private PlayerFriendsCache playerFriendsCache;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private WebApiResponses webApiResponses;

    private GameTournamentController underTest;

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);

        when(lobbySessionCache.getActiveSession(request)).thenReturn(aLobbySession());
        when(tournamentLobbyCache.getTournamentSchedule(GAME_TYPE, PLAYER_ID))
                .thenReturn(asList(aTournamentAt(1000)));

        underTest = new GameTournamentController(lobbySessionCache, tournamentLobbyCache, tournamentService,
                playerTrophyService, playerFriendsCache, webApiResponses);
    }

    @Test
    public void tournamentScheduleReturnsAnInvalidRequestIfGameTypeIsNotProvided() throws IOException {
        underTest.tournamentSchedule(null, request, response);
        verify(response).sendError(400);
    }

    @Test
    public void tournamentScheduleShouldFindTheScheduleForTheLoggedInUser() throws IOException {
        underTest.tournamentSchedule(GAME_TYPE, request, response);
        TournamentDetail tournamentDetail = aTournamentAt(1000);
        verify(webApiResponses).writeOk(response, asList(tournamentDetail));
    }

    @Test
    public void tournamentScheduleShouldBeEmptyIfThereIsNoLoggedInUser() throws IOException {
        reset(lobbySessionCache);
        underTest.tournamentSchedule(GAME_TYPE, request, response);
        verify(webApiResponses).writeOk(response, Collections.emptyList());
    }

    @Test
    public void shouldReturnEmptyListWhenNoTournamentSummary() throws Exception {
        underTest.lastTournament("SLOTS", response);
        verify(webApiResponses).writeOk(response, Collections.emptyList());
    }

    @Test
    public void shouldReturnListWhenTournamentSummary() throws Exception {
        Summary tournamentSummary = expectsTournamentSummary();
        underTest.lastTournament("SLOTS", response);
        verify(webApiResponses).writeOk(response, tournamentSummary.getPlayers());
    }

    private Summary expectsTournamentSummary() {
        TournamentPlayerSummary playerSummary = new TournamentPlayerSummary(BigDecimal.ONE, 5, "Test", BigDecimal.TEN,
                "foo");
        Summary tournamentSummary = new Summary(BigDecimal.valueOf(100), "aTournamentName", new DateTime().toDate(),
                "SLOTS", asList(playerSummary));
        when(tournamentLobbyCache.getLastTournamentSummary(tournamentSummary.getGameType())).thenReturn(tournamentSummary);
        return tournamentSummary;
    }

    @Test
    public void shouldReturnFilteredListWhenWhenTournamentSummaryFriends() throws Exception {
        Summary tournamentSummary = expectsTournamentSummary();
        final HashSet<BigDecimal> friendIds = expectFriends(tournamentSummary.getPlayers().get(0).getId());
        underTest.lastTournamentFriends("SLOTS", response,request);
        verify(webApiResponses).writeOk(response, tournamentSummary.getPlayersWithIds(friendIds));
    }

    @Test
    public void shouldReturnEmptyListWhenNoHallOfFame() throws Exception {
        underTest.hallOfFame("SLOTS", response);
        verify(webApiResponses).writeOk(response, Collections.emptyList());
    }

    @Test
    public void shouldReturnListWhenHallOfFame() throws Exception {
        HashMap<String, List<TrophyWinner>> results = new HashMap<>();
        List<TrophyWinner> expected = Arrays.asList(new TrophyWinner(BigDecimal.ONE, "Test", "foo", 10));
        results.put("SLOTS", expected);
        when(playerTrophyService.findWinnersByTrophyName(anyString(), anyInt(), eq("SLOTS"))).thenReturn(results);
        underTest.hallOfFame("SLOTS", response);
        verify(webApiResponses).writeOk(response, expected);
    }

    private TournamentDetail aTournamentAt(final long time) {
        return new TournamentDetail(BigDecimal.valueOf(2000), "aTournamentName", GAME_TYPE, "aVariationTemplateName", "aDescription",
                2, 0, false, true, BigDecimal.valueOf(1000), BigDecimal.ZERO, BigDecimal.valueOf(100), 10000000L, new Date(time));
    }

    private LobbySession aLobbySession() {
        return new LobbySession(BigDecimal.valueOf(3141592), PLAYER_ID, "aName", "aSessionkey", Partner.YAZINO, "aPictureUrl", "anEmail", null, false, Platform.WEB, AuthProvider.YAZINO);
    }

    @Test
    public void trophyleaderBoardSummaryShouldReturnEmptyResponseWhenNoLobbySession() {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(null);

        final String responseBody = underTest.trophyLeaderBoardSummary(request, response, GAME_TYPE, 10, null);
        assertThat(responseBody, is("{}"));
    }

    @Test
    public void trophyleaderBoardSummaryShouldReturnEmptyResponseWhenNoGameType() {
        final String responseBody = underTest.trophyLeaderBoardSummary(request, response, null, 10, null);
        assertThat(responseBody, is("{}"));
    }

    @Test
    public void shouldReturnTrophyLeaderBoardSummaryForWorld() {
        // given the summary
        final TrophyLeaderboardPlayer player = new TrophyLeaderboardPlayer(1, PLAYER_ID, "aName", 1000, "url");
        TrophyLeaderBoardSummary expectedSummary = new TrophyLeaderBoardSummary(
                Arrays.asList(player),
                Arrays.asList(new TrophyLeaderboardPosition(1,34,3400, BigDecimal.TEN)),
                player,
                300000);
        when(tournamentLobbyCache.getTrophyLeaderBoardSummary(PLAYER_ID, GAME_TYPE, 10)).thenReturn(expectedSummary);

        // when requesting the summary
        final String responseBody = underTest.trophyLeaderBoardSummary(request, response, GAME_TYPE, 10, null);

        // then the response should be
        String expectedJson = new JsonHelper().serialize(expectedSummary);
        assertThat(responseBody, is(expectedJson));
    }

    @Test
    public void shouldReturnTrophyLeaderBoardSummaryForFriends() {
        // given the summary
        final TrophyLeaderboardPlayer player = new TrophyLeaderboardPlayer(1, PLAYER_ID, "aName", 1000, "url");
        TrophyLeaderBoardSummary expectedSummary = new TrophyLeaderBoardSummary(
                Arrays.asList(player),
                Arrays.asList(new TrophyLeaderboardPosition(1,34,3400, BigDecimal.TEN)),
                player,
                300000);
        final HashSet<BigDecimal> friendIds = expectFriends();
        when(tournamentLobbyCache.getTrophyLeaderBoardSummaryForFriends(PLAYER_ID, GAME_TYPE, friendIds, 10)).thenReturn(expectedSummary);

        // when requesting the summary
        final String responseBody = underTest.trophyLeaderBoardSummary(request, response, GAME_TYPE, 10, FRIENDS);

        // then the response should be
        String expectedJson = new JsonHelper().serialize(expectedSummary);
        assertThat(responseBody, is(expectedJson));
    }

    private HashSet<BigDecimal> expectFriends(final BigDecimal... friendIds) {
        final HashSet<BigDecimal> friendIdsSet = new HashSet<>(Arrays.asList(friendIds));
        when(playerFriendsCache.getFriendIds(PLAYER_ID)).thenReturn(friendIdsSet);
        return friendIdsSet;
    }

}
