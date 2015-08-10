package com.yazino.platform.gamehost;

import com.yazino.game.api.GameStatus;
import com.yazino.game.api.time.SettableTimeSource;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.gamehost.external.NovomaticGameRequestService;
import com.yazino.platform.gamehost.postprocessing.*;
import com.yazino.platform.gamehost.preprocessing.*;
import com.yazino.platform.gamehost.wallet.BufferedGameHostWalletFactory;
import com.yazino.platform.gamehost.wallet.SpaceBufferedGameHostWallet;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.messaging.destination.DestinationFactory;
import com.yazino.platform.messaging.host.ErrorHostDocument;
import com.yazino.platform.messaging.host.GameStatusHostDocument;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.chat.ChatChannel;
import com.yazino.platform.model.community.PlayerSessionSummary;
import com.yazino.platform.model.table.CommandWrapper;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.processor.table.GameCompletePublisher;
import com.yazino.platform.repository.chat.ChatRepository;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.table.GameRepository;
import com.yazino.platform.service.audit.Auditor;
import com.yazino.platform.service.community.LocationService;
import com.yazino.platform.session.Location;
import com.yazino.platform.session.LocationChangeType;
import com.yazino.platform.table.PlayerInformation;
import com.yazino.platform.table.TableStatus;
import com.yazino.platform.test.InMemoryGameRepository;
import com.yazino.platform.test.InMemoryPlayerStatisticEventsPublisher;
import com.yazino.platform.test.PrintlnRules;
import com.yazino.platform.test.PrintlnStatus;
import com.yazino.platform.util.UUIDSource;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.*;

import static com.yazino.platform.table.TableType.PUBLIC;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

public class GameHostIntegrationTest {

    private static final Set<com.yazino.game.api.ScheduledEvent> emptyEvents = new HashSet<>();
    private static final BigDecimal TABLE_ID = BigDecimal.valueOf(15);
    private static final String TABLE_NAME = "testTable";
    private static final long GAME_ID = -1l;
    private static final String UUID = "12345";
    private static final String AUDIT_LABEL = "L1";
    private static final String NEW_UUID = "aUuid";
    private static final BigDecimal PLAYER_ID = BigDecimal.TEN;
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(34);
    private static final String PLAYER_NAME = "y";
    private static final BigDecimal PLAYER_ACCOUNT_ID = BigDecimal.valueOf(4543);
    private static final BigDecimal PLAYER2_ACCOUNT_ID = BigDecimal.ONE;
    private static final BigDecimal ACCOUNT_BALANCE = BigDecimal.ZERO;
    private static final BigDecimal PLAYER1_ACCOUNT_ID = BigDecimal.ZERO;
    private static final BigDecimal PLAYER2_ID = BigDecimal.valueOf(21);
    private static final BigDecimal PLAYER1_ID = BigDecimal.valueOf(20);
    private static final com.yazino.game.api.GamePlayer A_PLAYER = new com.yazino.game.api.GamePlayer(PLAYER_ID, SESSION_ID, PLAYER_NAME);
    private static final String GAME_TYPE = "TEST";

    @Mock
    private com.yazino.game.api.GameRules gameRules;
    @Mock
    private com.yazino.game.api.GameStatus gameStatus;
    @Mock
    private com.yazino.game.api.GameStatus gameStatus1;
    @Mock
    private DocumentDispatcher documentDispatcher;
    @Mock
    private GameCompletePublisher gameCompletePublisher;
    @Mock
    private Auditor auditor;
    @Mock
    private LocationService locationService;
    @Mock
    private BufferedGameHostWalletFactory bufferedGameHostWalletFactory;
    @Mock
    private NovomaticGameRequestService novomaticRequestService;
    @Mock
    private SpaceBufferedGameHostWallet bufferedGameHostWallet;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private UUIDSource uuidSource;
    @Mock
    private ChatRepository chatRepository;

    private GameHost host;
    private CommandWrapper command;
    private Table table;
    private InMemoryPlayerStatisticEventsPublisher playerStatisticEventsPublisher;
    private SettableTimeSource timeSource;

