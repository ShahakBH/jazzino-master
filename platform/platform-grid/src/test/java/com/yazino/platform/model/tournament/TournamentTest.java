package com.yazino.platform.model.tournament;

import com.yazino.game.api.time.SettableTimeSource;
import com.yazino.platform.Platform;
import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.session.PlayerSession;
import com.yazino.platform.model.table.Client;
import com.yazino.platform.processor.tournament.SingleTableAllocatorFactory;
import com.yazino.platform.processor.tournament.TableAllocator;
import com.yazino.platform.processor.tournament.TournamentHost;
import com.yazino.platform.processor.tournament.TournamentPlayerStatisticPublisher;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import com.yazino.platform.repository.tournament.TournamentRepository;
import com.yazino.platform.service.account.InternalWalletService;
import com.yazino.platform.service.audit.AuditLabelFactory;
import com.yazino.platform.service.tournament.TournamentTableService;
import com.yazino.platform.tournament.*;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.*;

import static com.yazino.platform.Partner.YAZINO;
import static com.yazino.platform.account.TransactionContext.transactionContext;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TournamentTest {
    private static final BigDecimal GAME_VARIATION_TEMPLATE_ID = BigDecimal.valueOf(3234232L);
    private static final String TOURNAMENT_NAME = "T1test";
    private static final String PARTNER_ID = "INTERNAL";
    private static final String CLIENT_ID = "Red Blackjack";
    private static final BigDecimal TOURNAMENT_ID = BigDecimal.valueOf(18L);
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(43345);
    private static final BigDecimal PLAYER_ACCOUNT_ID = BigDecimal.valueOf(8989);
    private static final BigDecimal TOURNAMENT_PLAYER_ACCOUNT_ID = BigDecimal.valueOf(18934L);
    private static final String AUDIT_LABEL = "bob";
    private static final BigDecimal MINIMUM_ROUND_BALANCE = BigDecimal.valueOf(12);
    private static final int ROUND_END_INTERVAL = 5000;
    private static final int ROUND_LENGTH = 2000;
    private static final String PLAYER_NAME = "player name";
    private static final long CANCELLATION_EXPIRY_DELAY = 170000L;
    private static final BigDecimal tableId1 = BigDecimal.valueOf(18834L);
    private static final BigDecimal tableId2 = BigDecimal.valueOf(5433423L);
    private static final BigDecimal tableId3 = BigDecimal.valueOf(2332939L);
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(3141592);

    @Mock
    private AuditLabelFactory auditor;
    @Mock
    private TournamentTableService tournamentTableService;
    @Mock
    private TableAllocator tableAllocator;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private PlayerSessionRepository playerSessionRepository;
    @Mock
    private TournamentLeaderboard tournamentLeaderboard;
    @Mock
    private InternalWalletService internalWalletService;

    private SettableTimeSource timeSource;
    private TournamentHost tournamentHost;

    @Before
    public void setUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
        timeSource = new SettableTimeSource();
        timeSource.setMillis(System.currentTimeMillis());

        when(tournamentTableService.findClientById(eq(CLIENT_ID))).thenReturn(new Client(CLIENT_ID, 5, null, null, null));
        when(playerSessionRepository.findAllByPlayer(PLAYER_ID)).thenReturn(asList(aPlayerSession()));

        tournamentHost = new TournamentHost(timeSource,
                internalWalletService,
                tournamentTableService,
                mock(TournamentRepository.class),
                new SingleTableAllocatorFactory(tableAllocator),
                playerRepository,
                playerSessionRepository,
                mock(DocumentDispatcher.class),
                mock(TournamentPlayerStatisticPublisher.class));

        tournamentHost.setCancellationExpiryDelay(CANCELLATION_EXPIRY_DELAY);
    }

    @Test
    public void startTournamentWorksWithSitNGoTournament() throws WalletServiceException {
        final TournamentVariationTemplate template = constructTournamentVariationTemplate(BigDecimal.valueOf(10),
                BigDecimal.valueOf(1), TournamentType.SITNGO);

        final List<TournamentPlayer> tournamentPlayers = buildTournamentPlayers(5, TournamentPlayerStatus.ACTIVE);
        final Tournament tournament = constructTournament(template, TournamentStatus.REGISTERING, tournamentPlayers);
        final Set<TournamentPlayer> playerSet = new HashSet<>(tournamentPlayers);

        PlayerGroup playersForTable1 = new PlayerGroup(asList(tournamentPlayers.get(0), tournamentPlayers.get(1), tournamentPlayers.get(2)));
        PlayerGroup playersForTable2 = new PlayerGroup(asList(tournamentPlayers.get(3), tournamentPlayers.get(4)));

        final Collection<PlayerGroup> allocatedPlayers = new ArrayList<>();
        allocatedPlayers.add(playersForTable1);
        allocatedPlayers.add(playersForTable2);

        when(tableAllocator.allocate(playerSet, 5)).thenReturn(allocatedPlayers);
        when(tournamentTableService.createTables(2, tournament.retrieveTournamentGameType(),
                GAME_VARIATION_TEMPLATE_ID, CLIENT_ID, PARTNER_ID, tournament.getName())).thenReturn(asList(tableId1, tableId2));

        tournament.start(tournamentHost);

        assertEquals(TournamentStatus.RUNNING, tournament.getTournamentStatus());
        assertEquals(new Integer(0), tournament.getCurrentRoundIndex());

        verify(tournamentTableService).reopenAndStartNewGame(tableId2, playersForTable2, GAME_VARIATION_TEMPLATE_ID, CLIENT_ID);
        verify(tournamentTableService).reopenAndStartNewGame(tableId1, playersForTable1, GAME_VARIATION_TEMPLATE_ID, CLIENT_ID);
    }

    @Test
    public void startTournamentWorksWithPresetTournament() throws WalletServiceException {
        final TournamentVariationTemplate template = constructTournamentVariationTemplate(BigDecimal.valueOf(10),
                BigDecimal.valueOf(1), TournamentType.PRESET);

        final List<TournamentPlayer> tournamentPlayers = buildTournamentPlayers(8, TournamentPlayerStatus.ACTIVE);
        final Tournament tournament = constructTournament(
                template, TournamentStatus.REGISTERING, null, tournamentPlayers, BigDecimal.ZERO);
        tournament.setStartTimeStamp(new DateTime(timeSource.getCurrentTimeStamp() - 1000));

        final Set<TournamentPlayer> playerSet = new HashSet<>(tournamentPlayers);

        PlayerGroup playersForTable1 = new PlayerGroup(asList(tournamentPlayers.get(0), tournamentPlayers.get(1), tournamentPlayers.get(2)));
        PlayerGroup playersForTable2 = new PlayerGroup(asList(tournamentPlayers.get(3), tournamentPlayers.get(4), tournamentPlayers.get(5)));
        PlayerGroup playersForTable3 = new PlayerGroup(asList(tournamentPlayers.get(6), tournamentPlayers.get(7)));

        final Collection<PlayerGroup> allocatedPlayers = new ArrayList<>();
        allocatedPlayers.add(playersForTable1);
        allocatedPlayers.add(playersForTable2);
        allocatedPlayers.add(playersForTable3);

        when(tableAllocator.allocate(eq(playerSet), eq(5))).thenReturn(allocatedPlayers);

        when(tournamentTableService.createTables(eq(3), eq(tournament.retrieveTournamentGameType()),
                eq(GAME_VARIATION_TEMPLATE_ID), eq(CLIENT_ID), eq(PARTNER_ID), eq(tournament.getName()))).thenReturn(
                asList(tableId1, tableId2, tableId3));

        tournament.start(tournamentHost);

        assertEquals(TournamentStatus.RUNNING, tournament.getTournamentStatus());
        assertEquals(new Integer(0), tournament.getCurrentRoundIndex());

        verify(tournamentTableService).reopenAndStartNewGame(eq(tableId1), eq(playersForTable1), eq(GAME_VARIATION_TEMPLATE_ID), eq(CLIENT_ID));
        verify(tournamentTableService).reopenAndStartNewGame(eq(tableId2), eq(playersForTable2), eq(GAME_VARIATION_TEMPLATE_ID), eq(CLIENT_ID));
        verify(tournamentTableService).reopenAndStartNewGame(eq(tableId3), eq(playersForTable3), eq(GAME_VARIATION_TEMPLATE_ID), eq(CLIENT_ID));
    }

    @Test
    public void startTournamentVerifiesStartTimeStamp() {
        final TournamentVariationTemplate template = constructTournamentVariationTemplate(BigDecimal.valueOf(10),
                BigDecimal.valueOf(1), TournamentType.PRESET);

        final Tournament tournament = constructTournament(template, TournamentStatus.REGISTERING, null);
        tournament.setStartTimeStamp(new DateTime(timeSource.getCurrentTimeStamp() + 1000));

        try {
            tournament.start(tournamentHost);
            fail("Expected exception not thrown");
        } catch (IllegalStateException e) {
            // pass
        }

        assertEquals(TournamentStatus.REGISTERING, tournament.getTournamentStatus());
    }

    @Test
    public void startTournamentCancelsTournamentIfBelowMinimumPlayers() {
        final TournamentVariationTemplate template = constructTournamentVariationTemplate(BigDecimal.valueOf(10),
                BigDecimal.valueOf(1), null, null, TournamentType.PRESET, 5, 10);

        final Set<TournamentPlayer> tournamentPlayers = new HashSet<>();
        for (int i = 0; i < 2; ++i) {
            tournamentPlayers.add(new TournamentPlayer(
                    BigDecimal.valueOf(i), "player " + i, BigDecimal.valueOf(i), TournamentPlayerStatus.ACTIVE));
        }
        final Tournament tournament = constructTournament(
                template, TournamentStatus.REGISTERING, tournamentPlayers);

        tournament.start(tournamentHost);

        assertEquals(TournamentStatus.CANCELLING, tournament.getTournamentStatus());
    }

    @Test
    public void startTournamentVerifiesMaximumPlayers() {
        final TournamentVariationTemplate template = constructTournamentVariationTemplate(BigDecimal.valueOf(10),
                BigDecimal.valueOf(1), null, null, TournamentType.PRESET, 5, 5);

        final Set<TournamentPlayer> tournamentPlayers = new HashSet<>();
        for (int i = 0; i < 6; ++i) {
            tournamentPlayers.add(new TournamentPlayer(
                    BigDecimal.valueOf(i), "player " + i, BigDecimal.valueOf(i), TournamentPlayerStatus.ACTIVE));
        }

        final Tournament tournament = constructTournament(
                template, TournamentStatus.REGISTERING, tournamentPlayers);

        try {
            tournament.start(tournamentHost);
            fail("Expected exception not thrown");
        } catch (IllegalStateException e) {
            // pass
        }

        assertEquals(TournamentStatus.REGISTERING, tournament.getTournamentStatus());
    }

    @Test
    public void startTournamentVerifiesCurrentState() {
        final TournamentVariationTemplate template = constructTournamentVariationTemplate(BigDecimal.valueOf(10),
                BigDecimal.valueOf(1), TournamentType.PRESET);

        final Set<TournamentPlayer> tournamentPlayers = new HashSet<>();
        for (int i = 0; i < 2; ++i) {
            tournamentPlayers.add(new TournamentPlayer(
                    BigDecimal.valueOf(i), "player " + i, BigDecimal.valueOf(i), TournamentPlayerStatus.ACTIVE));
        }

        final Tournament tournament = constructTournament(
                template, TournamentStatus.RUNNING, tournamentPlayers);

        try {
            tournament.start(tournamentHost);
            fail("Expected exception not thrown");
        } catch (IllegalStateException e) {
            // pass
        }

        assertEquals(TournamentStatus.RUNNING, tournament.getTournamentStatus());
    }

    @Test
    public void startTournamentThrowsExceptionIfNoRoundsPresent() {
        final TournamentVariationTemplate template = new TournamentVariationTemplateBuilder()
                .setTournamentVariationTemplateId(BigDecimal.valueOf(1L))
                .setTemplateName("Bla")
                .setMinPlayers(1)
                .setMaxPlayers(10)
                .setGameType("BLACKJACK")
                .setTournamentType(TournamentType.PRESET)
                .toTemplate();

        final Set<TournamentPlayer> tournamentPlayers = new HashSet<>(buildTournamentPlayers(2, TournamentPlayerStatus.ACTIVE));
        final Tournament tournament = constructTournament(template, TournamentStatus.ANNOUNCED, tournamentPlayers);

        try {
            tournament.start(tournamentHost);
            fail("Expected exception not thrown");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("has been created with no rounds"));
        }

        assertEquals(TournamentStatus.ANNOUNCED, tournament.getTournamentStatus());
    }

    @Test
    public void startRoundVerifiesCurrentState() {
        final TournamentVariationTemplate template = constructTournamentVariationTemplate(BigDecimal.valueOf(10),
                BigDecimal.valueOf(1), TournamentType.PRESET);

        final Tournament tournament = constructTournament(template, TournamentStatus.RUNNING, null);
        final List<BigDecimal> tableIds = new ArrayList<>();
        tournament.setTables(tableIds);
        when(tournamentLeaderboard.updateLeaderboard(tournament, tournamentHost, false, true)).thenReturn(false);
        when(tournamentTableService.getOpenTableCount(eq(new HashSet<>(tableIds)))).thenReturn(0);

        try {
            tournament.startRound(tournamentHost);
            fail("Expected exception not thrown");
        } catch (IllegalStateException e) {
            // pass
        }

        assertEquals(TournamentStatus.RUNNING, tournament.getTournamentStatus());
    }

    @Test
    public void startRoundThrowsExceptionIfMaxRoundsExceeded() {
        final TournamentVariationTemplate template = constructTournamentVariationTemplate(BigDecimal.valueOf(10),
                BigDecimal.valueOf(1), TournamentType.PRESET);

        final Tournament tournament = constructTournament(
                template, TournamentStatus.ON_BREAK, 2, null, BigDecimal.ZERO);
        List<BigDecimal> tableIds = new ArrayList<>();
        tournament.setTables(tableIds);
        when(tournamentLeaderboard.updateLeaderboard(tournament, tournamentHost, false, true)).thenReturn(false);
        when(tournamentTableService.getOpenTableCount(eq(new HashSet<>(tableIds)))).thenReturn(0);

        try {
            tournament.startRound(tournamentHost);
            fail("Expected exception not thrown");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("is trying to incremented past max number"));
        }
        assertEquals(TournamentStatus.ON_BREAK, tournament.getTournamentStatus());
    }

    @Test
    public void startRoundReopensExistingTables() throws WalletServiceException {
        final TournamentVariationTemplate template = constructTournamentVariationTemplate(BigDecimal.valueOf(10),
                BigDecimal.valueOf(1), TournamentType.SITNGO);
        final List<BigDecimal> tables = new ArrayList<>(asList(tableId1, tableId2));

        final List<TournamentPlayer> tournamentPlayers = buildTournamentPlayers(5, TournamentPlayerStatus.ADDITION_PENDING);
        final Set<TournamentPlayer> playerSet = new HashSet<>(tournamentPlayers);

        final Tournament tournament = constructTournament(
                template, TournamentStatus.ON_BREAK, 0, tournamentPlayers, BigDecimal.ZERO);
        tournament.setTables(tables);

        PlayerGroup playersForTable1 = new PlayerGroup(asList(tournamentPlayers.get(0), tournamentPlayers.get(1), tournamentPlayers.get(2)));
        PlayerGroup playersForTable2 = new PlayerGroup(asList(tournamentPlayers.get(3), tournamentPlayers.get(4)));

        final Collection<PlayerGroup> allocatedPlayers = new ArrayList<>();
        allocatedPlayers.add(playersForTable1);
        allocatedPlayers.add(playersForTable2);

        when(internalWalletService.getBalance(any(BigDecimal.class))).thenReturn(BigDecimal.valueOf(20));
        when(tournamentLeaderboard.updateLeaderboard(tournament, tournamentHost, false, true)).thenReturn(false);
        when(tableAllocator.allocate(eq(playerSet), eq(5))).thenReturn(allocatedPlayers);

        when(tournamentLeaderboard.getActivePlayers()).thenReturn(playerSet);

        // this will only get called if assertions are on
        when(tournamentTableService.getOpenTableCount(eq(new HashSet<>(tables)))).thenReturn(0);

        tournament.startRound(tournamentHost);

        assertEquals(TournamentStatus.RUNNING, tournament.getTournamentStatus());
        assertEquals(new Integer(1), tournament.getCurrentRoundIndex());

        verify(tournamentTableService).reopenAndStartNewGame(eq(tableId1), eq(playersForTable1), eq(GAME_VARIATION_TEMPLATE_ID), eq(CLIENT_ID));
        verify(tournamentTableService).reopenAndStartNewGame(eq(tableId2), eq(playersForTable2), eq(GAME_VARIATION_TEMPLATE_ID), eq(CLIENT_ID));
    }

    @Test
    public void startRoundUpdatesUsedTables() throws WalletServiceException {
        final TournamentVariationTemplate template = constructTournamentVariationTemplate(BigDecimal.valueOf(10),
                BigDecimal.valueOf(1), TournamentType.SITNGO);
        final List<BigDecimal> tables = new ArrayList<>(asList(tableId1, tableId2));

        final List<TournamentPlayer> tournamentPlayers = buildTournamentPlayers(2, TournamentPlayerStatus.ADDITION_PENDING);
        final Set<TournamentPlayer> playerSet = new HashSet<>(tournamentPlayers);

        final Tournament tournament = constructTournament(
                template, TournamentStatus.ON_BREAK, 0, tournamentPlayers, BigDecimal.ZERO);
        tournament.setTables(tables);

        PlayerGroup playersForTable1 = new PlayerGroup(asList(tournamentPlayers.get(0), tournamentPlayers.get(1)));

        final Collection<PlayerGroup> allocatedPlayers = new ArrayList<>();
        allocatedPlayers.add(playersForTable1);

        when(internalWalletService.getBalance(any(BigDecimal.class))).thenReturn(BigDecimal.valueOf(20));
        when(tournamentLeaderboard.updateLeaderboard(tournament, tournamentHost, false, true)).thenReturn(false);
        when(tableAllocator.allocate(eq(playerSet), eq(5))).thenReturn(allocatedPlayers);

        when(tournamentLeaderboard.getActivePlayers()).thenReturn(playerSet);

        // this will only get called if assertions are on
        when(tournamentTableService.getOpenTableCount(eq(new HashSet<>(tables)))).thenReturn(0);

        tournament.startRound(tournamentHost);

        assertEquals(TournamentStatus.RUNNING, tournament.getTournamentStatus());
        assertEquals(new Integer(1), tournament.getCurrentRoundIndex());
        assertEquals(1, tournament.getTables().size());

        verify(tournamentTableService).reopenAndStartNewGame(eq(tableId1), eq(playersForTable1), eq(GAME_VARIATION_TEMPLATE_ID), eq(CLIENT_ID));
    }

    @Test
    public void finishRoundRequestTableClosure() {
        final TournamentVariationTemplate template = constructTournamentVariationTemplate(BigDecimal.valueOf(10),
                BigDecimal.valueOf(1), TournamentType.SITNGO);

        when(tournamentLeaderboard.isInsufficientPlayersPresent()).thenReturn(false);

        final Tournament tournament = constructTournament(template, TournamentStatus.RUNNING, null);
        tournament.setTables(asList(tableId1, tableId2));

        tournament.finishRound(tournamentHost);

        assertEquals(TournamentStatus.WAITING_FOR_CLIENTS, tournament.getTournamentStatus());

        verify(tournamentTableService).requestClosing(eq(new HashSet<>(tournament.getTables())));
    }

    @Test
    public void finishRoundRequestTableClosureAndFinishesTournamentIfInsufficientPlayers() {
        final TournamentVariationTemplate template = constructTournamentVariationTemplate(BigDecimal.valueOf(10),
                BigDecimal.valueOf(1), TournamentType.SITNGO);

        final Tournament tournament = constructTournament(template, TournamentStatus.RUNNING, null);
        tournament.setCurrentRoundIndex(0);
        tournament.setStartTimeStamp(new DateTime(tournamentHost.getTimeSource().getCurrentTimeStamp() - 10));
        final List<BigDecimal> tableIds = asList(tableId1, tableId2);
        tournament.setTables(tableIds);
        tournamentTableService.requestClosing(eq(new HashSet<>(tournament.getTables())));

        when(tournamentTableService.getOpenTableCount(eq(new HashSet<>(tableIds)))).thenReturn(0);

        when(tournamentLeaderboard.isInsufficientPlayersPresent()).thenReturn(true);
        when(tournamentLeaderboard.updateLeaderboard(eq(tournament), eq(tournamentHost), eq(false), eq(false))).thenReturn(true);

        tournament.finishRound(tournamentHost);

        assertEquals(TournamentStatus.FINISHED, tournament.getTournamentStatus());
    }

    @Test
    public void waitingTablesAreTransitionedToOnBreakWhenAllTablesClosed() {
        final TournamentVariationTemplate template = constructTournamentVariationTemplate(BigDecimal.valueOf(10),
                BigDecimal.valueOf(1), TournamentType.SITNGO);

        timeSource.setMillis(System.currentTimeMillis());

        final Tournament tournament = constructTournament(template, TournamentStatus.WAITING_FOR_CLIENTS, null);
        tournament.setStartTimeStamp(new DateTime(timeSource.getCurrentTimeStamp() - 10));
        tournament.setTables(asList(tableId1, tableId2));
        tournament.setCurrentRoundIndex(0);

        // a little messy but more performant, as we shortcut the event process and immediately process the on-break state
        when(tournamentTableService.getOpenTableCount(eq(new HashSet<>(tournament.getTables())))).thenReturn(0);
        when(tournamentLeaderboard.updateLeaderboard(eq(tournament), eq(tournamentHost), eq(false), eq(false))).thenReturn(false);

        tournament.processEvent(tournamentHost);

        assertEquals(TournamentStatus.ON_BREAK, tournament.getTournamentStatus());
    }

    @Test
    public void waitingTablesPollWhileTablesRemainOpen() {
        final TournamentVariationTemplate template = constructTournamentVariationTemplate(BigDecimal.valueOf(10),
                BigDecimal.valueOf(1), TournamentType.SITNGO);

        timeSource.setMillis(System.currentTimeMillis());

        final Tournament tournament = constructTournament(template, TournamentStatus.WAITING_FOR_CLIENTS, null);
        tournament.setTables(asList(tableId1, tableId2));
        when(tournamentTableService.getOpenTableCount(eq(new HashSet<>(tournament.getTables())))).thenReturn(3);

        tournament.processEvent(tournamentHost);

        assertEquals(TournamentStatus.WAITING_FOR_CLIENTS, tournament.getTournamentStatus());
        assertEquals(timeSource.getCurrentTimeStamp() + tournamentHost.getPollDelay(), tournament.getNextEvent().longValue());
    }

    @Test
    public void finishUpdatesState() {
        final TournamentVariationTemplate template = constructTournamentVariationTemplate(BigDecimal.valueOf(10),
                BigDecimal.valueOf(1), TournamentType.SITNGO);

        final Tournament tournament = constructTournament(template, TournamentStatus.ON_BREAK, null);
        tournament.setTables(asList(tableId1, tableId2));

        // only called if assertions are enabled
        when(tournamentTableService.getOpenTableCount(eq(new HashSet<>(tournament.getTables())))).thenReturn(0);

        tournament.finish(tournamentHost);

        assertEquals(TournamentStatus.FINISHED, tournament.getTournamentStatus());
        assertNull(tournament.getNextEvent());
    }

    @Test
    public void finishVerifiesState() {
        final TournamentVariationTemplate template = constructTournamentVariationTemplate(BigDecimal.valueOf(10),
                BigDecimal.valueOf(1), TournamentType.SITNGO);

        final Tournament tournament = constructTournament(template, TournamentStatus.ANNOUNCED, null);
        tournament.setTables(asList(tableId1, tableId2));

        // only called if assertions are enabled
        when(tournamentTableService.getOpenTableCount(eq(new HashSet<>(tournament.getTables())))).thenReturn(0);

        try {
            tournament.finish(tournamentHost);
            fail("Exception exception not thrown");
        } catch (Throwable e) {
            // expected
        }

        assertEquals(TournamentStatus.ANNOUNCED, tournament.getTournamentStatus());
    }

    @Test
    public void cancelRefundsAllPlayers() throws TournamentException, WalletServiceException {
        final BigDecimal entryFee = new BigDecimal("12.45");
        final BigDecimal serviceFee = new BigDecimal("17.55");
        final TournamentVariationTemplate tournamentVariationTemplate = constructTournamentVariationTemplate(entryFee, serviceFee);
        final long time = System.currentTimeMillis();

        timeSource.setMillis(time);

        final TournamentPlayer tournamentPlayer = new TournamentPlayer(
                PLAYER_ID, "Fred", TOURNAMENT_PLAYER_ACCOUNT_ID, TournamentPlayerStatus.ADDITION_PENDING);
        final HashSet<TournamentPlayer> players = new HashSet<>();
        players.add(tournamentPlayer);

        when(playerRepository.findById(PLAYER_ID)).thenReturn(aPlayer());

        final Tournament tournament = constructTournament(tournamentVariationTemplate, TournamentStatus.CANCELLING, players);

        when(auditor.newLabel()).thenReturn(AUDIT_LABEL);

        tournament.cancel(tournamentHost);

        for (TournamentPlayer tplayer : players) {
            verify(internalWalletService).closeAccount(tplayer.getAccountId());
        }
        verify(internalWalletService).postTransaction(PLAYER_ACCOUNT_ID, entryFee.add(serviceFee),
                "Tournament Fees Refund", "Refund of Fees for tournament " + tournament.getName(), TransactionContext.EMPTY);

        assertEquals(TournamentStatus.CANCELLED, tournament.getTournamentStatus());
        assertEquals(Long.valueOf(time + CANCELLATION_EXPIRY_DELAY), tournament.getNextEvent());
    }

    @Test(expected = IllegalStateException.class)
    public void addPlayerToTournamentThrowAnExceptionWhenTheEntryFeeIsBelowZero() throws TournamentException, WalletServiceException {
        final BigDecimal entryFee = new BigDecimal("-45.33");
        final BigDecimal serviceFee = new BigDecimal("0.50");
        final TournamentVariationTemplate tournamentVariationTemplate = constructTournamentVariationTemplate(entryFee, serviceFee);
        final Tournament tournament = constructTournament(tournamentVariationTemplate, TournamentStatus.REGISTERING, null);
        tournament.setSignupStartTimeStamp(new DateTime().minus(1000L));
        when(internalWalletService.createAccount(eq("henrythe7th:TOURNAMENT"))).thenReturn(PLAYER_ACCOUNT_ID);

        tournament.addPlayer(aPlayer(), tournamentHost);
    }

    @Test(expected = IllegalStateException.class)
    public void addPlayerToTournamentThrowAnExceptionWhenTheServiceFeeIsBelowZero() throws TournamentException, WalletServiceException {
        final BigDecimal entryFee = new BigDecimal("45.33");
        final BigDecimal serviceFee = new BigDecimal("-0.50");
        final TournamentVariationTemplate tournamentVariationTemplate = constructTournamentVariationTemplate(entryFee, serviceFee);
        final Tournament tournament = constructTournament(tournamentVariationTemplate, TournamentStatus.REGISTERING, null);
        tournament.setSignupStartTimeStamp(new DateTime().minus(1000L));
        when(internalWalletService.createAccount(eq("henrythe7th:TOURNAMENT"))).thenReturn(PLAYER_ACCOUNT_ID);

        tournament.addPlayer(aPlayer(), tournamentHost);
    }

    @Test
    public void addPlayerToTournamentAddsThePlayer() throws TournamentException, WalletServiceException {
        final BigDecimal entryFee = new BigDecimal("45.33");
        final BigDecimal serviceFee = new BigDecimal("0.50");
        final TournamentVariationTemplate tournamentVariationTemplate = constructTournamentVariationTemplate(entryFee, serviceFee);
        final Tournament tournament = constructTournament(tournamentVariationTemplate, TournamentStatus.REGISTERING, null);
        tournament.setSignupStartTimeStamp(new DateTime().minus(1000L));
        when(internalWalletService.createAccount(eq("henrythe7th:TOURNAMENT"))).thenReturn(PLAYER_ACCOUNT_ID);

        tournament.addPlayer(aPlayer(), tournamentHost);

        assertEquals(1, tournament.playerCount());
        final TournamentPlayer addedPlayer = getPlayerFromTournament(tournament, PLAYER_ID);
        assertNotNull(addedPlayer);
        assertEquals(TournamentPlayerStatus.ACTIVE, addedPlayer.getStatus());
    }

    @Test
    public void addPlayerToTournamentDoesNotAddThePlayerIfTheTransferFails() throws TournamentException, WalletServiceException {
        final BigDecimal entryFee = new BigDecimal("45.33");
        final BigDecimal serviceFee = new BigDecimal("0.50");
        final TournamentVariationTemplate tournamentVariationTemplate = constructTournamentVariationTemplate(entryFee, serviceFee);
        final Tournament tournament = constructTournament(tournamentVariationTemplate, TournamentStatus.REGISTERING, null);
        tournament.setSignupStartTimeStamp(new DateTime().minus(1000L));
        when(internalWalletService.createAccount(eq("henrythe7th:TOURNAMENT"))).thenReturn(PLAYER_ACCOUNT_ID);
        when(internalWalletService.postTransaction(PLAYER_ACCOUNT_ID, BigDecimal.ZERO.subtract(entryFee).subtract(serviceFee),
                "Tournament Fees", "Fees for tournament " + TOURNAMENT_NAME, transactionContext().withSessionId(SESSION_ID).build()))
                .thenThrow(new WalletServiceException("aTestException"));

        try {
            tournament.addPlayer(aPlayer(), tournamentHost);
            fail("Did not throw exception");
        } catch (TournamentException e) {
            // ignored for test
        }

        assertEquals(0, tournament.playerCount());
    }

    @Test
    public void addPlayerToTournamentDoesNotChargeThePlayerWhenTheFeesAreZero() throws TournamentException, WalletServiceException {
        final TournamentVariationTemplate tournamentVariationTemplate = constructTournamentVariationTemplate(BigDecimal.ZERO, null);
        final Tournament tournament = constructTournament(tournamentVariationTemplate, TournamentStatus.REGISTERING, null);
        tournament.setSignupStartTimeStamp(new DateTime().minus(1000L));
        when(internalWalletService.createAccount(eq("henrythe7th:TOURNAMENT"))).thenReturn(PLAYER_ACCOUNT_ID);

        tournament.addPlayer(aPlayer(), tournamentHost);

        verify(internalWalletService).createAccount(eq("henrythe7th:TOURNAMENT"));
        verifyNoMoreInteractions(internalWalletService);
    }

    @Test
    public void addPlayerToTournamentChargesThePlayerTheServiceAndEntryFees() throws TournamentException, WalletServiceException {
        final BigDecimal entryFee = new BigDecimal("45.33");
        final BigDecimal serviceFee = new BigDecimal("0.50");
        final TournamentVariationTemplate tournamentVariationTemplate = constructTournamentVariationTemplate(entryFee, serviceFee);
        final Tournament tournament = constructTournament(tournamentVariationTemplate, TournamentStatus.REGISTERING, null);
        tournament.setSignupStartTimeStamp(new DateTime().minus(1000L));
        when(internalWalletService.createAccount(eq("henrythe7th:TOURNAMENT"))).thenReturn(PLAYER_ACCOUNT_ID);

        tournament.addPlayer(aPlayer(), tournamentHost);

        verify(internalWalletService).postTransaction(PLAYER_ACCOUNT_ID, BigDecimal.ZERO.subtract(entryFee).subtract(serviceFee),
                "Tournament Fees", "Fees for tournament " + TOURNAMENT_NAME, transactionContext().withSessionId(SESSION_ID).build());
    }

    @Test
    public void addPlayerToTournamentChargesThePlayerTheServiceFeeWhenNoEntryFeeIsPresent() throws TournamentException, WalletServiceException {
        final BigDecimal serviceFee = new BigDecimal("0.50");
        final TournamentVariationTemplate tournamentVariationTemplate = constructTournamentVariationTemplate(null, serviceFee);
        final Tournament tournament = constructTournament(tournamentVariationTemplate, TournamentStatus.REGISTERING, null);
        tournament.setSignupStartTimeStamp(new DateTime().minus(1000L));
        when(internalWalletService.createAccount(eq("henrythe7th:TOURNAMENT"))).thenReturn(PLAYER_ACCOUNT_ID);

        tournament.addPlayer(aPlayer(), tournamentHost);

        verify(internalWalletService).postTransaction(PLAYER_ACCOUNT_ID, BigDecimal.ZERO.subtract(serviceFee),
                "Tournament Fees", "Fees for tournament " + TOURNAMENT_NAME, transactionContext().withSessionId(SESSION_ID).build());
    }

    @Test
    public void addPlayerToTournamentChargesThePlayerTheEntryFeeWhenNoServiceFeeIsPresent() throws TournamentException, WalletServiceException {
        final BigDecimal entryFee = new BigDecimal("45.33");
        final TournamentVariationTemplate tournamentVariationTemplate = constructTournamentVariationTemplate(entryFee, null);
        final Tournament tournament = constructTournament(tournamentVariationTemplate, TournamentStatus.REGISTERING, null);
        tournament.setSignupStartTimeStamp(new DateTime().minus(1000L));
        when(internalWalletService.createAccount(eq("henrythe7th:TOURNAMENT"))).thenReturn(PLAYER_ACCOUNT_ID);

        tournament.addPlayer(aPlayer(), tournamentHost);

        verify(internalWalletService).postTransaction(PLAYER_ACCOUNT_ID, BigDecimal.ZERO.subtract(entryFee),
                "Tournament Fees", "Fees for tournament " + TOURNAMENT_NAME, transactionContext().withSessionId(SESSION_ID).build());
    }

    @Test
    public void addPlayerToAllowsAdditionDuringRegisteringPhase() throws TournamentException {
        final TournamentVariationTemplate tournamentVariationTemplate = constructTournamentVariationTemplate(new BigDecimal("45.33"), new BigDecimal("0.50"));
        final Tournament tournament = constructTournament(tournamentVariationTemplate, TournamentStatus.REGISTERING, null);
        tournament.setSignupStartTimeStamp(new DateTime().minus(1000L));

        final List<TournamentStatus> statuses = asList(TournamentStatus.CLOSED, TournamentStatus.RUNNING,
                TournamentStatus.FINISHED, TournamentStatus.ANNOUNCED, TournamentStatus.ANNOUNCED, TournamentStatus.SETTLED,
                TournamentStatus.ON_BREAK);
        for (final TournamentStatus status : statuses) {
            tournament.setTournamentStatus(status);
            try {
                tournament.addPlayer(aPlayer(), tournamentHost);
                fail("Exception not thrown");
            } catch (TournamentException e) {
                // expected
            }
        }
    }

    @Test
    public void removePlayerOnlyAllowsRemovingDuringRegisteringPhase() throws TournamentException {
        final TournamentVariationTemplate tournamentVariationTemplate = constructTournamentVariationTemplate(new BigDecimal("45.33"), new BigDecimal("0.50"));
        final TournamentPlayer tournamentPlayer = new TournamentPlayer(
                PLAYER_ID, PLAYER_NAME, TOURNAMENT_PLAYER_ACCOUNT_ID, TournamentPlayerStatus.ACTIVE);
        final Tournament tournament = constructTournament(tournamentVariationTemplate, TournamentStatus.REGISTERING, asList(tournamentPlayer));
        tournament.setSignupStartTimeStamp(new DateTime().minus(1000L));

        final List<TournamentStatus> statuses = asList(TournamentStatus.CLOSED, TournamentStatus.RUNNING,
                TournamentStatus.FINISHED, TournamentStatus.ANNOUNCED, TournamentStatus.ANNOUNCED, TournamentStatus.SETTLED,
                TournamentStatus.ON_BREAK);
        for (final TournamentStatus status : statuses) {
            tournament.setTournamentStatus(status);
            try {
                tournament.removePlayer(aPlayer(), tournamentHost);
                fail("Exception not thrown for status " + status);
            } catch (TournamentException e) {
                // expected
            }
        }
    }

    @Test
    public void removePlayerFromTournamentDoesNotRefundARefundedPlayer() throws TournamentException {
        final TournamentVariationTemplate tournamentVariationTemplate = constructTournamentVariationTemplate(new BigDecimal("45.33"), new BigDecimal("0.50"));
        final TournamentPlayer tournamentPlayer = new TournamentPlayer(PLAYER_ID, PLAYER_NAME, PLAYER_ACCOUNT_ID, TournamentPlayerStatus.REFUNDED);
        final Tournament tournament = constructTournament(tournamentVariationTemplate, TournamentStatus.REGISTERING, asList(tournamentPlayer));
        tournament.setSignupStartTimeStamp(new DateTime().minus(1000L));

        tournament.removePlayer(aPlayer(), tournamentHost);

        verifyZeroInteractions(internalWalletService);
    }

    @Test
    public void removePlayerFromTournamentDoesNotRefundAPlayerWhenTheFeesAreZero() throws TournamentException {
        final TournamentVariationTemplate tournamentVariationTemplate = constructTournamentVariationTemplate(null, BigDecimal.ZERO);
        final TournamentPlayer tournamentPlayer = new TournamentPlayer(PLAYER_ID, PLAYER_NAME, PLAYER_ACCOUNT_ID, TournamentPlayerStatus.ACTIVE);
        final Tournament tournament = constructTournament(tournamentVariationTemplate, TournamentStatus.REGISTERING, asList(tournamentPlayer));
        tournament.setSignupStartTimeStamp(new DateTime().minus(1000L));

        tournament.removePlayer(aPlayer(), tournamentHost);

        verifyZeroInteractions(internalWalletService);
    }

    @Test
    public void removePlayerFromTournamentRemovesARemovalPendingPlayer() throws TournamentException {
        final TournamentVariationTemplate tournamentVariationTemplate = constructTournamentVariationTemplate(new BigDecimal("45.33"), new BigDecimal("0.50"));
        final TournamentPlayer tournamentPlayer = new TournamentPlayer(PLAYER_ID, PLAYER_NAME, PLAYER_ACCOUNT_ID, TournamentPlayerStatus.REMOVAL_PENDING);
        final Tournament tournament = constructTournament(tournamentVariationTemplate, TournamentStatus.REGISTERING, asList(tournamentPlayer));
        tournament.setSignupStartTimeStamp(new DateTime().minus(1000L));

        tournament.removePlayer(aPlayer(), tournamentHost);

        assertEquals(0, tournament.playerCount());
    }

    @Test
    public void removePlayerFromTournamentRemovesThePlayer() throws TournamentException {
        final TournamentVariationTemplate tournamentVariationTemplate = constructTournamentVariationTemplate(new BigDecimal("45.33"), new BigDecimal("0.50"));
        final TournamentPlayer tournamentPlayer = new TournamentPlayer(PLAYER_ID, PLAYER_NAME, PLAYER_ACCOUNT_ID, TournamentPlayerStatus.ACTIVE);
        final Tournament tournament = constructTournament(tournamentVariationTemplate, TournamentStatus.REGISTERING, asList(tournamentPlayer));
        tournament.setSignupStartTimeStamp(new DateTime().minus(1000L));

        tournament.removePlayer(aPlayer(), tournamentHost);

        assertEquals(0, tournament.playerCount());
    }

    @Test
    public void removePlayerRefundsThePlayerBothServiceAndEntryFees() throws TournamentException, WalletServiceException {
        final BigDecimal entryFee = new BigDecimal("45.33");
        final BigDecimal serviceFee = new BigDecimal("0.50");
        final TournamentVariationTemplate tournamentVariationTemplate = constructTournamentVariationTemplate(entryFee, serviceFee);
        final TournamentPlayer tournamentPlayer = new TournamentPlayer(PLAYER_ID, PLAYER_NAME, PLAYER_ACCOUNT_ID, TournamentPlayerStatus.ACTIVE);
        final Tournament tournament = constructTournament(tournamentVariationTemplate, TournamentStatus.REGISTERING, asList(tournamentPlayer));
        tournament.setSignupStartTimeStamp(new DateTime().minus(1000L));

        tournament.removePlayer(aPlayer(), tournamentHost);

        verify(internalWalletService).postTransaction(PLAYER_ACCOUNT_ID, entryFee.add(serviceFee),
                "Tournament Fees Refund", "Refund of Fees for tournament " + TOURNAMENT_NAME,
                transactionContext().withSessionId(SESSION_ID).build());
    }

    @Test
    public void removePlayerRefundsStartingChipsWhenPresent() throws TournamentException, WalletServiceException {
        final BigDecimal entryFee = new BigDecimal("45.33");
        final BigDecimal serviceFee = new BigDecimal("0.50");
        final TournamentVariationTemplate tournamentVariationTemplate = constructTournamentVariationTemplate(
                entryFee, serviceFee, null, BigDecimal.valueOf(1000), TournamentType.PRESET, null, null);
        final TournamentPlayer tournamentPlayer = new TournamentPlayer(PLAYER_ID, PLAYER_NAME, TOURNAMENT_PLAYER_ACCOUNT_ID, TournamentPlayerStatus.ACTIVE);
        final Tournament tournament = constructTournament(tournamentVariationTemplate, TournamentStatus.REGISTERING, asList(tournamentPlayer));
        tournament.setSignupStartTimeStamp(new DateTime().minus(1000L));

        tournament.removePlayer(aPlayer(), tournamentHost);

        verify(internalWalletService).postTransaction(PLAYER_ACCOUNT_ID, serviceFee.add(entryFee),
                "Tournament Fees Refund", "Refund of Fees for tournament " + TOURNAMENT_NAME,
                transactionContext().withSessionId(SESSION_ID).build());
        verify(internalWalletService).postTransaction(eq(TOURNAMENT_PLAYER_ACCOUNT_ID), eq(BigDecimal.ZERO.subtract(BigDecimal.valueOf(1000))),
                eq("Tournament Chips"), eq("Initial chips for tournament " + TOURNAMENT_ID), eq(transactionContext().withSessionId(SESSION_ID).build()));
    }

    @Test
    public void removePlayerRefundsThePlayerTheServiceFeeWhenNoEntryFeeIsPresent() throws TournamentException, WalletServiceException {
        final BigDecimal serviceFee = new BigDecimal("0.50");
        final TournamentVariationTemplate tournamentVariationTemplate = constructTournamentVariationTemplate(null, serviceFee);
        final TournamentPlayer tournamentPlayer = new TournamentPlayer(PLAYER_ID, PLAYER_NAME, PLAYER_ACCOUNT_ID, TournamentPlayerStatus.ACTIVE);
        final Tournament tournament = constructTournament(tournamentVariationTemplate, TournamentStatus.REGISTERING, asList(tournamentPlayer));
        tournament.setSignupStartTimeStamp(new DateTime().minus(1000L));

        tournament.removePlayer(aPlayer(), tournamentHost);

        verify(internalWalletService).postTransaction(PLAYER_ACCOUNT_ID, serviceFee,
                "Tournament Fees Refund", "Refund of Fees for tournament " + TOURNAMENT_NAME,
                transactionContext().withSessionId(SESSION_ID).build());
    }

    @Test
    public void removePlayerRefundsThePlayerTheEntryFeeWhenNoServiceFeeIsPresent() throws TournamentException, WalletServiceException {
        final BigDecimal entryFee = new BigDecimal("45.33");
        final TournamentVariationTemplate tournamentVariationTemplate = constructTournamentVariationTemplate(entryFee, null);
        final TournamentPlayer tournamentPlayer = new TournamentPlayer(PLAYER_ID, PLAYER_NAME, PLAYER_ACCOUNT_ID, TournamentPlayerStatus.ACTIVE);
        final Tournament tournament = constructTournament(tournamentVariationTemplate, TournamentStatus.REGISTERING, asList(tournamentPlayer));
        tournament.setSignupStartTimeStamp(new DateTime().minus(1000L));

        tournament.removePlayer(aPlayer(), tournamentHost);

        verify(internalWalletService).postTransaction(PLAYER_ACCOUNT_ID, entryFee,
                "Tournament Fees Refund", "Refund of Fees for tournament " + TOURNAMENT_NAME,
                transactionContext().withSessionId(SESSION_ID).build());
    }

    @Test
    public void removePlayerDoesNotRemoveThePlayerIfTheRefundFails() throws TournamentException, WalletServiceException {
        final BigDecimal entryFee = new BigDecimal("45.33");
        final BigDecimal serviceFee = new BigDecimal("0.50");
        final TournamentVariationTemplate tournamentVariationTemplate = constructTournamentVariationTemplate(entryFee, serviceFee);
        final TournamentPlayer tournamentPlayer = new TournamentPlayer(PLAYER_ID, PLAYER_NAME, PLAYER_ACCOUNT_ID, TournamentPlayerStatus.ACTIVE);
        final Tournament tournament = constructTournament(tournamentVariationTemplate, TournamentStatus.REGISTERING, asList(tournamentPlayer));
        tournament.setSignupStartTimeStamp(new DateTime().minus(1000L));
        when(internalWalletService.postTransaction(PLAYER_ACCOUNT_ID, entryFee.add(serviceFee),
                "Tournament Fees Refund", "Refund of Fees for tournament " + TOURNAMENT_NAME,
                transactionContext().withSessionId(SESSION_ID).build())).thenThrow(new WalletServiceException("aTestException"));

        try {
            tournament.removePlayer(aPlayer(), tournamentHost);
            fail("No exception thrown");
        } catch (TournamentException e) {
            // ignored for test
        }

        assertEquals(1, tournament.playerCount());
    }

    @Test
    public void addPlayerToTournamentThrowsExceptionIfBeforeSignUpStart() {
        final BigDecimal entryFee = new BigDecimal("45.33");
        final TournamentVariationTemplate tournamentVariationTemplate = constructTournamentVariationTemplate(entryFee, null);

        final Tournament tournament = constructTournament(tournamentVariationTemplate, TournamentStatus.REGISTERING, null);
        tournament.setSignupStartTimeStamp(new DateTime().plus(100000L));

        try {
            tournament.addPlayer(aPlayer(), tournamentHost);
            fail("Exception not thrown");
        } catch (TournamentException e) {
            assertEquals(TournamentOperationResult.BEFORE_SIGNUP_TIME, e.getResult());
        }
    }

    @Test
    public void addPlayerToTournamentReturnsErrorResponseIfAfterSignupEnd() {
        final BigDecimal entryFee = new BigDecimal("45.33");
        final TournamentVariationTemplate tournamentVariationTemplate = constructTournamentVariationTemplate(entryFee, null);

        final Tournament tournament = constructTournament(tournamentVariationTemplate, TournamentStatus.REGISTERING, null);
        tournament.setSignupStartTimeStamp(new DateTime().minus(10000L));
        tournament.setSignupEndTimeStamp(new DateTime().minus(5000L));

        try {
            tournament.addPlayer(aPlayer(), tournamentHost);
            fail("Exception not thrown");
        } catch (TournamentException e) {
            assertEquals(TournamentOperationResult.AFTER_SIGNUP_TIME, e.getResult());
        }
    }

    @Test
    public void addPlayerToTournamentReturnsErrorResponseIfPlayerAlreadyRegistered() {
        final BigDecimal entryFee = new BigDecimal("45.33");
        final TournamentVariationTemplate tournamentVariationTemplate = constructTournamentVariationTemplate(entryFee, null);

        final BigDecimal accountId = BigDecimal.valueOf(47897);
        final TournamentPlayer existingPlayer = new TournamentPlayer(PLAYER_ID, PLAYER_NAME, accountId, TournamentPlayerStatus.ADDITION_PENDING);

        final Tournament tournament = constructTournament(tournamentVariationTemplate, TournamentStatus.REGISTERING, asList(existingPlayer));
        tournament.setSignupStartTimeStamp(new DateTime().minus(1000L));

        try {
            tournament.addPlayer(aPlayer(), tournamentHost);
            fail("Exception not thrown");
        } catch (TournamentException e) {
            assertEquals(TournamentOperationResult.PLAYER_ALREADY_REGISTERED, e.getResult());
        }
    }

    @Test
    public void addPlayerToTournamentReturnsErrorResponseIfMaxPlayersExceeded() {
        final TournamentVariationTemplate tournamentVariationTemplate = new TournamentVariationTemplateBuilder()
                .setTournamentVariationTemplateId(new BigDecimal(34345L))
                .setTournamentType(TournamentType.PRESET)
                .setTemplateName("templ1")
                .setEntryFee(new BigDecimal("45.33"))
                .setServiceFee(new BigDecimal("0.0"))
                .setStartingChips(new BigDecimal("0.0"))
                .setMinPlayers(0)
                .setMaxPlayers(3)
                .toTemplate();

        final Tournament tournament = constructTournament(tournamentVariationTemplate, TournamentStatus.REGISTERING,
                buildTournamentPlayers(3, TournamentPlayerStatus.ACTIVE));
        tournament.setSignupStartTimeStamp(new DateTime().minus(1000L));

        try {
            tournament.addPlayer(aPlayer(), tournamentHost);
            fail("Exception not thrown");
        } catch (TournamentException e) {
            assertEquals(TournamentOperationResult.MAX_PLAYERS_EXCEEDED, e.getResult());
        }
    }

    @Test
    public void removePlayerFromTournamentReturnsErrorResponseIfBeforeSignupStart() {
        final BigDecimal entryFee = new BigDecimal("45.33");
        final BigDecimal serviceFee = new BigDecimal("0.50");
        final TournamentVariationTemplate tournamentVariationTemplate = constructTournamentVariationTemplate(entryFee, serviceFee);

        final TournamentPlayer tournamentPlayer = new TournamentPlayer(PLAYER_ID, PLAYER_NAME, TOURNAMENT_PLAYER_ACCOUNT_ID, TournamentPlayerStatus.ADDITION_PENDING);

        final Tournament tournament = constructTournament(tournamentVariationTemplate, TournamentStatus.REGISTERING, asList(tournamentPlayer));
        tournament.setSignupStartTimeStamp(new DateTime().plus(1000000L));

        try {
            tournament.removePlayer(aPlayer(), tournamentHost);
            fail("Exception not thrown");
        } catch (TournamentException e) {
            assertEquals(TournamentOperationResult.BEFORE_SIGNUP_TIME, e.getResult());
        }
    }

    @Test
    public void removePlayerFromTournamentReturnsErrorResponseIfAfterSignupEnd() {
        final BigDecimal entryFee = new BigDecimal("45.33");
        final BigDecimal serviceFee = new BigDecimal("0.50");
        final TournamentVariationTemplate tournamentVariationTemplate = constructTournamentVariationTemplate(entryFee, serviceFee);

        final TournamentPlayer tournamentPlayer = new TournamentPlayer(PLAYER_ID, PLAYER_NAME, TOURNAMENT_PLAYER_ACCOUNT_ID, TournamentPlayerStatus.ADDITION_PENDING);

        final Tournament tournament = constructTournament(tournamentVariationTemplate, TournamentStatus.REGISTERING, asList(tournamentPlayer));
        tournament.setSignupStartTimeStamp(new DateTime().minus(60000L));
        tournament.setSignupEndTimeStamp(new DateTime().minus(6000L));

        try {
            tournament.removePlayer(aPlayer(), tournamentHost);
            fail("Exception not thrown");
        } catch (TournamentException e) {
            assertEquals(TournamentOperationResult.AFTER_SIGNUP_TIME, e.getResult());
        }
    }

    @Test
    public void removePlayerFromTournamentReturnsErrorResponseIfPlayerIsNotRegistered() {
        final BigDecimal entryFee = new BigDecimal("45.33");
        final BigDecimal serviceFee = new BigDecimal("0.50");
        final TournamentVariationTemplate tournamentVariationTemplate = constructTournamentVariationTemplate(entryFee, serviceFee);

        final Tournament tournament = constructTournament(tournamentVariationTemplate, TournamentStatus.CLOSED, null);
        tournament.setSignupStartTimeStamp(new DateTime().minus(10000L));

        try {
            tournament.removePlayer(aPlayer(), tournamentHost);
            fail("Exception not thrown");
        } catch (TournamentException e) {
            assertEquals(TournamentOperationResult.PLAYER_NOT_REGISTERED, e.getResult());
        }
    }

    @Test
    public void restartTables_assigns_tables_to_players_and_calls_table_service_to_start_tables() {
        BigDecimal table1 = BigDecimal.valueOf(17);
        BigDecimal table2 = BigDecimal.valueOf(18);
        BigDecimal table3 = BigDecimal.valueOf(19);

        Collection<PlayerGroup> playersForTables = new ArrayList<>();
        List<TournamentPlayer> allPlayers = buildTournamentPlayers(9, TournamentPlayerStatus.ACTIVE);
        PlayerGroup playersForTable1 = new PlayerGroup(allPlayers.subList(0, 3));
        playersForTables.add(playersForTable1);
        PlayerGroup playersForTable2 = new PlayerGroup(allPlayers.subList(3, 6));
        playersForTables.add(playersForTable2);
        PlayerGroup playersForTable3 = new PlayerGroup(allPlayers.subList(6, 9));
        playersForTables.add(playersForTable3);

        final TournamentVariationTemplate template = constructTournamentVariationTemplate(null, null);
        final Tournament tournament = constructTournament(template, TournamentStatus.ANNOUNCED, allPlayers);
        tournament.setTables(new ArrayList<>(asList(table1, table2, table3)));

        tournamentTableService.reopenAndStartNewGame(eq(table1), eq(playersForTable1), eq(GAME_VARIATION_TEMPLATE_ID), eq(CLIENT_ID));
        tournamentTableService.reopenAndStartNewGame(eq(table2), eq(playersForTable2), eq(GAME_VARIATION_TEMPLATE_ID), eq(CLIENT_ID));
        tournamentTableService.reopenAndStartNewGame(eq(table3), eq(playersForTable3), eq(GAME_VARIATION_TEMPLATE_ID), eq(CLIENT_ID));

        tournament.restartTables(tournamentHost, playersForTables, CLIENT_ID, GAME_VARIATION_TEMPLATE_ID);
    }

    @Test
    public void calculate_time_until_next_break() {
        final long length = 10 * 60 * 1000;
        final long breakLenth = 3 * 60 * 1000;
        final TournamentVariationTemplate template = new TournamentVariationTemplateBuilder()
                .setTournamentVariationTemplateId(BigDecimal.ONE)
                .setTournamentType(TournamentType.PRESET)
                .setTemplateName("template")
                .addTournamentRound(new TournamentVariationRound(1, 0l, length, BigDecimal.valueOf(3234232L), "Red Blackjack", MINIMUM_ROUND_BALANCE, MINIMUM_ROUND_BALANCE.toString()))
                .addTournamentRound(new TournamentVariationRound(2, 0l, length, BigDecimal.valueOf(3234232L), "Red Blackjack", MINIMUM_ROUND_BALANCE, MINIMUM_ROUND_BALANCE.toString()))
                .addTournamentRound(new TournamentVariationRound(3, breakLenth, length, BigDecimal.valueOf(3234232L), "Red Blackjack", MINIMUM_ROUND_BALANCE, MINIMUM_ROUND_BALANCE.toString()))
                .addTournamentRound(new TournamentVariationRound(4, 0l, length, BigDecimal.valueOf(3234232L), "Red Blackjack", MINIMUM_ROUND_BALANCE, MINIMUM_ROUND_BALANCE.toString()))
                .toTemplate();
        final Tournament tournament = constructTournament(template, TournamentStatus.ANNOUNCED, null);
        final DateTime startTime = new DateTime(2009, 1, 1, 12, 0, 0, 0);
        tournament.setStartTimeStamp(startTime);
        tournament.setCurrentRoundIndex(1);
        assertEquals(new DateTime(2009, 1, 1, 12, 20, 0, 0), new DateTime(tournament.retrieveEndTimeOfCurrentRound()));
        assertEquals(new DateTime(2009, 1, 1, 12, 30, 0, 0), new DateTime(tournament.retrieveNextBreakTime()));
        tournament.setCurrentRoundIndex(3);
        assertEquals(new DateTime(2009, 1, 1, 12, 43, 0, 0), new DateTime(tournament.retrieveNextBreakTime()));
    }

    @Test
    public void payPlayersWithPresetPrizePool() throws WalletServiceException {
        final long expiryDelay = 10003434L;
        final TournamentVariationTemplate template = new TournamentVariationTemplateBuilder()
                .setTournamentVariationTemplateId(BigDecimal.ONE)
                .setTournamentType(TournamentType.PRESET)
                .setTemplateName("template")
                .setPrizePool(BigDecimal.valueOf(100))
                .addTournamentPayout(new TournamentVariationPayout(1, BigDecimal.valueOf(0.5)))
                .addTournamentPayout(new TournamentVariationPayout(2, BigDecimal.valueOf(0.3)))
                .addTournamentPayout(new TournamentVariationPayout(3, BigDecimal.valueOf(0.2)))
                .setExpiryDelay(expiryDelay)
                .toTemplate();
        payPlayersForTemplate(expiryDelay, template, BigDecimal.ZERO);
    }

    private void payPlayersForTemplate(final long expiryDelay, final TournamentVariationTemplate template, BigDecimal initialPotValue) throws WalletServiceException {
        final BigDecimal tournamentId = BigDecimal.TEN;
        final TournamentPlayer tournamentPlayer1 = createTournamentPlayer(1, 1);
        final TournamentPlayer tournamentPlayer2 = createTournamentPlayer(2, 2);
        final TournamentPlayer tournamentPlayer3 = createTournamentPlayer(3, 2);
        final TournamentPlayer tournamentPlayer4 = createTournamentPlayer(4, 2);
        List<TournamentPlayer> players = asList(tournamentPlayer1, tournamentPlayer2, tournamentPlayer3, tournamentPlayer4);

        final Tournament tournament = constructTournament(template, TournamentStatus.FINISHED, null, players, initialPotValue);
        tournament.setTournamentId(tournamentId);
        when(tournamentLeaderboard.updateLeaderboard(tournament, tournamentHost, false, false)).thenReturn(false);
        when(auditor.newLabel()).thenReturn("auditLabel");

        when(playerRepository.findById(tournamentPlayer1.getPlayerId())).thenReturn(
                new Player(tournamentPlayer1.getPlayerId(), "aPlayer", tournamentPlayer1.getPlayerId().multiply(BigDecimal.TEN), null, null, null, null));
        when(playerRepository.findById(tournamentPlayer2.getPlayerId())).thenReturn(
                new Player(tournamentPlayer2.getPlayerId(), "aPlayer", tournamentPlayer2.getPlayerId().multiply(BigDecimal.TEN), null, null, null, null));
        when(playerRepository.findById(tournamentPlayer3.getPlayerId())).thenReturn(
                new Player(tournamentPlayer3.getPlayerId(), "aPlayer", tournamentPlayer3.getPlayerId().multiply(BigDecimal.TEN), null, null, null, null));
        when(playerRepository.findById(tournamentPlayer4.getPlayerId())).thenReturn(
                new Player(tournamentPlayer4.getPlayerId(), "aPlayer", tournamentPlayer4.getPlayerId().multiply(BigDecimal.TEN), null, null, null, null));
        final BigDecimal rank1Prize = new BigDecimal("50.00");
        final BigDecimal rank2Prize = new BigDecimal("16.66");
        tournamentPlayerPrizeExpectation(tournamentPlayer1, rank1Prize);
        tournamentPlayerPrizeExpectation(tournamentPlayer2, rank2Prize);
        tournamentPlayerPrizeExpectation(tournamentPlayer3, rank2Prize);
        tournamentPlayerPrizeExpectation(tournamentPlayer4, rank2Prize);

        timeSource.setMillis(342324234324L);

        tournament.settle(tournamentHost);

        for (TournamentPlayer player : players) {
            verify(internalWalletService).closeAccount(player.getAccountId());
        }

        verify(internalWalletService).postTransaction(tournamentPlayer1.getPlayerId().multiply(BigDecimal.TEN), rank1Prize, "Tournament Payout",
                "Tournament (10) payout for player 1", TransactionContext.EMPTY);
        verify(internalWalletService).postTransaction(tournamentPlayer2.getPlayerId().multiply(BigDecimal.TEN), rank2Prize, "Tournament Payout",
                "Tournament (10) payout for player 2", TransactionContext.EMPTY);
        verify(internalWalletService).postTransaction(tournamentPlayer3.getPlayerId().multiply(BigDecimal.TEN), rank2Prize, "Tournament Payout",
                "Tournament (10) payout for player 3", TransactionContext.EMPTY);
        verify(internalWalletService).postTransaction(tournamentPlayer4.getPlayerId().multiply(BigDecimal.TEN), rank2Prize, "Tournament Payout",
                "Tournament (10) payout for player 4", TransactionContext.EMPTY);

        assertEquals(TournamentStatus.SETTLED, tournament.getTournamentStatus());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(tournament.getSettledPrizePot()));
        assertEquals(Long.valueOf(expiryDelay + timeSource.getCurrentTimeStamp()), tournament.getNextEvent());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotPayoutIfTournamentNotFinished() {
        final TournamentVariationTemplate template = new TournamentVariationTemplateBuilder()
                .setTournamentVariationTemplateId(BigDecimal.ONE)
                .setTournamentType(TournamentType.PRESET)
                .setTemplateName("template")
                .toTemplate();
        final Tournament tournament = constructTournament(template, TournamentStatus.RUNNING, null);
        tournament.settle(tournamentHost);
    }

    @Test
    public void settledTournamentsShouldMoveToClosedOnEvent() {
        final TournamentVariationTemplate template = constructTournamentVariationTemplate(
                BigDecimal.valueOf(10), BigDecimal.valueOf(5));
        final Tournament tournament = constructTournament(template, TournamentStatus.SETTLED, null);

        tournament.processEvent(tournamentHost);

        assertEquals(TournamentStatus.CLOSED, tournament.getTournamentStatus());
    }

    @Test
    public void cancelledTournamentsShouldMoveToClosedOnEvent() {
        final TournamentVariationTemplate template = constructTournamentVariationTemplate(
                BigDecimal.valueOf(10), BigDecimal.valueOf(5));
        final Tournament tournament = constructTournament(template, TournamentStatus.CANCELLED, null);

        tournament.processEvent(tournamentHost);

        assertEquals(TournamentStatus.CLOSED, tournament.getTournamentStatus());
    }

    @Test
    public void processEventSetsWarningTimeoutWhenStartIsAfterWarning() {
        tournamentHost.setWarningBeforeStartMillis(360000L);
        Tournament underTest = new Tournament();
        underTest.setTournamentStatus(TournamentStatus.ANNOUNCED);
        final DateTime startTimeStamp = new DateTime(timeSource.getCurrentTimeStamp() + (2 * tournamentHost.getWarningBeforeStartMillis()));
        underTest.setStartTimeStamp(startTimeStamp);
        underTest.setSignupStartTimeStamp(new DateTime(timeSource.getCurrentTimeStamp()));
        timeSource.addMillis(5000);
        underTest.processEvent(tournamentHost);
        assertEquals((Long) (startTimeStamp.getMillis() - tournamentHost.getWarningBeforeStartMillis()), underTest.getNextEvent());
    }

    @Test
    public void processEventSetsStartTimeoutWhenWarningIsPast() {
        tournamentHost.setWarningBeforeStartMillis(360000L);
        Tournament underTest = new Tournament();
        underTest.setTournamentStatus(TournamentStatus.ANNOUNCED);
        final DateTime startTimeStamp = new DateTime(timeSource.getCurrentTimeStamp() + (tournamentHost.getWarningBeforeStartMillis() - 2000));
        underTest.setStartTimeStamp(startTimeStamp);
        underTest.setSignupStartTimeStamp(new DateTime(timeSource.getCurrentTimeStamp()));
        timeSource.addMillis(5000);
        underTest.processEvent(tournamentHost);
        assertEquals((Long) (startTimeStamp.getMillis()), underTest.getNextEvent());
    }

    @Test
    public void shouldPublishWarningWhenNextEventIsWarningTimeout() {
        tournamentHost.setWarningBeforeStartMillis(360000L);
        Tournament underTest = new Tournament();
        final DateTime startTimeStamp = new DateTime(timeSource.getCurrentTimeStamp() + (2 * tournamentHost.getWarningBeforeStartMillis()));
        underTest.setStartTimeStamp(startTimeStamp);
        underTest.setNextEvent(startTimeStamp.getMillis() - tournamentHost.getWarningBeforeStartMillis());
        final boolean result = underTest.calculateShouldSendWarningOfImpendingStart(tournamentHost);
        assertTrue(result);
    }

    @Test
    public void shouldNotPublishWarningWhenNextEventIsWarningTimeout() {
        tournamentHost.setWarningBeforeStartMillis(360000L);
        Tournament underTest = new Tournament();
        final DateTime startTimeStamp = new DateTime(timeSource.getCurrentTimeStamp() + (2 * tournamentHost.getWarningBeforeStartMillis()));
        underTest.setStartTimeStamp(startTimeStamp);
        underTest.setNextEvent(startTimeStamp.getMillis() - tournamentHost.getWarningBeforeStartMillis() + 500);
        final boolean result = underTest.calculateShouldSendWarningOfImpendingStart(tournamentHost);
        assertFalse(result);
    }

    private Tournament constructTournament(final TournamentVariationTemplate template,
                                           final TournamentStatus status,
                                           final Collection<TournamentPlayer> players) {
        return constructTournament(template, status, null, players, BigDecimal.ZERO);
    }

    private Tournament constructTournament(final TournamentVariationTemplate template,
                                           final TournamentStatus status,
                                           final Integer currentRound,
                                           final Collection<TournamentPlayer> players, BigDecimal initialPotValue) {
        final Set<TournamentPlayer> initialPlayers = new HashSet<>();
        if (players != null) {
            initialPlayers.addAll(players);
        }

        final Tournament tournament = new Tournament(TOURNAMENT_ID, initialPlayers);
        tournament.setTournamentVariationTemplate(template);
        tournament.setTournamentStatus(status);
        tournament.setName(TOURNAMENT_NAME);
        tournament.setCurrentRoundIndex(currentRound);
        tournament.setPartnerId(PARTNER_ID);
        tournament.setPot(initialPotValue);
        tournament.setTournamentLeaderboard(tournamentLeaderboard);
        return tournament;
    }

    private TournamentVariationTemplate constructTournamentVariationTemplate(BigDecimal entryFee, BigDecimal serviceFee) {
        return constructTournamentVariationTemplate(entryFee, serviceFee, TournamentType.PRESET);
    }

    private TournamentVariationTemplate constructTournamentVariationTemplate(final BigDecimal entryFee,
                                                                             final BigDecimal serviceFee,
                                                                             final TournamentType tournamentType) {
        return constructTournamentVariationTemplate(entryFee, serviceFee, null, null, tournamentType, null, null);
    }

    private TournamentVariationTemplate constructTournamentVariationTemplate(final BigDecimal entryFee,
                                                                             final BigDecimal serviceFee,
                                                                             final BigDecimal prizePool,
                                                                             final BigDecimal startingChips,
                                                                             final TournamentType tournamentType,
                                                                             final Integer minPlayers,
                                                                             final Integer maxPlayers) {
        return new TournamentVariationTemplateBuilder()
                .setTournamentVariationTemplateId(new BigDecimal(34345L))
                .setTournamentType(tournamentType)
                .setTemplateName("templ1")
                .setEntryFee(entryFee)
                .setServiceFee(serviceFee)
                .setPrizePool(prizePool)
                .setMinPlayers(minPlayers)
                .setMaxPlayers(maxPlayers)
                .setGameType("BLACKJACK")
                .setStartingChips(startingChips)
                .addTournamentRound(new TournamentVariationRound(1, ROUND_END_INTERVAL, ROUND_LENGTH, BigDecimal.valueOf(3234232L), "Red Blackjack", MINIMUM_ROUND_BALANCE, MINIMUM_ROUND_BALANCE.toString()))
                .addTournamentRound(new TournamentVariationRound(2, ROUND_END_INTERVAL, ROUND_LENGTH, BigDecimal.valueOf(3234232L), "Red Blackjack", MINIMUM_ROUND_BALANCE, MINIMUM_ROUND_BALANCE.toString()))
                .toTemplate();
    }

    private List<TournamentPlayer> buildTournamentPlayers(final int playerCount, TournamentPlayerStatus playerStatus) {
        final List<TournamentPlayer> tournamentPlayers = new ArrayList<>();
        for (int i = 0; i < playerCount; ++i) {
            tournamentPlayers.add(new TournamentPlayer(BigDecimal.valueOf(i),
                    "player " + i, BigDecimal.valueOf(i), playerStatus));
        }
        return tournamentPlayers;
    }

    private Player aPlayer() {
        return new Player(PLAYER_ID, "henrythe7th", PLAYER_ACCOUNT_ID, "aPictureUrl", null, null, null);
    }

    private TournamentPlayer createTournamentPlayer(int playerId, int leaderboardPosition) {
        final TournamentPlayer result = new TournamentPlayer(BigDecimal.valueOf(playerId), "Player " + playerId);
        result.setLeaderboardPosition(leaderboardPosition);
        result.setAccountId(BigDecimal.valueOf(playerId * 10));
        return result;
    }

    private void tournamentPlayerPrizeExpectation(TournamentPlayer player, BigDecimal prize) {
        TournamentPlayer playerWithPrize = new TournamentPlayer(player);
        playerWithPrize.setSettledPrize(prize);
    }

    private TournamentPlayer getPlayerFromTournament(final Tournament tournament, final BigDecimal playerId) {
        final TournamentPlayers tournamentPlayers = (TournamentPlayers) ReflectionTestUtils.getField(tournament, "players");
        return tournamentPlayers.getByPlayerId(playerId);
    }

    private PlayerSession aPlayerSession() {
        return new PlayerSession(SESSION_ID, PLAYER_ID, "aSessionKey", "aPictureUrl", "aNickname", YAZINO,
                Platform.WEB, "127.0.0.1", BigDecimal.ZERO, "anEmail");
    }
}
