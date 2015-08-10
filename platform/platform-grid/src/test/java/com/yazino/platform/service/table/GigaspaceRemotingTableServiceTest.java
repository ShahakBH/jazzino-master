package com.yazino.platform.service.table;

import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.model.table.*;
import com.yazino.platform.persistence.SequenceGenerator;
import com.yazino.platform.persistence.table.TableDAO;
import com.yazino.platform.repository.table.*;
import com.yazino.platform.table.*;
import com.yazino.platform.util.Visitor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.collect.Sets.newHashSet;
import static com.yazino.platform.model.table.TableControlMessageType.SHUTDOWN;
import static com.yazino.platform.table.TableSearchOption.ONLY_OPEN;
import static com.yazino.platform.table.TableSearchOption.ONLY_WITH_PLAYERS;
import static java.math.BigDecimal.valueOf;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("NullableProblems")
public class GigaspaceRemotingTableServiceTest {

    private static final com.yazino.game.api.GameType GAME_TYPE = new com.yazino.game.api.GameType("BLACKJACK", "Blackjack", Collections.<String>emptySet());
    private static final String GAME_VARIATION_NAME = "testTemplate1";
    private static final String CLIENT_ID = "testClient";
    private static final BigDecimal TABLE_ID = valueOf(344234);
    private static final BigDecimal GAME_VARIATION_ID = valueOf(45345);
    private static final String TABLE_NAME = "myLittleTable";
    private static final BigDecimal OWNER_ID = new BigDecimal(123);
    private static final String GAME_ID = "GAME_ID";
    private static final String GAME_SHORT_NAME = "shortName";
    private static final String DISPLAY_NAME = "Display Name";

    private final Map<String, String> expectedProperties = new ConcurrentHashMap<>();

    @Mock
    private TableDAO tableDao;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private TableRepository tableRepository;
    @Mock
    private GameRepository gameRepository;
    @Mock
    private GameConfigurationRepository gameConfigurationRepository;
    @Mock
    private GameVariationRepository gameVariationRepository;
    @Mock
    private SequenceGenerator sequenceGenerator;
    @Mock
    private InternalTableService internalTableService;
    @Mock
    private com.yazino.game.api.GameRules gameRules;

    private Client client;
    private GigaspaceRemotingTableService underTest;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        underTest = new GigaspaceRemotingTableService(tableDao, clientRepository,
                tableRepository, gameRepository, gameConfigurationRepository, gameVariationRepository, internalTableService);

        client = new Client(CLIENT_ID);
        client.setGameType(GAME_TYPE.getId());
        client.setNumberOfSeats(5);
        client.setClientFile("aClientFile");

        expectedProperties.put("Hello", "Fellow");

        when(clientRepository.findById(CLIENT_ID)).thenReturn(client);
        when(clientRepository.findAll(GAME_TYPE.getId())).thenReturn(new Client[]{client});

        when(sequenceGenerator.next()).thenReturn(TABLE_ID);

        when(gameRepository.isGameAvailable(GAME_TYPE.getId())).thenReturn(true);
        when(gameRepository.getGameTypeFor(GAME_TYPE.getId())).thenReturn(GAME_TYPE);
        when(gameRepository.getGameRules(GAME_TYPE.getId())).thenReturn(gameRules);