    @Before
    public void setUp() throws com.yazino.game.api.GameException, WalletServiceException {
        MockitoAnnotations.initMocks(this);

        timeSource = new SettableTimeSource(System.currentTimeMillis());

        command = new CommandWrapper(TABLE_ID, GAME_ID, A_PLAYER.getId(), SESSION_ID, "T");
        command.setRequestId(UUID);
        playerStatisticEventsPublisher = new InMemoryPlayerStatisticEventsPublisher();

        table = new Table(new com.yazino.game.api.GameType(GAME_TYPE, "Test", Collections.<String>emptySet()), BigDecimal.ONE, "test", true);
        table.setTableId(TABLE_ID);
        table.setTableStatus(TableStatus.open);
        table.setVariationProperties(new HashMap<String, String>());

        when(bufferedGameHostWalletFactory.create(table.getTableId(), command.getRequestId())).thenReturn(bufferedGameHostWallet);
        when(bufferedGameHostWalletFactory.create(table.getTableId())).thenReturn(bufferedGameHostWallet);

        when(auditor.newLabel()).thenReturn(AUDIT_LABEL);
        when(bufferedGameHostWallet.getBalance(Mockito.isA(BigDecimal.class))).thenReturn(BigDecimal.ZERO);

        when(uuidSource.getNewUUID()).thenReturn(NEW_UUID);
        when(auditor.newLabel()).thenReturn(AUDIT_LABEL);
        when(bufferedGameHostWallet.getBalance(Mockito.isA(BigDecimal.class))).thenReturn(ACCOUNT_BALANCE);

        when(gameRules.getGameType()).thenReturn("TEST");

        when(playerRepository.findSummaryByPlayerAndSession(PLAYER_ID, SESSION_ID)).thenReturn(aPlayerSessionSummary(PLAYER_ID, "Player", PLAYER_ACCOUNT_ID, BigDecimal.valueOf(100)));
        when(playerRepository.findSummaryByPlayerAndSession(PLAYER_ID, null)).thenReturn(aPlayerSessionSummary(PLAYER_ID, "Player", PLAYER_ACCOUNT_ID, BigDecimal.valueOf(100)));
        when(playerRepository.findSummaryByPlayerAndSession(PLAYER1_ID, null)).thenReturn(aPlayerSessionSummary(PLAYER1_ID, "Player1", PLAYER1_ACCOUNT_ID, BigDecimal.valueOf(200)));
        when(playerRepository.findSummaryByPlayerAndSession(PLAYER2_ID, null)).thenReturn(aPlayerSessionSummary(PLAYER2_ID, "Player2", PLAYER2_ACCOUNT_ID, BigDecimal.valueOf(300)));

        when(chatRepository.getOrCreateForLocation(TABLE_ID.toPlainString())).thenReturn(new ChatChannel(TABLE_ID.toPlainString()));
    }

