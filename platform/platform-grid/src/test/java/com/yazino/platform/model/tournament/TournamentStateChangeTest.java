package com.yazino.platform.model.tournament;

import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.model.table.Client;
import com.yazino.platform.processor.tournament.SingleTableAllocatorFactory;
import com.yazino.platform.processor.tournament.TableAllocator;
import com.yazino.platform.processor.tournament.TournamentHost;
import com.yazino.platform.processor.tournament.TournamentPlayerStatisticPublisher;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import com.yazino.platform.repository.tournament.TournamentRepository;
import com.yazino.platform.service.account.InternalWalletService;
import com.yazino.platform.service.tournament.TournamentTableService;
import com.yazino.platform.tournament.TournamentStatus;
import com.yazino.platform.tournament.TournamentType;
import com.yazino.platform.tournament.TournamentVariationRound;
import com.yazino.platform.tournament.TournamentVariationTemplate;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import com.yazino.game.api.time.SettableTimeSource;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class TournamentStateChangeTest {
    private static final String TABLE_PARTNER_ID = "INTERNAL";
    private static final String TABLE_CLIENT_ID = "Red Blackjack";
    private static final BigDecimal GAME_VARIATION_ID = BigDecimal.valueOf(8883423L);
    private static final BigDecimal TOURNAMENT_ID = BigDecimal.valueOf(18L);
    private static final BigDecimal TABLE_ID_1 = BigDecimal.valueOf(18834L);
    private static final BigDecimal TABLE_ID_2 = BigDecimal.valueOf(5433423L);
    private static final BigDecimal TABLE_ID_3 = BigDecimal.valueOf(2332939L);

    private final Map<BigDecimal, List<TournamentPlayer>> playerMap = new HashMap<BigDecimal, List<TournamentPlayer>>();

    private TournamentHost tournamentHost;
    private TournamentTableService tournamentTableService;
    private TableAllocator tableAllocator;

    private SettableTimeSource timeSource;
    private long tournamentSignupStartTime = System.currentTimeMillis();
    private long tournamentSignupEndTime = tournamentSignupStartTime + convertMinutesToMiliseconds(1);
    private long tournamentStartTime = tournamentSignupEndTime + convertMinutesToMiliseconds(1);

    private long tournamentEndTime = tournamentStartTime + convertMinutesToMiliseconds(18);
    long roundLength = 300;
    long roundGap = 60;
    private TournamentLeaderboard leaderboard;

    @Before
    public void setUp() {
        playerMap.clear();

        timeSource = new SettableTimeSource();

        tournamentTableService = mock(TournamentTableService.class);
        tableAllocator = mock(TableAllocator.class);
        leaderboard = mock(TournamentLeaderboard.class);

        when(tournamentTableService.findClientById(TABLE_CLIENT_ID)).thenReturn(
                new Client(TABLE_CLIENT_ID, 5, null, null, null));

        tournamentHost = new TournamentHost(timeSource,
                mock(InternalWalletService.class),
                tournamentTableService,
                mock(TournamentRepository.class),
                new SingleTableAllocatorFactory(tableAllocator),
                mock(PlayerRepository.class),
                mock(PlayerSessionRepository.class),
                mock(DocumentDispatcher.class),
                mock(TournamentPlayerStatisticPublisher.class));
    }

    @Test
    public void moves_state_from_announced_to_running_if_past_start_time() throws WalletServiceException {
        final Tournament tournament = createTestTournament(TournamentStatus.ANNOUNCED);
        timeSource.setMillis(tournamentStartTime + 100);

        expectTableOpen(tournament, true);

        tournament.processEvent(tournamentHost);

        assertEquals(TournamentStatus.RUNNING, tournament.getTournamentStatus());
        // nextEvent = round end time.
        assertEquals(tournament.getNextEvent().longValue(), tournamentStartTime + roundLength);

        verifyTableOpen(tournament);
    }

    @Test
    public void moves_state_from_announced_to_registering_if_past_signup_start_time_and_signup_end_time_is_NOT_null() {
        final Tournament tournament = createTestTournament(TournamentStatus.ANNOUNCED);
        timeSource.setMillis(tournamentSignupStartTime + 100);

        tournament.processEvent(tournamentHost);

        assertEquals(TournamentStatus.REGISTERING, tournament.getTournamentStatus());
        assertEquals(tournament.getNextEvent().longValue(), tournamentSignupEndTime);
    }

    @Test
    public void moves_state_from_announced_to_registering_if_past_signup_start_time_and_signup_end_time_IS_null() {
        final Tournament tournament = createTestTournament(TournamentStatus.ANNOUNCED);
        timeSource.setMillis(tournamentSignupStartTime + 100);
        tournament.setSignupEndTimeStamp(null);

        tournament.processEvent(tournamentHost);

        assertEquals(TournamentStatus.REGISTERING, tournament.getTournamentStatus());
        assertEquals(tournament.getNextEvent().longValue(), tournamentStartTime);
    }

    @Test
    public void moves_state_from_registering_to_running_if_past_start_time() throws WalletServiceException {
        final Tournament tournament = createTestTournament(TournamentStatus.REGISTERING);
        timeSource.setMillis(tournamentStartTime + 100);

        expectTableOpen(tournament, true);

        tournament.processEvent(tournamentHost);

        assertEquals(TournamentStatus.RUNNING, tournament.getTournamentStatus());

        // nextEvent = round end time.
        assertEquals(tournament.getNextEvent().longValue(), tournamentStartTime + roundLength);
        verifyTableOpen(tournament);
    }

    @Test
    public void moves_state_from_registering_to_announced_if_past_signup_end_time() {
        final Tournament tournament = createTestTournament(TournamentStatus.REGISTERING);
        timeSource.setMillis(tournamentSignupEndTime + 100);

        tournament.processEvent(tournamentHost);

        assertEquals(TournamentStatus.ANNOUNCED, tournament.getTournamentStatus());
        assertEquals(tournament.getNextEvent().longValue(), tournamentStartTime);
    }

    @Test
    public void moves_state_from_running_to_waiting_if_past_end_time() {
        final Tournament tournament = createTestTournament(TournamentStatus.RUNNING);
        timeSource.setMillis(tournamentEndTime + 100);

        tournament.setTournamentLeaderboard(leaderboard);
        when(leaderboard.isInsufficientPlayersPresent()).thenReturn(false);

        tournament.processEvent(tournamentHost);

        assertEquals(TournamentStatus.WAITING_FOR_CLIENTS, tournament.getTournamentStatus());
        assertEquals(tournament.getNextEvent().longValue(), timeSource.getCurrentTimeStamp() + tournamentHost.getPollDelay());
        verify(tournamentTableService).requestClosing(new HashSet<BigDecimal>(tournament.getTables()));
    }

    @Test
    public void moves_state_from_running_to_waiting_if_first_round_ended() {
        final Tournament tournament = createTestTournament(TournamentStatus.RUNNING);
        timeSource.setMillis(tournamentStartTime + roundLength + 1);

        tournament.setTournamentLeaderboard(leaderboard);
        when(leaderboard.isInsufficientPlayersPresent()).thenReturn(false);

        tournament.processEvent(tournamentHost);

        assertEquals(TournamentStatus.WAITING_FOR_CLIENTS, tournament.getTournamentStatus());
        assertEquals(tournament.getNextEvent().longValue(), timeSource.getCurrentTimeStamp() + tournamentHost.getPollDelay());
        verify(tournamentTableService).requestClosing(new HashSet<BigDecimal>(tournament.getTables()));
    }

    @Test
    public void moves_state_from_running_to_waiting_if_second_round_ended() {
        final Tournament tournament = createTestTournament(TournamentStatus.RUNNING);

        timeSource.setMillis(tournamentStartTime + roundLength + roundGap + roundLength + 1);

        tournament.setTournamentLeaderboard(leaderboard);
        when(leaderboard.isInsufficientPlayersPresent()).thenReturn(false);

        tournament.processEvent(tournamentHost);

        assertEquals(TournamentStatus.WAITING_FOR_CLIENTS, tournament.getTournamentStatus());
        assertEquals(tournament.getNextEvent().longValue(), timeSource.getCurrentTimeStamp() + tournamentHost.getPollDelay());
        verify(tournamentTableService).requestClosing(new HashSet<BigDecimal>(tournament.getTables()));
    }

    @Test
    public void does_not_change_state_if_tables_remain_open() {
        final Tournament tournament = createTestTournament(TournamentStatus.WAITING_FOR_CLIENTS);
        timeSource.setMillis(tournamentStartTime + 1);

        expectTableCloseCheck(tournament, 1);

        tournament.processEvent(tournamentHost);

        assertEquals(TournamentStatus.WAITING_FOR_CLIENTS, tournament.getTournamentStatus());
        assertEquals(tournament.getNextEvent().longValue(), timeSource.getCurrentTimeStamp() + tournamentHost.getPollDelay());
    }

    @Test
    public void moves_state_from_onbreak_to_running_if_past_next_round_start_time_second_round() throws WalletServiceException {
        final Tournament tournament = createTestTournament(TournamentStatus.ON_BREAK);
        tournament.setTournamentLeaderboard(leaderboard);
        timeSource.setMillis(tournamentStartTime + roundLength + roundGap + 1);

        expectTableOpen(tournament, false);
        when(leaderboard.getActivePlayers()).thenReturn(new HashSet<TournamentPlayer>(playerMap.get(tournament.getTournamentId())));
        when(leaderboard.updateLeaderboard(tournament, tournamentHost, false, true)).thenReturn(false);

        tournament.processEvent(tournamentHost);

        assertEquals(TournamentStatus.RUNNING, tournament.getTournamentStatus());

        //nextEvent = round end time
        assertEquals(tournament.getNextEvent().longValue(), tournamentStartTime + roundLength + roundGap + roundLength);
        verifyTableOpen(tournament);
    }

    @Test
    public void moves_state_from_onbreak_to_running_if_past_next_round_start_time_third_round() throws WalletServiceException {
        final Tournament tournament = createTestTournament(TournamentStatus.ON_BREAK, 1);
        tournament.setTournamentLeaderboard(leaderboard);
        timeSource.setMillis(tournamentStartTime + 2 * roundLength + 2 * roundGap + 1);

        expectTableOpen(tournament, false);
        when(leaderboard.getActivePlayers()).thenReturn(new HashSet<TournamentPlayer>(playerMap.get(tournament.getTournamentId())));
        when(leaderboard.updateLeaderboard(tournament, tournamentHost, false, true)).thenReturn(false);

        tournament.processEvent(tournamentHost);

        assertEquals(TournamentStatus.RUNNING, tournament.getTournamentStatus());

        //nextEvent = round end time
        assertEquals(tournament.getNextEvent().longValue(), tournamentStartTime + 2 * roundLength + 2 * roundGap + roundLength);
        verifyTableOpen(tournament);
    }

    private Tournament createTestTournament(TournamentStatus status) {
        return createTestTournament(status, 0);
    }

    private Tournament createTestTournament(TournamentStatus status, int roundIndex) {
        final TournamentVariationTemplate template = new TournamentVariationTemplateBuilder()
                .setTournamentVariationTemplateId(BigDecimal.valueOf(17L))
                .setTournamentType(TournamentType.SITNGO)
                .setEntryFee(BigDecimal.valueOf(10))
                .setServiceFee(BigDecimal.valueOf(1))
                .setStartingChips(BigDecimal.valueOf(100))
                .setMinPlayers(1)
                .setMaxPlayers(10)
                .setTemplateName("testTmpl")
                .addTournamentRound(new TournamentVariationRound(1, roundGap, roundLength, GAME_VARIATION_ID, "Red Blackjack", BigDecimal.ZERO, "0"))
                .addTournamentRound(new TournamentVariationRound(2, roundGap, roundLength, GAME_VARIATION_ID, "Red Blackjack", BigDecimal.ZERO, "0"))
                .addTournamentRound(new TournamentVariationRound(3, roundGap, roundLength, GAME_VARIATION_ID, "Red Blackjack", BigDecimal.ZERO, "0"))
                .toTemplate();

        final List<TournamentPlayer> tournamentPlayers = new ArrayList<TournamentPlayer>();
        for (int i = 0; i < 8; ++i) {
            tournamentPlayers.add(new TournamentPlayer(
                    BigDecimal.valueOf(i), "player " + i, BigDecimal.valueOf(i), TournamentPlayerStatus.ACTIVE));
        }

        final Tournament tournament = new Tournament(TOURNAMENT_ID, new HashSet<TournamentPlayer>(tournamentPlayers));
        tournament.setTournamentVariationTemplate(template);
        tournament.setCurrentRoundIndex(roundIndex);
        tournament.setStartTimeStamp(new DateTime(tournamentStartTime));
        tournament.setSignupStartTimeStamp(new DateTime(tournamentSignupStartTime));
        tournament.setSignupEndTimeStamp(new DateTime(tournamentSignupEndTime));

        tournament.setPartnerId(TABLE_PARTNER_ID);

        tournament.setTournamentStatus(status);
        tournament.setName("T1test");
        tournament.setTables(new ArrayList<BigDecimal>());
        playerMap.put(tournament.getTournamentId(), tournamentPlayers);

        return tournament;
    }

    private void expectTableCloseCheck(final Tournament tournament, final int result) {
        when(tournamentTableService.getOpenTableCount(new HashSet<BigDecimal>(tournament.getTables()))).thenReturn(result);
    }

    private void expectTableOpen(final Tournament tournament,
                                 final boolean create) throws WalletServiceException {
        final List<TournamentPlayer> tournamentPlayers = playerMap.get(tournament.getTournamentId());

        final Set<TournamentPlayer> playerSet = new HashSet<TournamentPlayer>(tournamentPlayers);

        PlayerGroup playersForTable1 = new PlayerGroup(Arrays.asList(tournamentPlayers.get(0), tournamentPlayers.get(1), tournamentPlayers.get(2)));
        PlayerGroup playersForTable2 = new PlayerGroup(Arrays.asList(tournamentPlayers.get(3), tournamentPlayers.get(4), tournamentPlayers.get(5)));
        PlayerGroup playersForTable3 = new PlayerGroup(Arrays.asList(tournamentPlayers.get(6), tournamentPlayers.get(7)));

        final Collection<PlayerGroup> allocatedPlayers = new ArrayList<PlayerGroup>();
        allocatedPlayers.add(playersForTable1);
        allocatedPlayers.add(playersForTable2);
        allocatedPlayers.add(playersForTable3);

        when(tableAllocator.allocate(playerSet, 5)).thenReturn(allocatedPlayers);

        List<BigDecimal> tables = new ArrayList<BigDecimal>(Arrays.asList(TABLE_ID_1, TABLE_ID_2, TABLE_ID_3));

        if (create) {
            when(tournamentTableService.createTables(eq(3), eq(tournament.retrieveTournamentGameType()),
                    (BigDecimal) any(), eq(TABLE_CLIENT_ID), eq(TABLE_PARTNER_ID), eq(tournament.getName()))).thenReturn(
                    tables);
        } else {
            tournament.setTables(tables);
        }

        // this is a cover for assertions - make sure you mock correctly if you want a different response
        when(tournamentTableService.getOpenTableCount(new HashSet<BigDecimal>(tables))).thenReturn(0);
    }

    private void verifyTableOpen(final Tournament tournament) {
        final List<TournamentPlayer> tournamentPlayers = playerMap.get(tournament.getTournamentId());

        PlayerGroup playersForTable1 = new PlayerGroup(Arrays.asList(tournamentPlayers.get(0), tournamentPlayers.get(1), tournamentPlayers.get(2)));
        PlayerGroup playersForTable2 = new PlayerGroup(Arrays.asList(tournamentPlayers.get(3), tournamentPlayers.get(4), tournamentPlayers.get(5)));
        PlayerGroup playersForTable3 = new PlayerGroup(Arrays.asList(tournamentPlayers.get(6), tournamentPlayers.get(7)));

        verify(tournamentTableService).reopenAndStartNewGame(TABLE_ID_1, playersForTable1, GAME_VARIATION_ID, TABLE_CLIENT_ID);
        verify(tournamentTableService).reopenAndStartNewGame(TABLE_ID_2, playersForTable2, GAME_VARIATION_ID, TABLE_CLIENT_ID);
        verify(tournamentTableService).reopenAndStartNewGame(TABLE_ID_3, playersForTable3, GAME_VARIATION_ID, TABLE_CLIENT_ID);
    }

    private long convertMinutesToMiliseconds(int minutes) {
        assert minutes >= 0;

        return minutes * 60 * 1000;
    }
}
