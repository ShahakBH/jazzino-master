package com.yazino.web.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.yazino.platform.tournament.TournamentRankView;
import com.yazino.platform.tournament.TournamentRoundView;
import com.yazino.platform.tournament.TournamentView;
import com.yazino.platform.tournament.TournamentViewDetails;
import com.yazino.test.ThreadLocalDateTimeUtils;
import com.yazino.web.domain.TournamentDocument;
import com.yazino.web.domain.TournamentDocumentBuilder;
import com.yazino.web.domain.TournamentDocumentRequest;
import org.hamcrest.CoreMatchers;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.*;

import static com.yazino.platform.tournament.TournamentRankView.EliminationReason.NOT_ENOUGH_CHIPS_FOR_ROUND;
import static com.yazino.platform.tournament.TournamentViewDetails.Status.REGISTERING;
import static com.yazino.platform.tournament.TournamentViewDetails.Status.RUNNING;
import static com.yazino.web.service.TournamentViewDocumentWorker.DocumentType.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TournamentViewDocumentWorkerTest {

    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(200);
    private static final BigDecimal PLAYER_ID_NOT_PLAYING = BigDecimal.ZERO;
    public static final BigDecimal TABLE_ID = BigDecimal.TEN;

    private final ObjectMapper mapper = new ObjectMapper();

    private TournamentViewDocumentWorker underTest;
    private TournamentView view;
    private TournamentViewDetails overview;
    private List<TournamentRoundView> rounds;
    private Map<BigDecimal, TournamentRankView> players;
    private List<TournamentRankView> ranks;
    private String sent;

    @Before
    public void setUp() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(3141592L);

        mapper.registerModule(new JodaModule());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        underTest = new TournamentViewDocumentWorker();
        overview = new TournamentViewDetails.Builder().name("aTournament").status(RUNNING).build();
        ranks = new ArrayList<>();
        players = new HashMap<>();
        final TournamentRankView rankPlayer1 = new TournamentRankView.Builder().playerId(PLAYER_ID).rank(1).build();
        ranks.add(rankPlayer1);
        players.put(PLAYER_ID, rankPlayer1);
        rounds = new ArrayList<>();
        rounds.add(new TournamentRoundView.Builder().level(1).minutes(5).build());

        sent = new DateTime().toString("yyyyMMdd-HHmm-ss-SSS");
        view = new TournamentView(overview, players, ranks, rounds, DateTimeUtils.currentTimeMillis());
    }

    @After
    public void resetJodaTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotBuildWithoutView() {
        underTest.buildDocument(null,
                new TournamentDocumentRequest(TOURNAMENT_PLAYER, PLAYER_ID,
                        null, null));
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotBuildWithoutDocumentRequest() {
        underTest.buildDocument(view, null);
    }

    @Test
    public void shouldBuildDocumentForPlayerWhenNotPlaying() throws JsonProcessingException {
        String expectedJson = mapper.writeValueAsString(
                new TournamentRankView.Builder().playerId(PLAYER_ID_NOT_PLAYING).build());
        String json = underTest.buildDocument(view,
                new TournamentDocumentRequest(TOURNAMENT_PLAYER,
                        PLAYER_ID_NOT_PLAYING, null, null));
        assertThat(json, is(expectedJson));
    }

    @Test
    public void shouldBuildDocumentForRegisteredPlayer() throws JsonProcessingException {
        TournamentRankView registeredPlayer = new TournamentRankView.Builder()
                .playerId(PLAYER_ID)
                .playerName("Arbuthnot Twitter")
                .status(TournamentRankView.Status.ACTIVE)
                .build();
        players.put(PLAYER_ID, registeredPlayer);
        String expectedJson = mapper.writeValueAsString(registeredPlayer);

        String json = underTest.buildDocument(view,
                new TournamentDocumentRequest(TOURNAMENT_PLAYER, PLAYER_ID,
                        null, null));

        assertThat(json, is(expectedJson));
    }

    @Test
    public void shouldBuildDocumentForActivePlayer() throws JsonProcessingException {
        TournamentRankView playingPlayer = new TournamentRankView.Builder()
                .playerId(PLAYER_ID)
                .playerName("Arbuthnot Twitter")
                .status(TournamentRankView.Status.ACTIVE)
                .balance(BigDecimal.valueOf(10000))
                .tableId(BigDecimal.valueOf(10306))
                .rank(45)
                .prize(BigDecimal.valueOf(2500))
                .build();
        players.put(PLAYER_ID, playingPlayer);
        String expectedJson = mapper.writeValueAsString(playingPlayer);

        String json = underTest.buildDocument(view,
                new TournamentDocumentRequest(TOURNAMENT_PLAYER, PLAYER_ID,
                        null, null));

        assertThat(json, is(expectedJson));
    }

    @Test
    public void shouldBuildDocumentForEliminatedPlayer() throws JsonProcessingException {
        TournamentRankView playingPlayer = new TournamentRankView.Builder()
                .status(TournamentRankView.Status.ELIMINATED)
                .playerId(PLAYER_ID)
                .playerName("Arbuthnot Twitter")
                .rank(12)
                .prize(BigDecimal.valueOf(0))
                .eliminationReason(NOT_ENOUGH_CHIPS_FOR_ROUND)
                .build();
        players.put(PLAYER_ID, playingPlayer);
        String expectedJson = mapper.writeValueAsString(playingPlayer);

        String json = underTest.buildDocument(view,
                new TournamentDocumentRequest(TOURNAMENT_PLAYER, PLAYER_ID, null, null));

        assertThat(json, is(expectedJson));
    }


    @Test
    public void shouldBuildDocumentForOverviewOnly() throws JsonProcessingException {
        String json = underTest.buildDocument(view,
                new TournamentDocumentRequest(TOURNAMENT_OVERVIEW, PLAYER_ID, null, null));
        assertTrue(json.contains(sent));
        assertTrue(json.contains(mapper.writeValueAsString(overview)));
    }

    @Test
    public void shouldBuildDocumentForAllRanksOnly() throws JsonProcessingException {
        // given 2 players
        createRanks(2);

        // when requesting the tournament rank document
        String json = underTest.buildDocument(view, new TournamentDocumentRequest(TOURNAMENT_RANKS, null, 1, 10));

        // then the serialized document should have players ordered by rank
        TournamentDocument expected = new TournamentDocumentBuilder().withOverview(null)
                .withRanks(ranks)
                .withRankPages(1)
                .withRankPage(1)
                .withRounds(null)
                .withSent(sent)
                .build();
        assertThat(json, CoreMatchers.is(mapper.writeValueAsString(expected)));
    }

    @Test
    public void shouldBuildDocumentForFullTournamentWhenRegisteringWithRanksAndPlayers() throws JsonProcessingException {
        // given a registering tournament
        overview = new TournamentViewDetails.Builder().name("aTournament").status(REGISTERING).build();
        // with payouts for top 12 ranks
        createRankPayouts(12);
        // and with these 5 players
        createRegisteringPlayers(5);
        // and requested player page is
        int pageNumber = 2;
        //with page size of
        int pageSize = 2;
        view = new TournamentView(overview, players, ranks, rounds, new DateTime().getMillis());

        // when requesting the full tournament document for page 1
        String json = underTest.buildDocument(view,
                new TournamentDocumentRequest(TOURNAMENT_STATUS, null, pageNumber, pageSize));

        // then the serialized document should have page 2 of players(ordered by name), ranks, overview and rounds
        TournamentDocument expected = new TournamentDocumentBuilder().withOverview(overview)
                .withRanks(ranks)
                .withPlayers(Arrays.asList(
                        players.get(BigDecimal.valueOf(3)),
                        players.get(BigDecimal.valueOf(2))))
                .withPlayerPages(3)
                .withPlayerPage(2)
                .withRounds(rounds)
                .withSent(sent)
                .build();
        assertThat(json, CoreMatchers.is(mapper.writeValueAsString(expected)));
    }

    private void createRankPayouts(int prizes) {
        ranks.clear();
        for (int i = 1; i <= prizes; i++) {
            final TournamentRankView rankView = new TournamentRankView.Builder()
                    .rank(i)
                    .prize(BigDecimal.valueOf(prizes - 1 + i * 1000))
                    .build();
            ranks.add(rankView);
        }
    }

    // creates players with ids 1..numberOfPlayers with names numberOfPlayers-1 .. 0, so not ordered as expected
    private void createRegisteringPlayers(int numberOfPlayers) {
        players.clear();
        for (int i = 1; i <= numberOfPlayers; i++) {
            BigDecimal playerId = BigDecimal.valueOf(i);
            players.put(playerId, new TournamentRankView.Builder()
                    .playerName("player name " + (numberOfPlayers - i))
                    .playerId(playerId)
                    .tableId(TABLE_ID)
                    .status(TournamentRankView.Status.ACTIVE)
                    .build());
        }
    }

    @Test
    public void shouldBuildDocumentForFullRunningTournament() throws JsonProcessingException {
        // given 3 players, and the overview and round info
        createRanks(3);

        String json = underTest.buildDocument(view, new TournamentDocumentRequest(TOURNAMENT_STATUS, null, 1, 5));

        // then the serialized document should have ranks ordered by rank and no players
        TournamentDocument expected = new TournamentDocumentBuilder().withOverview(overview)
                .withRanks(ranks)
                .withRankPages(1)
                .withRankPage(1)
                .withRounds(rounds)
                .withSent(sent)
                .build();
        assertThat(json, CoreMatchers.is(mapper.writeValueAsString(expected)));
    }

    @Test
    public void shouldBuildDocumentWithSpecifiedPageOfRanksOnly() throws JsonProcessingException {
        // given 7 ranks
        createRanks(7);
        // and page size of
        int pageSize = 2;
        // and page number
        int pageNumber = 2;

        // when requesting the tournament rank document
        String json = underTest.buildDocument(view,
                new TournamentDocumentRequest(TOURNAMENT_RANKS, null,
                        pageNumber, pageSize));

        // then the serialized document should have players ordered by rank
        TournamentDocument expected = new TournamentDocumentBuilder().withOverview(null)
                .withRanks(ranks.subList(2, 4))
                .withRankPages(4)
                .withRankPage(2)
                .withRounds(null)
                .withSent(sent)
                .build();
        assertThat(json, CoreMatchers.is(mapper.writeValueAsString(expected)));
    }

    @Test
    public void shouldBuildDocumentWithEmptyRanksWhenRequestingPageTooFar() throws JsonProcessingException {
        // given 7 ranks
        int numberOfRanks = 7;
        createRanks(numberOfRanks);
        // and page size of
        int pageSize = 4;
        // and page number
        int pageNumber = 3;

        // when requesting the tournament rank document for page 4
        String json = underTest.buildDocument(view,
                new TournamentDocumentRequest(TOURNAMENT_RANKS, null,
                        pageNumber, pageSize));

        // then the serialized document should have 0 players
        TournamentDocument expected = new TournamentDocumentBuilder().withOverview(null)
                .withRanks(ranks.subList(0, 0))
                .withRankPages(2)
                .withRankPage(3)
                .withRounds(null)
                .withSent(sent)
                .build();
        assertThat(json, CoreMatchers.is(mapper.writeValueAsString(expected)));
    }

    @Test
    public void shouldBuildDocumentWithEmptyRanksWhenRequestingPageZero() throws JsonProcessingException {
        // given 3 ranks
        int numberOfRanks = 3;
        createRanks(numberOfRanks);
        // and page size of
        int pageSize = 4;
        // and page number
        int pageNumber = 0;

        // when requesting the tournament rank document for page 4
        String json = underTest.buildDocument(view,
                new TournamentDocumentRequest(TOURNAMENT_RANKS, null,
                        pageNumber, pageSize));

        // then the serialized document should have 0 players
        TournamentDocument expected = new TournamentDocumentBuilder().withOverview(null)
                .withRanks(ranks.subList(0, 0))
                .withRankPages(1)
                .withRankPage(0)
                .withRounds(null)
                .withSent(sent)
                .build();
        assertThat(json, CoreMatchers.is(mapper.writeValueAsString(expected)));
    }

    private void createRanks(final int numberOfPlayers) {
        ranks.clear();
        List<TournamentRankView> players = new ArrayList<>();
        for (int i = 1; i <= numberOfPlayers; i++) {
            TournamentRankView rankView = new TournamentRankView.Builder()
                    .playerId(BigDecimal.valueOf(i))
                    .rank(i).build();
            players.add(rankView);
        }
        ranks.addAll(players);
    }

    @Test
    public void shouldBuildPlayersDocumentWithSpecifiedPageOfPlayersOnly() throws JsonProcessingException {
        // given 7 players
        int numberOfPlayers = 7;
        createRegisteringPlayers(numberOfPlayers);
        // and page size of
        int pageSize = 2;
        // and page number
        int pageNumber = 2;

        // when requesting the tournament players document
        String json = underTest.buildDocument(view,
                new TournamentDocumentRequest(TOURNAMENT_PLAYERS, null, pageNumber, pageSize));

        // then the serialized document should have players ordered by name
        List<TournamentRankView> orderedPlayers = new ArrayList<>(players.values());
        Collections.sort(orderedPlayers, new Comparator<TournamentRankView>() {
            @Override
            public int compare(TournamentRankView rv1, TournamentRankView rv2) {
                return rv1.getPlayerName().compareTo(rv2.getPlayerName());
            }
        });
        TournamentDocument expected = new TournamentDocumentBuilder().withOverview(null)
                .withPlayers(orderedPlayers.subList(2, 4))
                .withPlayerPages(4)
                .withPlayerPage(2)
                .withSent(sent)
                .build();
        assertThat(json, CoreMatchers.is(mapper.writeValueAsString(expected)));
    }

    @Test
    public void shouldBuildPlayersDocumentWithNoPlayersWhenRequestingAPageTooFar() throws JsonProcessingException {
        // given 7 players
        int numberOfPlayers = 7;
        createRegisteringPlayers(numberOfPlayers);
        // and page size of
        int pageSize = 4;
        // and page number
        int pageNumber = 3;

        // when requesting the tournament players document for a page 3
        String json = underTest.buildDocument(view,
                new TournamentDocumentRequest(TOURNAMENT_PLAYERS, null, pageNumber, pageSize));

        TournamentDocument expected = new TournamentDocumentBuilder().withOverview(null)
                .withPlayers(
                        Collections.<TournamentRankView>emptyList())
                .withPlayerPages(2)
                .withPlayerPage(3)
                .withSent(sent)
                .build();
        assertThat(json, CoreMatchers.is(mapper.writeValueAsString(expected)));
    }

    @Test
    public void shouldBuildPlayersDocumentWithNoPlayersWhenRequestingPageZero() throws JsonProcessingException {
        // given 7 players
        int numberOfPlayers = 7;
        createRegisteringPlayers(numberOfPlayers);
        // and page size of
        int pageSize = 4;
        // and page number
        int pageNumber = 0;

        // when requesting the tournament players document for a page 3
        String json = underTest.buildDocument(view,
                new TournamentDocumentRequest(TOURNAMENT_PLAYERS, null, pageNumber, pageSize));

        TournamentDocument expected = new TournamentDocumentBuilder().withOverview(null)
                .withPlayers(
                        Collections.<TournamentRankView>emptyList())
                .withPlayerPages(2)
                .withPlayerPage(0)
                .withSent(sent)
                .build();
        assertThat(json, CoreMatchers.is(mapper.writeValueAsString(expected)));
    }

}