    private PlayerSessionSummary aPlayerSessionSummary(final BigDecimal playerId, final String name, final BigDecimal accountId, final BigDecimal sessionId) {
        return new PlayerSessionSummary(playerId, accountId, name, sessionId);
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown", "ThrowableResultOfMethodCallIgnored"})
    @Test
    public void commandsAndExceptionGetAuditedInCaseOfErrors() throws com.yazino.game.api.GameException {
        whenGameStart();
        com.yazino.game.api.GameException exc = new com.yazino.game.api.GameException(new com.yazino.game.api.ParameterisedMessage("XYZ"));
        doThrow(exc).when(gameRules).execute(isA(com.yazino.game.api.ExecutionContext.class), isA(com.yazino.game.api.Command.class));
        executeCommand();
        verify(auditor).audit(AUDIT_LABEL, command.toCommand(PLAYER_NAME));
    }

    @Test
    public void testProcessTransactionResultIsNotProcessedForCompleteGame() {
        final GameRepository mockGameRepository = mock(GameRepository.class);
        when(mockGameRepository.getGameRules(GAME_TYPE)).thenReturn(gameRules);
        gameStatus = mock(com.yazino.game.api.GameStatus.class);
        when(gameRules.isComplete(gameStatus)).thenReturn(true);
        table.setCurrentGame(gameStatus);
        table.setGameId(GAME_ID);
        final com.yazino.game.api.TransactionResult txResult = new com.yazino.game.api.TransactionResult("foo", true, null, null, null, null);
        host = gameHost(mockGameRepository);
        host.processTransactionResult(table, GAME_ID, txResult);
        verifyNoMoreInteractions(documentDispatcher);
    }

    @Test
    public void testProcessTransactionResultCorrectlyCallsAuditOnSuccessWithNonNullExecutionResult()
            throws com.yazino.game.api.GameException {
        final String auditLabel = "test424243542354";
        gameStatus = mock(com.yazino.game.api.GameStatus.class);
        when(gameRules.canBeClosed(gameStatus)).thenReturn(false);
        when(gameRules.isComplete(gameStatus)).thenReturn(false);
        whenPlayers();
        when(gameRules.getPlayerInformation(gameStatus)).thenReturn(Collections.<com.yazino.game.api.PlayerAtTableInformation>emptyList());
        when(gameRules.getNumberOfSeatsTaken(gameStatus)).thenReturn(0);
        table.setCurrentGame(gameStatus);
        table.setGameId(GAME_ID);
        final com.yazino.game.api.TransactionResult txResult = new com.yazino.game.api.TransactionResult("foo", true, null, null, null, null);
        final com.yazino.game.api.ExecutionResult executionResult = new com.yazino.game.api.ExecutionResult.Builder(gameRules, gameStatus).build();
        when(gameRules.processTransactionResult(isA(com.yazino.game.api.ExecutionContext.class), eq(txResult)))
                .thenReturn(executionResult);
        auditor = mock(Auditor.class);
        when(auditor.newLabel()).thenReturn(auditLabel);
        host = gameHost(new InMemoryGameRepository(gameRules));
        host.processTransactionResult(table, GAME_ID, txResult);
    }

    @Test
    public void testProcessTransactionResultCorrectlyCallsAuditOnSuccessWithNullExecutionResult() {
        final String auditLabel = "test424542542354";
        gameStatus = mock(com.yazino.game.api.GameStatus.class);
        when(gameRules.isComplete(gameStatus)).thenReturn(false);
        table.setCurrentGame(gameStatus);
        table.setGameId(GAME_ID);
        final com.yazino.game.api.TransactionResult txResult = new com.yazino.game.api.TransactionResult("foo", true, null, null, null, null);
        auditor = mock(Auditor.class);
        when(auditor.newLabel()).thenReturn(auditLabel);
        host = gameHost(new InMemoryGameRepository(gameRules));
        host.processTransactionResult(table, GAME_ID, txResult);
    }

    @Test
    public void testProcessTransactionResultIsOnlyExecutedForTheCurrentGame() {
        final GameRepository mockGameRepository = mock(GameRepository.class);
        when(mockGameRepository.getGameRules(GAME_TYPE)).thenReturn(gameRules);
        final long differentGameId = GAME_ID + 6;
        gameStatus = mock(com.yazino.game.api.GameStatus.class);
        when(gameRules.isComplete(gameStatus)).thenReturn(false);
        table.setCurrentGame(gameStatus);
        table.setGameId(GAME_ID);
        final com.yazino.game.api.TransactionResult txResult = new com.yazino.game.api.TransactionResult("foo", true, null, null, null, null);
        host = gameHost(mockGameRepository);
        host.processTransactionResult(table, differentGameId, txResult);
        verifyNoMoreInteractions(locationService);
    }

    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored", "ThrowableInstanceNeverThrown"})
    @Test
    public void testProcessTransactionResultCorrectlyCallsAuditOnFailure() throws com.yazino.game.api.GameException {
        final String auditLabel = "test42454254345345";
        gameStatus = mock(com.yazino.game.api.GameStatus.class);
        when(gameRules.isComplete(gameStatus)).thenReturn(false);
        table.setCurrentGame(gameStatus);
        table.setGameId(GAME_ID);
        final com.yazino.game.api.TransactionResult txResult = new com.yazino.game.api.TransactionResult("foo", true, null, null, null, null);
        final com.yazino.game.api.GameException thrownException = new com.yazino.game.api.GameException(new com.yazino.game.api.ParameterisedMessage("test.mock.error"));
        when(gameRules.processTransactionResult(isA(com.yazino.game.api.ExecutionContext.class), eq(txResult))).thenThrow(thrownException);
        auditor = mock(Auditor.class);
        when(auditor.newLabel()).thenReturn(auditLabel);
        host = gameHost(new InMemoryGameRepository(gameRules));
        host.processTransactionResult(table, GAME_ID, txResult);
        verify(auditor, times(1)).newLabel();
    }

    @Test
    public void testProcessTransactionResultUpdatesCachedBalance() {
        BigDecimal ORIGINAL_BALANCE = BigDecimal.ONE;
        BigDecimal NEW_BALANCE = BigDecimal.TEN;
        table.setCurrentGame(gameStatus);
        table.setGameId(GAME_ID);
        com.yazino.game.api.GamePlayer player = new com.yazino.game.api.GamePlayer(PLAYER1_ID, SESSION_ID, "");
        addPlayersToGame(gameStatus, player);
        table.playerWithAccountId(PLAYER1_ACCOUNT_ID).setCachedBalance(ORIGINAL_BALANCE);
        final com.yazino.game.api.TransactionResult txResult = new com.yazino.game.api.TransactionResult("foo", true, null, PLAYER1_ACCOUNT_ID, NEW_BALANCE, null);
        host = gameHost(new InMemoryGameRepository(gameRules));

        // exercise SUT
        host.processTransactionResult(table, GAME_ID, txResult);

        // verify
        assertEquals(NEW_BALANCE, table.playerWithAccountId(PLAYER1_ACCOUNT_ID).getCachedBalance());
    }

    @Test
    public void testGetStatusSendsStatusToPlayer() {
        whenGameStart();
        command = new CommandWrapper(TABLE_ID, GAME_ID, A_PLAYER.getId(), SESSION_ID, "InitialGetStatus");
        command.setRequestId(UUID);
        final List<HostDocument> hostDocuments = executeCommand();
        assertThat((GameStatusHostDocument) hostDocuments.get(0), Matchers.isA(GameStatusHostDocument.class));
    }

    @Test
    public void commandsAndStatusChangesGetAudited() throws com.yazino.game.api.GameException {
        whenGameStart();
        whenPlayers();
        final com.yazino.game.api.ExecutionResult result = new com.yazino.game.api.ExecutionResult.Builder(gameRules, gameStatus1).scheduledEvents(emptyEvents).build();
        when(gameRules.execute(isA(com.yazino.game.api.ExecutionContext.class), isA(com.yazino.game.api.Command.class))).thenReturn(result);
        executeCommand();
        verify(auditor).audit(eq(AUDIT_LABEL), eq(command.toCommand(PLAYER_NAME)));
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    @Test
    public void senderGetsErrorMessageWhenExceptionThrownInGameHost() throws com.yazino.game.api.GameException {
        whenGameStart();
        com.yazino.game.api.ParameterisedMessage pm = new com.yazino.game.api.ParameterisedMessage("UPS!");
        when(gameRules.execute(isA(com.yazino.game.api.ExecutionContext.class), eq(command.toCommand(PLAYER_NAME)))).thenThrow(new com.yazino.game.api.GameException(pm));
        final List<HostDocument> hostDocuments = executeCommand();
        assertThat((ErrorHostDocument) hostDocuments.get(1), Matchers.isA(ErrorHostDocument.class));
    }

    @Test
    public void testObserversGetNotified() throws com.yazino.game.api.GameException {
        whenGameStart();
        final com.yazino.game.api.ExecutionResult executionResult = new com.yazino.game.api.ExecutionResult.Builder(new PrintlnRules(), new GameStatus(new PrintlnStatus())).scheduledEvents(emptyEvents).build();
        when(gameRules.getObservableStatus(gameStatus1, getContextFor(null))).thenReturn(new PrintlnStatus());
        when(gameRules.execute(isA(com.yazino.game.api.ExecutionContext.class), eq(command.toCommand(PLAYER_NAME)))).thenReturn(executionResult);
        final List<HostDocument> hostDocuments = executeCommand();
        assertThat((GameStatusHostDocument) hostDocuments.get(0), Matchers.isA(GameStatusHostDocument.class));
        assertThat((GameStatusHostDocument) hostDocuments.get(1), Matchers.isA(GameStatusHostDocument.class));
    }

    private void addPlayersToGame(com.yazino.game.api.GameStatus gameStatus, com.yazino.game.api.GamePlayer... players) {
        Collection<com.yazino.game.api.PlayerAtTableInformation> playerAtTableInformationCollection = new ArrayList<>();
        for (com.yazino.game.api.GamePlayer player : players) {
            playerAtTableInformationCollection.add(new com.yazino.game.api.PlayerAtTableInformation(player, Collections.<String, String>emptyMap()));
            if (table.playerAtTable(player.getId()) == null) {
                if (player.getId().equals(PLAYER_ID)) {
                    table.addPlayerToTable(new PlayerInformation(PLAYER_ID, PLAYER_NAME, PLAYER_ACCOUNT_ID, BigDecimal.TEN, BigDecimal.ZERO));
                } else if (player.getId().equals(PLAYER1_ID)) {
                    table.addPlayerToTable(new PlayerInformation(PLAYER1_ID, "player1", PLAYER1_ACCOUNT_ID, BigDecimal.TEN, BigDecimal.ZERO));
                } else if (player.getId().equals(PLAYER2_ID)) {
                    table.addPlayerToTable(new PlayerInformation(PLAYER2_ID, "player2", PLAYER2_ACCOUNT_ID, BigDecimal.TEN, BigDecimal.ZERO));
                } else {
                    table.addPlayerToTable(new PlayerInformation(player.getId(), player.getName(), player.getId(), BigDecimal.TEN, BigDecimal.ZERO));
                }
            }
        }

        when(gameRules.getPlayerInformation(gameStatus)).thenReturn(playerAtTableInformationCollection);
    }

    private void whenPlayers(com.yazino.game.api.GamePlayer... players) {
        addPlayersToGame(gameStatus1, players);
    }

    @Test
    public void testPlayersGetNewStatus() throws com.yazino.game.api.GameException {
        whenGameStart();
        command = commandFrom(PLAYER1_ID);
        com.yazino.game.api.GamePlayer p1 = new com.yazino.game.api.GamePlayer(PLAYER1_ID, SESSION_ID, "");
        com.yazino.game.api.GamePlayer p2 = new com.yazino.game.api.GamePlayer(PLAYER2_ID, SESSION_ID, "");
        table.setCurrentGame(gameStatus1);
        whenPlayers(p1, p2);
        when(gameRules.getObservableStatus(gameStatus1, getContextFor(p1, false, table.getIncrement()))).thenReturn(new PrintlnStatus());
        when(gameRules.getObservableStatus(gameStatus1, getContextFor(p2, true, table.getIncrement()))).thenReturn(new PrintlnStatus());
        final com.yazino.game.api.ExecutionResult result = new com.yazino.game.api.ExecutionResult.Builder(gameRules, gameStatus1).scheduledEvents(emptyEvents).build();
        when(gameRules.execute(isA(com.yazino.game.api.ExecutionContext.class), eq(command.toCommand(PLAYER_NAME)))).thenReturn(result);
        final List<HostDocument> hostDocuments = executeCommand();

        // table.lastGameChanges?
        assertThat(hostDocuments.size(), is(equalTo(2)));
        assertThat((GameStatusHostDocument) hostDocuments.get(0), Matchers.isA(GameStatusHostDocument.class));
        assertThat((GameStatusHostDocument) hostDocuments.get(1), Matchers.isA(GameStatusHostDocument.class));
    }

    private CommandWrapper commandFrom(final BigDecimal playerId) {
        final CommandWrapper commandWrapper = new CommandWrapper(command.getTableId(), command.getGameId(), playerId,
                SESSION_ID, command.getType(), command.getArgs());
        commandWrapper.setRequestId(UUID);
        return commandWrapper;
    }

    private com.yazino.game.api.ObservableContext getContextFor(final com.yazino.game.api.GamePlayer player) {
        return getContextFor(player, false, 0);
    }

    private com.yazino.game.api.ObservableContext getContextFor(final com.yazino.game.api.GamePlayer player, boolean skipIfPossible, long startIncrement) {
        if (player == null) {
            return new com.yazino.game.api.ObservableContext(null, null);
        }

        final PlayerInformation playerInfo = table.playerAtTable(player.getId());
        return playerInfo.toObservableContext(skipIfPossible, startIncrement);
    }

    @Test
    public void statsDontGetPublishedToActivePlayersIfGameNotOver() throws com.yazino.game.api.GameException {
        whenGameStart();
        com.yazino.game.api.GamePlayer p1 = new com.yazino.game.api.GamePlayer(PLAYER1_ID, SESSION_ID, "");
        com.yazino.game.api.GamePlayer p2 = new com.yazino.game.api.GamePlayer(PLAYER2_ID, SESSION_ID, "");
        whenPlayers(p1, p2);
        when(gameRules.getObservableStatus(gameStatus1, getContextFor(p1))).thenReturn(new PrintlnStatus());
        when(gameRules.getObservableStatus(gameStatus1, getContextFor(p2))).thenReturn(new PrintlnStatus());
        when(gameRules.isComplete(gameStatus1)).thenReturn(false);
        final com.yazino.game.api.ExecutionResult result = new com.yazino.game.api.ExecutionResult.Builder(gameRules, gameStatus1).scheduledEvents(emptyEvents).build();
        when(gameRules.execute(isA(com.yazino.game.api.ExecutionContext.class), eq(command.toCommand(PLAYER_NAME)))).thenReturn(result);
        executeCommand();
        verify(locationService, times(2)).notify(any(BigDecimal.class), any(BigDecimal.class), any(LocationChangeType.class), any(Location.class));
        verifyNoMoreInteractions(locationService);
    }

    @Test
    public void tournamentTableShouldNotRetrievePlayersMainAccount() {
        table.setShowInLobby(false);
        table.setGameId(1l);
        host = gameHost(new InMemoryGameRepository(gameRules));
        BigDecimal playerId = BigDecimal.valueOf(54321);
        CommandWrapper aCommand = new CommandWrapper(table.getTableId(), 1l, playerId, SESSION_ID, "aCommand");
        aCommand.setRequestId(UUID);
        when(playerRepository.findSummaryByPlayerAndSession(playerId, SESSION_ID)).thenReturn(aPlayerSessionSummary(playerId, "", BigDecimal.valueOf(10), BigDecimal.valueOf(100)));
        List<HostDocument> documents = host.execute(table, aCommand);
        assertEquals(1, documents.size());
        assertThat((ErrorHostDocument) documents.get(0), Matchers.isA(ErrorHostDocument.class));
    }

    @Test
    public void testTableGetsNewEvents() throws com.yazino.game.api.GameException {
        whenGameStart();
        whenPlayers();
        List<com.yazino.game.api.ScheduledEvent> events = Arrays.asList(
                new com.yazino.game.api.ScheduledEvent(100l, 100l, "X", "Y", new HashMap<String, String>(), false),
                new com.yazino.game.api.ScheduledEvent(300l, 100l, "Z", "Y", new HashMap<String, String>(), false),
                new com.yazino.game.api.ScheduledEvent(200l, 100l, "X", "Z", new HashMap<String, String>(), false)
        );
        final com.yazino.game.api.ExecutionResult result = new com.yazino.game.api.ExecutionResult.Builder(gameRules, gameStatus1).scheduledEvents(events).build();
        when(gameRules.execute((com.yazino.game.api.ExecutionContext) anyObject(), eq(command.toCommand(PLAYER_NAME)))).thenReturn(result);
        executeCommand();
        assertEquals(3, table.scheduledEventCount());
    }

    @Test
    public void executeAcknowledgement() {
        table.playerAcknowledgesIncrement(A_PLAYER.getId(), 1L);
        table.setIncrement(10L);
        table.setCurrentGame(gameStatus);
        when(gameRules.isAPlayer(gameStatus, A_PLAYER)).thenReturn(true);
        addPlayersToGame(gameStatus, A_PLAYER);
        command = new CommandWrapper(table.getTableId(), table.getGameId(), A_PLAYER.getId(), SESSION_ID, com.yazino.game.api.Command.CommandType.Ack.getCode(), "9");
        command.setRequestId(UUID);
        executeCommand();
        assertEquals(9L, table.playerAtTable(A_PLAYER.getId()).getAcknowledgedIncrement());
    }

    private void whenGameStart() {
        com.yazino.game.api.GameStatus newStatus = new com.yazino.game.api.GameStatus(new HashMap<String, Object>());
        final com.yazino.game.api.ExecutionResult r = new com.yazino.game.api.ExecutionResult.Builder(gameRules, newStatus).build();
        when(gameRules.startNewGame(isA(com.yazino.game.api.GameCreationContext.class))).thenReturn(r);
    }

    private List<HostDocument> executeCommand() {
        host = gameHost(new InMemoryGameRepository(gameRules));
        return host.execute(table, command);
    }

    @Test
    public void forceNewGameWillRestartTableAndStartNewGame() {
        Map<BigDecimal, BigDecimal> playerIdToAccountIdOverrides = new HashMap<>();
        playerIdToAccountIdOverrides.put(PLAYER_ID, BigDecimal.valueOf(202020));
        com.yazino.game.api.GameStatus status = new GameStatus(new PrintlnStatus());
        com.yazino.game.api.ExecutionResult result = new com.yazino.game.api.ExecutionResult.Builder(gameRules, status).build();
        when(gameRules.startNewGame(isA(com.yazino.game.api.GameCreationContext.class))).thenReturn(result);
        when(auditor.newLabel()).thenReturn("X");
        host = gameHost(new InMemoryGameRepository(gameRules));
        table.setTableStatus(TableStatus.closed);
        table.setCurrentGame(null);
        Collection<com.yazino.game.api.PlayerAtTableInformation> playersAtTable = Arrays.asList(
                new com.yazino.game.api.PlayerAtTableInformation(new com.yazino.game.api.GamePlayer(PLAYER1_ID, null, "One"), Collections.<String, String>emptyMap()),
                new com.yazino.game.api.PlayerAtTableInformation(new com.yazino.game.api.GamePlayer(PLAYER2_ID, null, "Two"), Collections.<String, String>emptyMap())
        );
        host.forceNewGame(table, playersAtTable, playerIdToAccountIdOverrides);
        ArgumentCaptor<com.yazino.game.api.GameCreationContext> contextCaptor = ArgumentCaptor.forClass(com.yazino.game.api.GameCreationContext.class);
        verify(gameRules).startNewGame(contextCaptor.capture());
        assertEquals(TableStatus.open, table.getTableStatus());
        assertEquals(status, table.getCurrentGame());
        assertEquals(playersAtTable, contextCaptor.getValue().getPlayersAtTableInformation());
    }

    @Test
    public void removeAllPlayersShouldSendLocationNotifications() throws WalletServiceException {
        com.yazino.game.api.GamePlayer p1 = new com.yazino.game.api.GamePlayer(PLAYER1_ID, SESSION_ID, "");
        com.yazino.game.api.GamePlayer p2 = new com.yazino.game.api.GamePlayer(PLAYER2_ID, SESSION_ID, "");
        table.setCurrentGame(gameStatus);
        table.setTableName(TABLE_NAME);
        addPlayersToGame(gameStatus, p1, p2);

        gameHost(gameRules).removeAllPlayers(table);

        final Location tableLocation = new Location(TABLE_ID.toString(), TABLE_NAME, table.getGameTypeId(), null, PUBLIC);
        verify(locationService).notify(PLAYER1_ID, SESSION_ID, LocationChangeType.REMOVE, tableLocation);
        verify(locationService).notify(PLAYER2_ID, SESSION_ID, LocationChangeType.REMOVE, tableLocation);
    }

    @Test
    public void shutdownIgnoresTablesWithNullGames() {
        table.setCurrentGame(null);

        List<HostDocument> result = gameHost(gameRules).shutdown(table);
        assertEquals(0, result.size());

    }

    @Test
    public void shutdownReturnsInvestedAmountsAndCreateDocumentsForPlayers() throws WalletServiceException {
        com.yazino.game.api.PlayerAtTableInformation p1 = new com.yazino.game.api.PlayerAtTableInformation(new com.yazino.game.api.GamePlayer(BigDecimal.valueOf(1), SESSION_ID, "p1"), BigDecimal.valueOf(10), Collections.<String, String>emptyMap());
        PlayerInformation pi1 = new PlayerInformation(BigDecimal.valueOf(1), "p1", BigDecimal.valueOf(501), BigDecimal.TEN, BigDecimal.valueOf(0));
        com.yazino.game.api.PlayerAtTableInformation p2 = new com.yazino.game.api.PlayerAtTableInformation(new com.yazino.game.api.GamePlayer(BigDecimal.valueOf(2), SESSION_ID, "p2"), BigDecimal.valueOf(0), Collections.<String, String>emptyMap());
        PlayerInformation pi2 = new PlayerInformation(BigDecimal.valueOf(2), "p2", BigDecimal.valueOf(501), BigDecimal.TEN, BigDecimal.valueOf(0));
        com.yazino.game.api.PlayerAtTableInformation p3 = new com.yazino.game.api.PlayerAtTableInformation(new com.yazino.game.api.GamePlayer(BigDecimal.valueOf(3), SESSION_ID, "p3"), BigDecimal.valueOf(20), Collections.<String, String>emptyMap());
        PlayerInformation pi3 = new PlayerInformation(BigDecimal.valueOf(3), "p3", BigDecimal.valueOf(501), BigDecimal.TEN, BigDecimal.valueOf(0));
        table.addPlayerToTable(pi1);
        table.addPlayerToTable(pi2);
        table.addPlayerToTable(pi3);
        Collection<com.yazino.game.api.PlayerAtTableInformation> players = Arrays.asList(p1, p2, p3);
        when(gameRules.getPlayerInformation(gameStatus)).thenReturn(players);
        table.setCurrentGame(gameStatus);
        List<HostDocument> documents = gameHost(gameRules).shutdown(table);
        assertEquals(3, documents.size());
        verify(bufferedGameHostWallet).post(table.getTableId(), table.getGameId(), pi1, p1.getInvestedChips(), com.yazino.game.api.TransactionType.Return, AUDIT_LABEL, "Shutdown refund", NEW_UUID);
        verify(bufferedGameHostWallet).post(table.getTableId(), table.getGameId(), pi3, p3.getInvestedChips(), com.yazino.game.api.TransactionType.Return, AUDIT_LABEL, "Shutdown refund", NEW_UUID);
        verify(bufferedGameHostWallet).flush();
        verifyNoMoreInteractions(bufferedGameHostWallet);
    }

    @Test
    public void shutdownReturnsInvestedAmountsOnlyOnceForPlayer() throws WalletServiceException {
        com.yazino.game.api.GamePlayer player1 = new com.yazino.game.api.GamePlayer(BigDecimal.valueOf(1), null, "p1");
        com.yazino.game.api.PlayerAtTableInformation p1 = new com.yazino.game.api.PlayerAtTableInformation(player1, BigDecimal.valueOf(10), Collections.<String, String>emptyMap());
        PlayerInformation pi1 = new PlayerInformation(player1.getId(), player1.getName(), BigDecimal.valueOf(501), BigDecimal.TEN, BigDecimal.valueOf(0));
        com.yazino.game.api.PlayerAtTableInformation p2 = new com.yazino.game.api.PlayerAtTableInformation(player1, BigDecimal.valueOf(15), Collections.<String, String>emptyMap());
        PlayerInformation pi2 = new PlayerInformation(player1.getId(), player1.getName(), BigDecimal.valueOf(501), BigDecimal.TEN, BigDecimal.valueOf(0));
        table.addPlayerToTable(pi1);
        table.addPlayerToTable(pi2);
        Collection<com.yazino.game.api.PlayerAtTableInformation> players = Arrays.asList(p1, p2);
        when(gameRules.getPlayerInformation(gameStatus)).thenReturn(players);
        table.setCurrentGame(gameStatus);
        List<HostDocument> documents = gameHost(gameRules).shutdown(table);
        assertEquals(1, documents.size());
        verify(bufferedGameHostWallet).post(table.getTableId(), table.getGameId(), pi1, BigDecimal.valueOf(25), com.yazino.game.api.TransactionType.Return, AUDIT_LABEL, "Shutdown refund", NEW_UUID);
        verify(bufferedGameHostWallet).flush();
        verifyNoMoreInteractions(bufferedGameHostWallet);
    }

    @Test
    public void shutdownIgnoresNullInvestedAmounts() {
        com.yazino.game.api.PlayerAtTableInformation p1 = new com.yazino.game.api.PlayerAtTableInformation(new com.yazino.game.api.GamePlayer(BigDecimal.valueOf(1), SESSION_ID, "p1"), null, Collections.<String, String>emptyMap());
        PlayerInformation pi1 = new PlayerInformation(BigDecimal.valueOf(1), "p1", BigDecimal.valueOf(501), BigDecimal.TEN, BigDecimal.valueOf(0));
        table.addPlayerToTable(pi1);
        Collection<com.yazino.game.api.PlayerAtTableInformation> players = Arrays.asList(p1);
        when(gameRules.getPlayerInformation(gameStatus)).thenReturn(players);
        table.setCurrentGame(gameStatus);
        List<HostDocument> documents = gameHost(gameRules).shutdown(table);
        assertEquals(1, documents.size());
        verify(bufferedGameHostWallet).flush();
        verifyNoMoreInteractions(bufferedGameHostWallet);
    }


    private GameHost gameHost(final com.yazino.game.api.GameRules gameRules) {
        return gameHost(new InMemoryGameRepository(gameRules));
    }

    private GameHost gameHost(final GameRepository mockGameRepository) {
        final DestinationFactory destinationFactory = new DestinationFactory();


        GameInitialiser gameInitialiser = new GameInitialiser();
        gameInitialiser.setEventPreInitialisationProcessors(Arrays.asList((EventPreprocessor)
                        new TableClosedPreprocessor(destinationFactory),
                new GameDisabledPreprocessor(mockGameRepository, destinationFactory)
        ));
        gameInitialiser.setCommandPreInitialisationProcessors(Arrays.asList(
                new TableClosedPreprocessor(destinationFactory),
                new TournamentPlayerValidator(destinationFactory),
                new GameDisabledPreprocessor(mockGameRepository, destinationFactory)));
        gameInitialiser.setPostInitialisationProcessors(Arrays.asList(
                new PlayerNotificationPostProcessor(destinationFactory, mockGameRepository),
                new ObserverNotificationPostprocessor(mockGameRepository, destinationFactory),
                new NotifyLocationChangesPostprocessor(mockGameRepository, playerRepository, chatRepository, locationService),
                new TableUpdatePostprocessor(mockGameRepository, timeSource)));

        GameEventExecutor eventExecutor = new GameEventExecutor(mockGameRepository, auditor, bufferedGameHostWalletFactory, novomaticRequestService);
        eventExecutor.setGameInitialiser(gameInitialiser);
        GameCommandExecutor commandExecutor = new GameCommandExecutor(mockGameRepository, auditor, bufferedGameHostWalletFactory, novomaticRequestService, playerRepository, destinationFactory);
        commandExecutor.setGameInitialiser(gameInitialiser);
        commandExecutor.setUuidSource(uuidSource);

        final GameHost gameHost = new GameHost(auditor, mockGameRepository, destinationFactory,
                playerRepository, bufferedGameHostWalletFactory, novomaticRequestService, uuidSource, eventExecutor, commandExecutor, gameInitialiser);


        gameHost.getEventExecutor().setGameInitialiser(gameInitialiser);
        gameHost.getCommandExecutor().setGameInitialiser(gameInitialiser);

        // this setup is a nasty side-effect of splitting initialisation of these classes out of the game host.
        // On the upside, it means the GH tests can in future just test the GH itself.


        gameHost.getEventExecutor().setPreExecutionProcessors(Arrays.asList(
                new AuditPreprocessor(auditor),
                new TableReadyEventPreprocessor(mockGameRepository),
                new NopEventPreprocessor(),
                new EventStillValidPreprocessor()));
        gameHost.getEventExecutor().setPostExecutionProcessors(Arrays.asList(
                new GameCompletePostprocessor(mockGameRepository, timeSource),
                new NotifyLocationChangesPostprocessor(mockGameRepository, playerRepository, chatRepository, locationService),
                new PlayerNotificationPostProcessor(destinationFactory, mockGameRepository),
                new ObserverNotificationPostprocessor(mockGameRepository, destinationFactory),
                new GameDisabledPostprocessor(mockGameRepository, destinationFactory),
                new TableUpdatePostprocessor(mockGameRepository, timeSource),
                new GameXPPublishingPostProcessor(playerStatisticEventsPublisher),
                new TableAuditPostprocessor(auditor, gameCompletePublisher, mockGameRepository)));

        gameHost.getCommandExecutor().setPreExecutionProcessors(Arrays.asList(
                new AuditPreprocessor(auditor),
                new TableClosingPreprocessor(destinationFactory),
                new InitialGetStatusPreprocessor(auditor, destinationFactory),
                new AcknowledgementPreprocessor()));

        gameHost.getCommandExecutor().setPostExecutionProcessors(Arrays.asList(
                new GameCompletePostprocessor(mockGameRepository, timeSource),
                new NotifyLocationChangesPostprocessor(mockGameRepository, playerRepository, chatRepository, locationService),
                new PlayerNotificationPostProcessor(destinationFactory, mockGameRepository),
                new ObserverNotificationPostprocessor(mockGameRepository, destinationFactory),
                new GameDisabledPostprocessor(mockGameRepository, destinationFactory),
                new TableUpdatePostprocessor(mockGameRepository, timeSource),
                new GameXPPublishingPostProcessor(playerStatisticEventsPublisher),
                new TableAuditPostprocessor(auditor, gameCompletePublisher, mockGameRepository)
        ));

        gameHost.setPlayerRemovalProcessors(Arrays.asList((PlayerRemovalProcessor)
                new NotifyLocationChangesPostprocessor(mockGameRepository, playerRepository, chatRepository, locationService)));

        return gameHost;
    }

}
