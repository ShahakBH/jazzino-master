package com.yazino.platform.model.tournament;

import com.yazino.game.api.GamePlayer;
import com.yazino.game.api.PlayerAtTableInformation;
import com.yazino.game.api.time.SettableTimeSource;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.processor.tournament.TournamentHost;
import com.yazino.platform.processor.tournament.TournamentPlayerStatisticPublisher;
import com.yazino.platform.repository.tournament.TournamentRepository;
import com.yazino.platform.service.account.InternalWalletService;
import com.yazino.platform.service.tournament.TournamentTableService;
import com.yazino.platform.tournament.TournamentStatus;
import com.yazino.platform.tournament.TournamentType;
import com.yazino.platform.tournament.TournamentVariationRound;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TournamentLeaderboardTest {

    private static final BigDecimal TOURNAMENT_ID = BigDecimal.valueOf(17L);
    private static final BigDecimal TABLE_ID = BigDecimal.valueOf(1);
    private static final BigDecimal PLAYER_1_ID = TABLE_ID;
    private static final BigDecimal PLAYER_2_ID = BigDecimal.valueOf(2);
    private static final BigDecimal PLAYER_3_ID = BigDecimal.valueOf(3);
    private static final BigDecimal DEFAULT_BALANCE = BigDecimal.TEN;

    @Mock
    private TournamentHost tournamentHost;
    @Mock
    private InternalWalletService internalWalletService;
    @Mock
    private TournamentTableService tableService;
    @Mock
    private Tournament tournament;
    @Mock
    private TournamentPlayers tournamentPlayers;
    @Mock
    private TournamentRepository tournamentRepository;

    private TournamentPlayer player1;
    private TournamentPlayer player2;
    private TournamentPlayer player3;

    private Map<BigDecimal, TournamentPlayer> playerMap;
    private List<PlayerAtTableInformation> playersAtTable;
    private List<BigDecimal> tables;

    private TournamentLeaderboard underTest;

    @Before
    public void setUp() {
        when(tournament.retrievePlayers()).thenReturn(tournamentPlayers);

        when(tournamentHost.getTournamentRepository()).thenReturn(tournamentRepository);
        when(tournamentHost.getInternalWalletService()).thenReturn(internalWalletService);
        when(tournamentHost.getTableService()).thenReturn(tableService);
        when(tournamentHost.getTimeSource()).thenReturn(new SettableTimeSource());
        final DocumentDispatcher documentDispatcher = mock(DocumentDispatcher.class);
        when(tournamentHost.getDocumentDispatcher()).thenReturn(documentDispatcher);
        final TournamentPlayerStatisticPublisher playerStatisticPublisher = mock(TournamentPlayerStatisticPublisher.class);
        when(tournamentHost.getTournamentPlayerStatisticPublisher()).thenReturn(playerStatisticPublisher);

        when(tournament.getTournamentVariationTemplate()).thenReturn(new TournamentVariationTemplateBuilder()
                .setTournamentVariationTemplateId(BigDecimal.valueOf(1L))
                .setTemplateName("Bla")
                .setMinPlayers(1)
                .setMaxPlayers(10)
                .setGameType("BLACKJACK")
                .setTournamentType(TournamentType.PRESET)
                .toTemplate());
        when(tournament.getName()).thenReturn("tournament-name");
        when(tournament.getTournamentId()).thenReturn(TOURNAMENT_ID);
        when(tournament.getPartnerId()).thenReturn("partner-id");
        when(tournament.getPlayers()).thenReturn(tournamentPlayers);
        when(tournamentPlayers.size()).thenReturn(10);

        underTest = new TournamentLeaderboard(TOURNAMENT_ID);

        player1 = tournamentPlayer(PLAYER_1_ID);
        player2 = tournamentPlayer(PLAYER_2_ID);
        player3 = tournamentPlayer(PLAYER_3_ID);

        playerMap = mapOfPlayers(player1, player2, player3);

        playersAtTable = newArrayList(playerAtTable(PLAYER_1_ID),
                playerAtTable(PLAYER_2_ID),
                playerAtTable(PLAYER_3_ID));

        tables = asList(TABLE_ID);

        when(tournament.getTables()).thenReturn(tables);

        final TournamentVariationRound defaultRound = new TournamentVariationRound(
                1, 0l, 0l, BigDecimal.ZERO, "props", BigDecimal.ONE, "1");
        when(tournament.retrieveCurrentRound()).thenReturn(defaultRound);
    }

    @Test
    public void leaderboardNotUpdateForInvalidTournamentStatus() {
        when(tournament.getTournamentStatus()).thenReturn(TournamentStatus.SETTLED);

        underTest.updateLeaderboard(tournament, tournamentHost, true, true);
    }

    @Test
    public void runningTournamentsHaveLeaderboardsUpdated() throws WalletServiceException {
        expectTableCalls(playersAtTable, tables.size());
        expectHeaderCalls(TournamentStatus.RUNNING);
        for (final BigDecimal playerId : playerMap.keySet()) {
            final TournamentPlayer player = playerMap.get(playerId);
            player.setStack(BigDecimal.ZERO);
            when(tournamentHost.isPlayerOnline(player.getPlayerId())).thenReturn(true);
        }
        expectBalanceQueries(playerMap.keySet());

        underTest.updateLeaderboard(tournament, tournamentHost, true, true);
        assertEquals(DEFAULT_BALANCE, player1.getStack());
        assertEquals(DEFAULT_BALANCE, player2.getStack());
    }

    @Test
    public void onlinePlayersNotEliminatedIfNotSpecified() throws WalletServiceException {
        expectTableCalls(playersAtTable, tables.size());
        expectHeaderCalls(TournamentStatus.RUNNING);
        for (final BigDecimal playerId : playerMap.keySet()) {
            final TournamentPlayer player = playerMap.get(playerId);
            player.setStack(BigDecimal.ZERO);
        }
        expectBalanceQueries(playerMap.keySet());

        underTest.updateLeaderboard(tournament, tournamentHost, true, false);
        assertEquals(DEFAULT_BALANCE, player1.getStack());
        assertEquals(DEFAULT_BALANCE, player2.getStack());
    }

    @Test
    public void runningTournamentsHaveLeaderboardsUpdatedForDraws() throws WalletServiceException {
        playerMap.clear();
        playersAtTable.clear();
        final Map<BigDecimal, BigDecimal> balances = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            final BigDecimal playerId = bd(i);
            final TournamentPlayer player = new TournamentPlayer(playerId, "player " + i);
            player.setAccountId(playerId);
            final BigDecimal playerBalance = bd(i + 100);
            balances.put(player.getAccountId(), playerBalance);
            player.setStatus(TournamentPlayerStatus.ACTIVE);
            playerMap.put(playerId, player);
            playersAtTable.add(playerAtTable(playerId));
            when(tournamentHost.isPlayerOnline(player.getPlayerId())).thenReturn(true);
        }
        when(internalWalletService.getBalances(playerMap.keySet())).thenReturn(balances);
        expectTableCalls(playersAtTable, tables.size());
        expectHeaderCalls(TournamentStatus.RUNNING);

        underTest.updateLeaderboard(tournament, tournamentHost, true, true);

        int rank = 1;
        for (int i = 9; i >= 0; i--) {
            final BigDecimal playerId = bd(i);
            assertEquals(rank++, playerMap.get(playerId).getLeaderboardPosition(), 0);
        }
    }

    @Test
    public void finishedTournamentsHaveLeaderboardsAndStateUpdated() throws WalletServiceException {
        expectTableCalls(playersAtTable, tables.size());
        expectHeaderCalls(TournamentStatus.RUNNING);
        expectPlayerOnlineCalls(playerMap.keySet());
        expectBalanceQueries(playerMap.keySet());

        underTest.updateLeaderboard(tournament, tournamentHost, true, true);
    }

    @Test
    public void eliminatePlayersNoLongerActiveWithNotEnoughChips() throws WalletServiceException {
        player3.setStatus(TournamentPlayerStatus.ACTIVE);
        final HashSet<PlayerAtTableInformation> activePlayers = new HashSet<>(
                asList(playerAtTable(PLAYER_1_ID), playerAtTable(PLAYER_2_ID)));

        expectTableCalls(activePlayers, tables.size());
        expectHeaderCalls(TournamentStatus.RUNNING);
        expectPlayerOnlineCalls(playerMap.keySet());

        expectBalanceQueries(playerMap.keySet());

        underTest.updateLeaderboard(tournament, tournamentHost, true, true);
        assertNull(player3.getStack());
        assertEquals(TournamentPlayer.EliminationReason.NO_CHIPS, player3.getEliminationReason());
    }

    @Test
    public void eliminatePlayersNoLongerActiveKickedByGame() throws WalletServiceException {
        player3.setStatus(TournamentPlayerStatus.ACTIVE);
        player3.setStack(BigDecimal.TEN);
        final HashSet<PlayerAtTableInformation> activePlayers = new HashSet<>(
                asList(playerAtTable(PLAYER_1_ID), playerAtTable(PLAYER_2_ID)));
        final TournamentVariationRound round = mock(TournamentVariationRound.class);
        when(tournament.retrieveCurrentRound()).thenReturn(round);
        when(round.getMinimumBalance()).thenReturn(BigDecimal.ZERO);

        expectTableCalls(activePlayers, tables.size());
        expectHeaderCalls(TournamentStatus.RUNNING);
        expectPlayerOnlineCalls(playerMap.keySet());

        expectBalanceQueries(playerMap.keySet());

        underTest.updateLeaderboard(tournament, tournamentHost, true, true);

        assertNull(player3.getStack());
        assertEquals(TournamentPlayer.EliminationReason.KICKED_OUT_BY_GAME, player3.getEliminationReason());
    }

    @Test
    public void updateActivePlayersWithNewProperties() throws WalletServiceException {
        player3.setStatus(TournamentPlayerStatus.ACTIVE);

        final Map<String, String> properties = new HashMap<>();
        properties.put("property1", "value1");
        final PlayerAtTableInformation player1Info = new PlayerAtTableInformation(
                new GamePlayer(PLAYER_1_ID, null, "player" + PLAYER_1_ID), properties);

        final HashSet<PlayerAtTableInformation> activePlayers = new HashSet<>(
                asList(player1Info, playerAtTable(PLAYER_2_ID)));

        expectTableCalls(activePlayers, tables.size());
        expectHeaderCalls(TournamentStatus.RUNNING);
        expectPlayerOnlineCalls(playerMap.keySet());

        expectBalanceQueries(playerMap.keySet());

        underTest.updateLeaderboard(tournament, tournamentHost, true, true);

        assertThat(player1.getProperties(), is(equalTo(properties)));
    }

    @Test
    public void doNotEliminateInactivePlayersIfFlagIsFalse() throws WalletServiceException {
        player3.setStatus(TournamentPlayerStatus.ACTIVE);

        expectHeaderCalls(TournamentStatus.RUNNING);
        expectPlayerOnlineCalls(playerMap.keySet());
        expectBalanceQueries(playerMap.keySet());

        underTest.updateLeaderboard(tournament, tournamentHost, false, true);
    }

    @Test
    public void doNotEliminateInactivePlayersIfNotAllTablesAreOpen() throws WalletServiceException {
        player3.setStatus(TournamentPlayerStatus.ACTIVE);
        final HashSet<PlayerAtTableInformation> activePlayers = new HashSet<>(
                asList(playerAtTable(PLAYER_1_ID), playerAtTable(PLAYER_2_ID)));

        expectTableCalls(activePlayers, tables.size() - 1);
        expectHeaderCalls(TournamentStatus.RUNNING);
        expectPlayerOnlineCalls(playerMap.keySet());
        expectBalanceQueries(playerMap.keySet());

        underTest.updateLeaderboard(tournament, tournamentHost, true, true);
    }

    @Test
    public void eliminatePlayersNotOnline() throws WalletServiceException {
        expectTableCalls(playersAtTable, tables.size());
        expectHeaderCalls(TournamentStatus.RUNNING);
        for (BigDecimal playerId : playerMap.keySet()) {
            final boolean isOnline = playerId.longValue() % 2 == 0;
            when(tournamentHost.isPlayerOnline(playerId)).thenReturn(isOnline);
        }
        expectBalanceQueries(playerMap.keySet());

        underTest.updateLeaderboard(tournament, tournamentHost, true, true);

        for (BigDecimal playerId : playerMap.keySet()) {
            if (playerId.longValue() % 2 != 0) {
                assertEquals(TournamentPlayerStatus.ELIMINATED, playerMap.get(playerId).getStatus());
            } else {
                assertEquals(TournamentPlayerStatus.ACTIVE, playerMap.get(playerId).getStatus());
            }
        }
    }

    @Test
    public void eliminatePlayersBelowMinimumBalance() throws WalletServiceException {
        expectTableCalls(playersAtTable, tables.size());
        expectHeaderCalls(TournamentStatus.ON_BREAK);
        when(tournament.retrieveNextRound()).thenReturn(new TournamentVariationRound(1, 1l, 1l, BigDecimal.ONE, "client", BigDecimal.ONE, "1"));
        expectPlayerOnlineCalls(playerMap.keySet());
        final Map<BigDecimal, BigDecimal> balances = new HashMap<>();
        for (BigDecimal playerId : playerMap.keySet()) {
            final boolean belowMinimum = playerId.longValue() % 2 == 0;
            if (belowMinimum) {
                balances.put(playerId, BigDecimal.ZERO);
            } else {
                balances.put(playerId, DEFAULT_BALANCE);
            }
        }
        when(internalWalletService.getBalances(playerMap.keySet())).thenReturn(balances);

        underTest.updateLeaderboard(tournament, tournamentHost, true, true);

        for (BigDecimal playerId : playerMap.keySet()) {
            if (playerId.longValue() % 2 == 0) {
                assertEquals(TournamentPlayerStatus.ELIMINATED, playerMap.get(playerId).getStatus());
            } else {
                assertEquals(TournamentPlayerStatus.ACTIVE, playerMap.get(playerId).getStatus());
            }
        }
    }

    @Test
    public void activePlayerCountIsCorrectlyCalculated() {
        underTest.setActivePlayers(asList(
                new TournamentPlayer(bd(11), "bob", bd(12), TournamentPlayerStatus.ACTIVE),
                new TournamentPlayer(bd(21), "fred", bd(22), TournamentPlayerStatus.ACTIVE),
                new TournamentPlayer(bd(31), "sam", bd(32), TournamentPlayerStatus.ELIMINATED),
                new TournamentPlayer(bd(41), "bill", bd(42), TournamentPlayerStatus.ACTIVE)));

        assertEquals(3, underTest.getActivePlayerCount());
    }

    private PlayerAtTableInformation playerAtTable(final BigDecimal id) {
        return new PlayerAtTableInformation(new GamePlayer(id, null, "player" + id), Collections.<String, String>emptyMap());
    }

    private void expectPlayerOnlineCalls(Set<BigDecimal> playerIds) {
        for (BigDecimal playerId : playerIds) {
            when(tournamentHost.isPlayerOnline(playerId)).thenReturn(true);
        }
    }

    private void expectBalanceQueries(final Collection<BigDecimal> players) throws WalletServiceException {
        final Map<BigDecimal, BigDecimal> result = new HashMap<>();
        for (BigDecimal playerId : players) {
            result.put(playerId, DEFAULT_BALANCE);
        }
        when(internalWalletService.getBalances(players)).thenReturn(result);
    }

    private void expectTableCalls(final Collection<PlayerAtTableInformation> activePlayers,
                                  final int openTableCount) {
        when(tableService.getActivePlayers(tables)).thenReturn(new HashSet<>(activePlayers));
        when(tableService.getOpenTableCount(tables)).thenReturn(openTableCount);
    }

    private void expectHeaderCalls(TournamentStatus status) {
        when(tournament.getTournamentStatus()).thenReturn(status);
        when(tournamentPlayers.getByStatus(TournamentPlayerStatus.ACTIVE)).thenReturn(
                new HashSet<>(playerMap.values()));
    }

    private BigDecimal bd(long value) {
        return BigDecimal.valueOf(value);
    }

    private Map<BigDecimal, TournamentPlayer> mapOfPlayers(final TournamentPlayer... players) {
        final Map<BigDecimal, TournamentPlayer> playerMap = new HashMap<>();
        for (TournamentPlayer player : players) {
            playerMap.put(player.getPlayerId(), player);
        }
        return playerMap;
    }

    private TournamentPlayer tournamentPlayer(final BigDecimal playerId) {
        final TournamentPlayer player = new TournamentPlayer(playerId, "player " + playerId);
        player.setAccountId(playerId);
        player.setStatus(TournamentPlayerStatus.ACTIVE);
        player.setLeaderboardPosition(playerId.intValue());
        return player;
    }
}