        when(gameVariationRepository.getIdForName(GAME_VARIATION_NAME, GAME_TYPE.getId())).thenReturn(GAME_VARIATION_ID);
        GameVariation gameVariation = new GameVariation(GAME_VARIATION_ID, GAME_TYPE.getId(), GAME_VARIATION_NAME, expectedProperties);
        when(gameVariationRepository.findById(GAME_VARIATION_ID)).thenReturn(gameVariation);
        when(gameVariationRepository.variationsFor(GAME_TYPE.getId())).thenReturn(newHashSet(gameVariation));
    }

    @Test
    public void aPublicTableShouldBeCreatedByTheInternalServiceAndReturned()
            throws WalletServiceException, TableException {
        when(internalTableService.createPublicTable(GAME_TYPE.getId(), GAME_VARIATION_NAME, CLIENT_ID, TABLE_NAME, Collections.<String>emptySet()))
                .thenReturn(aTableNamed("aTestTable").summarise(gameRules));

        final TableSummary table = underTest.createPublicTable(GAME_TYPE.getId(), GAME_VARIATION_NAME,
                CLIENT_ID, TABLE_NAME, Collections.<String>emptySet());

        assertThat(table, is(equalTo(aTableNamed("aTestTable").summarise(gameRules))));
    }

    @Test
    public void aPublicTableUsingTheDefaultTemplateAndVariationShouldBeCreatedAndTheSummaryReturned()
            throws WalletServiceException, TableException {
        when(internalTableService.createPublicTable(GAME_TYPE.getId(), TABLE_NAME, Collections.<String>emptySet()))
                .thenReturn(aTableNamed("anotherTestTable").summarise(gameRules));

        final TableSummary table = underTest.createPublicTable(GAME_TYPE.getId(), TABLE_NAME, Collections.<String>emptySet());

        assertThat(table, is(equalTo(aTableNamed("anotherTestTable").summarise(gameRules))));
    }


    @Test
    public void aPrivateTableShouldBeCreatedAndTheSummaryReturned()
            throws WalletServiceException, TableException {
        when(internalTableService.createPrivateTableForPlayer(GAME_TYPE.getId(), GAME_VARIATION_NAME, CLIENT_ID, TABLE_NAME, OWNER_ID))
                .thenReturn(aPrivateTableNamed(TABLE_NAME).summarise(gameRules));

        final TableSummary table = underTest.createPrivateTableForPlayer(
                GAME_TYPE.getId(), GAME_VARIATION_NAME, CLIENT_ID, TABLE_NAME, OWNER_ID);

        assertThat(table, is(equalTo(aPrivateTableNamed(TABLE_NAME).summarise(gameRules))));
    }

    @Test
    public void aGameSummaryCanBeRetrievedById() {
        when(tableRepository.findById(TABLE_ID)).thenReturn(aTableNamed("bob"));

        final TableGameSummary actualTable = underTest.findGameSummaryById(TABLE_ID);

        assertThat(actualTable, is(equalTo(aGameSummaryOf(aTableNamed("bob")))));
    }

    @Test(expected = NullPointerException.class)
    public void findingAGameSummaryByANullIdThrowsAnIllegalArgumentException() {
        underTest.findGameSummaryById(null);
    }

    @Test
    public void aTableSummaryCanBeRetrievedById() {
        when(tableRepository.findById(TABLE_ID)).thenReturn(aTableNamed("bob"));

        final TableSummary actualTable = underTest.findSummaryById(TABLE_ID);

        assertThat(actualTable, is(equalTo(aSummaryOf(aTableNamed("bob")))));
    }

    @Test(expected = NullPointerException.class)
    public void findingASummaryByANullIdThrowsAnIllegalArgumentException() {
        underTest.findSummaryById(null);
    }

    @Test
    public void aNullResultForASummaryReturnsNull() {
        final TableSummary actualTable = underTest.findSummaryById(TABLE_ID);

        assertThat(actualTable, is(nullValue()));
    }

    @Test
    public void closeRequestsShouldBeWrittenToTheSpace() {
        underTest.closeTable(TABLE_ID);

        verify(tableRepository).sendControlMessage(TABLE_ID, TableControlMessageType.CLOSE);
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void loadAllShouldGenerateAControlMessageForAllTables() {
        doAnswer(new Answer() {
            @SuppressWarnings({"unchecked"})
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final Visitor<Table> visitor = (Visitor<Table>) invocation.getArguments()[1];
                visitor.visit(aTable(valueOf(1)));
                visitor.visit(aTable(valueOf(2)));
                visitor.visit(aTable(valueOf(3)));
                return null;
            }
        }).when(tableDao).visitTables(eq(TableStatus.open), (Visitor<Table>) any());

        underTest.loadAll();

        verify(tableRepository).sendControlMessage(valueOf(1), TableControlMessageType.LOAD);
        verify(tableRepository).sendControlMessage(valueOf(2), TableControlMessageType.LOAD);
        verify(tableRepository).sendControlMessage(valueOf(3), TableControlMessageType.LOAD);
    }

    @Test
    public void unloadRequestsShouldGenerateAControlMessage() {
        underTest.unload(TABLE_ID);

        verify(tableRepository).sendControlMessage(TABLE_ID, TableControlMessageType.UNLOAD);
    }

    @Test
    public void reOpenRequestsShouldGenerateAControlMessage() {
        underTest.reOpen(TABLE_ID);

        verify(tableRepository).sendControlMessage(TABLE_ID, TableControlMessageType.REOPEN);
    }

    @Test
    public void shutdownRequestsShouldGenerateAControlMessage() {
        underTest.shutdown(TABLE_ID);

        verify(tableRepository).sendControlMessage(TABLE_ID, SHUTDOWN);
    }

    @Test
    public void resetRequestsShouldGenerateAControlMessage() {
        underTest.reset(TABLE_ID);

        verify(tableRepository).sendControlMessage(TABLE_ID, TableControlMessageType.RESET);
    }

    @Test
    public void replacingTheGameStatusShouldSendATableRequest() {
        final com.yazino.game.api.GameStatus newStatus = mock(com.yazino.game.api.GameStatus.class);

        underTest.testReplaceGame(TABLE_ID, newStatus);

        verify(tableRepository).sendRequest(new TestAlterGameRequest(TABLE_ID, newStatus));
    }

    @Test(expected = NullPointerException.class)
    public void replacingTheGameStatusShouldRejectANullTableId() {
        underTest.testReplaceGame(null, mock(com.yazino.game.api.GameStatus.class));
    }

    @Test(expected = NullPointerException.class)
    public void replacingTheGameStatusShouldRejectANullGameStatus() {
        underTest.testReplaceGame(TABLE_ID, null);
    }

    @Test(expected = NullPointerException.class)
    public void closeRequestsWithNullTableIdsAreRejected() {
        underTest.closeTable(null);
    }

    @Test
    public void tablesForPlayerAreReturnedPartitionedByGameType() {
        final Table blackjackTable = aPublicTableNamed("BJ");
        blackjackTable.setGameTypeId("BLACKJACK");
        final Table slotsTable = aPublicTableNamed("Slots");
        slotsTable.setGameTypeId("SLOTS");

        when(tableRepository.findAllTablesOwnedByPlayer(OWNER_ID)).thenReturn(newHashSet(blackjackTable.summarise(gameRules), slotsTable.summarise(gameRules)));

        final Map<String, TableSummary> allTablesOwnedByPlayer = underTest.findAllTablesOwnedByPlayer(OWNER_ID);

        assertThat(allTablesOwnedByPlayer.get("BLACKJACK"), is(equalTo(aSummaryOf(blackjackTable))));
        assertThat(allTablesOwnedByPlayer.get("SLOTS"), is(equalTo(aSummaryOf(slotsTable))));
    }

    @Test
    public void anEmptyMapIsReturnedWhenAPlayerHasNoTables() {
        when(tableRepository.findAllTablesOwnedByPlayer(OWNER_ID)).thenReturn(null);

        final Map<String, TableSummary> allTablesOwnedByPlayer = underTest.findAllTablesOwnedByPlayer(OWNER_ID);

        assertThat(allTablesOwnedByPlayer.size(), is(equalTo(0)));
    }

    @Test(expected = NullPointerException.class)
    public void findingAllTablesForAPlayerShouldRejectsANullPlayerId() {
        underTest.findAllTablesOwnedByPlayer(null);
    }

    @Test(expected = NullPointerException.class)
    public void findByTypeRejectsNullTypes() {
        underTest.findByType(null, 0);
    }

    @Test
    public void findByTypePassesTypesWithNoOptionsToTheRepository() {
        final int page = 0;
        when(tableRepository.findByType(TableType.PUBLIC, page)).thenReturn(
                new PagedData<>(page, 2, 2, asList(aTableNamed("bob").summarise(gameRules), aTableNamed("fred").summarise(gameRules))));

        final PagedData<TableSummary> tables = underTest.findByType(TableType.PUBLIC, page);
        assertThat(tables.getData(), is(equalTo((Collection<TableSummary>) asList(
                aSummaryOf(aTableNamed("bob")), aSummaryOf(aTableNamed("fred"))))));
        assertThat(tables.getStartPosition(), is(equalTo(page)));
        assertThat(tables.getTotalSize(), is(equalTo(2)));
        assertThat(tables.getSize(), is(equalTo(2)));
    }

    @Test
    public void findByTypePassesTypesWithOptionsToTheRepository() {
        final int page = 1;
        when(tableRepository.findByType(TableType.PUBLIC, page, ONLY_OPEN, ONLY_WITH_PLAYERS)).thenReturn(
                new PagedData<>(page, 2, 2, asList(aTableNamed("bob").summarise(gameRules), aTableNamed("fred").summarise(gameRules))));

        final PagedData<TableSummary> tables = underTest.findByType(TableType.PUBLIC, page, ONLY_OPEN, ONLY_WITH_PLAYERS);
        assertThat(tables.getData(), is(equalTo((Collection<TableSummary>)
                asList(aSummaryOf(aTableNamed("bob")), aSummaryOf(aTableNamed("fred"))))));
        assertThat(tables.getStartPosition(), is(equalTo(page)));
        assertThat(tables.getTotalSize(), is(equalTo(2)));
        assertThat(tables.getSize(), is(equalTo(2)));
    }

    @Test(expected = NullPointerException.class)
    public void countByTypeRejectsNullTypes() {
        underTest.countByType(null);
    }

    @Test
    public void countByTypePassesTypesWithNoOptionsToTheRepository() {
        when(tableRepository.countByType(TableType.PUBLIC)).thenReturn(6);

        final int tables = underTest.countByType(TableType.PUBLIC);
        assertThat(tables, is(equalTo(6)));
    }

    @Test
    public void countByTypePassesTypesWithOptionsToTheRepository() {
        when(tableRepository.countByType(TableType.PUBLIC, ONLY_OPEN, ONLY_WITH_PLAYERS)).thenReturn(8);

        final int tables = underTest.countByType(TableType.PUBLIC, ONLY_OPEN, ONLY_WITH_PLAYERS);

        assertThat(tables, is(equalTo(8)));
    }

    @Test
    public void countTablesWithPlayersDelegatesToTheGlobalRepository() {
        when(tableRepository.countTablesWithPlayers("aGameType")).thenReturn(6);

        final int count = underTest.countTablesWithPlayers("aGameType");

        assertThat(count, is(equalTo(6)));
    }

    @Test(expected = NullPointerException.class)
    public void forcingANewGameRejectsNullTableIds() {
        final BigDecimal playerId = valueOf(12);
        final com.yazino.game.api.PlayerAtTableInformation playerInfo = new com.yazino.game.api.PlayerAtTableInformation(
                new com.yazino.game.api.GamePlayer(playerId, null, "aPlayer"), singletonMap("aProperty", "aValue"));

        underTest.forceNewGame(null, asList(playerInfo), valueOf(113), "aClientId", singletonMap(playerId, valueOf(104)));
    }

    @Test
    public void forcingANewGameDelegatesToTheLocalRepository() {
        final BigDecimal playerId = valueOf(12);
        final com.yazino.game.api.PlayerAtTableInformation playerInfo = new com.yazino.game.api.PlayerAtTableInformation(
                new com.yazino.game.api.GamePlayer(playerId, null, "aPlayer"), singletonMap("aProperty", "aValue"));

        underTest.forceNewGame(valueOf(18), asList(playerInfo), valueOf(113), "aClientId", singletonMap(playerId, valueOf(104)));

        verify(tableRepository).forceNewGame(valueOf(18), asList(playerInfo), valueOf(113), "aClientId", singletonMap(playerId, valueOf(104)));
    }

    @Test(expected = NullPointerException.class)
    public void sendCommandShouldRejectANullCommand() {
        underTest.sendCommand(null);
    }

    @Test
    public void sendCommandShouldDelegateToTheLocalRepository() {
        final Command command = new Command(valueOf(100), 200L, valueOf(300), null, "aCommand");
        when(tableRepository.sendRequest(new CommandWrapper(command))).thenReturn("anId");

        underTest.sendCommand(command);

        verify(tableRepository).sendRequest(new CommandWrapper(command));
    }

    @Test
    public void countingOutstandingRequestsDelegatesToTheGlobalRepository() {
        when(tableRepository.countOutstandingRequests()).thenReturn(10245);

        final int outstandingReqs = underTest.countOutstandingRequests();

        assertThat(outstandingReqs, is(equalTo(10245)));
    }

    @Test(expected = NullPointerException.class)
    public void countingOutstandingRequestsForATableRejectsANullTableId() {
        underTest.countOutstandingRequests(null);
    }

    @Test
    public void countingOutstandingRequestsForATableDelegatesToTheGlobalRepository() {
        when(tableRepository.countOutstandingRequests(valueOf(234))).thenReturn(20245);

        final int outstandingReqs = underTest.countOutstandingRequests(valueOf(234));

        assertThat(outstandingReqs, is(equalTo(20245)));
    }

    @Test
    public void findingAVariationByIdShouldFetchItFromTheRepository() {
        final GameVariation expectedGameVariation = new GameVariation(
                GAME_VARIATION_ID, GAME_TYPE.getId(), GAME_VARIATION_NAME, expectedProperties);
        when(gameVariationRepository.findById(GAME_VARIATION_ID)).thenReturn(expectedGameVariation);

        final GameVariation gameVariation = underTest.getGameVariation(GAME_VARIATION_ID);

        assertThat(gameVariation, is(equalTo(expectedGameVariation)));
    }

    @Test
    public void findingANonExistentVariationByIdShouldReturnNull() {
        when(gameVariationRepository.findById(GAME_VARIATION_ID)).thenReturn(null);

        final GameVariation gameVariation = underTest.getGameVariation(GAME_VARIATION_ID);

        assertThat(gameVariation, is(nullValue()));
    }

    @Test(expected = NullPointerException.class)
    public void findingAVariationWithANullIdShouldThrowAnException() {
        underTest.getGameVariation(null);
    }

    @Test
    public void shouldFindAllGameConfigurations() throws Exception {
        GameConfiguration gameConfiguration = new GameConfiguration(GAME_ID, GAME_SHORT_NAME, DISPLAY_NAME, Arrays.asList("g1", "ag1"), 0);
        gameConfiguration = gameConfiguration.withProperties(Arrays.asList(new GameConfigurationProperty(BigDecimal.ONE, GAME_ID, "propertyName", "value")));
        when(gameConfigurationRepository.retrieveAll()).thenReturn(Arrays.asList(gameConfiguration));

        Collection<GameConfiguration> gameConfigurations = underTest.getGameConfigurations();

        assertThat(gameConfigurations.size(), is(equalTo(1)));
        assertThat(gameConfigurations, hasItem(gameConfiguration));
    }


    @Test
    public void findingATableByGameTypeAndPlayerShouldQueryTheRepository() {
        when(tableRepository.findTableByGameTypeAndPlayer(GAME_TYPE.getId(), OWNER_ID))
                .thenReturn(aPrivateTableNamed(TABLE_NAME).summarise(gameRules));

        final TableSummary table = underTest.findTableByGameTypeAndPlayerId(GAME_TYPE.getId(), OWNER_ID);

        assertThat(table, is(equalTo(aSummaryOf(aPrivateTableNamed(TABLE_NAME)))));
    }

    @Test
    public void findingATableByGameTypeAndPlayerShouldReturnNullIfTheRepositoryDoesSo() {
        final TableSummary table = underTest.findTableByGameTypeAndPlayerId(GAME_TYPE.getId(), OWNER_ID);

        assertThat(table, is(nullValue()));
    }

    @Test(expected = NullPointerException.class)
    public void findingATableByGameTypeAndPlayerShouldRejectANullGameType() {
        underTest.findTableByGameTypeAndPlayerId(null, OWNER_ID);
    }

    @Test(expected = NullPointerException.class)
    public void findingATableByGameTypeAndPlayerShouldRejectANullPlayerId() {
        underTest.findTableByGameTypeAndPlayerId(GAME_TYPE.getId(), null);
    }

    private Table aPublicTableNamed(final String tableName) {
        final Table table = aTableNamed(tableName);
        table.setShowInLobby(true);
        table.setTableStatus(TableStatus.open);
        return table;
    }

    private Table aPrivateTableNamed(final String tableName) {
        final Table table = aTableNamed(tableName);
        table.setShowInLobby(false);
        table.setTableStatus(TableStatus.open);
        table.setOwnerId(OWNER_ID);
        return table;
    }

    private Table aTable(final BigDecimal tableId) {
        final Table table = new Table();
        table.setTableId(tableId);
        return table;
    }

    private Table aTableNamed(final String tableName) {
        final Table table = new Table(GAME_TYPE, GAME_VARIATION_ID, CLIENT_ID, true);
        table.setTableName(tableName);
        table.setTableId(TABLE_ID);
        table.setClient(client);
        table.setFull(false);
        table.setTableStatus(TableStatus.open);
        table.setTemplateName(GAME_VARIATION_NAME);
        table.setVariationProperties(expectedProperties);
        return table;
    }

    private TableGameSummary aGameSummaryOf(final Table table) {
        if (table == null) {
            return null;
        }

        return table.summariseGame();
    }

    private TableSummary aSummaryOf(final Table table) {
        if (table == null) {
            return null;
        }

        return table.summarise(gameRules);
    }

}
