package com.yazino.platform.repository.table;


import com.google.common.base.Function;
import com.yazino.game.api.GameStatus;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.model.table.*;
import com.yazino.platform.table.TableSearchOption;
import com.yazino.platform.table.TableStatus;
import com.yazino.platform.table.TableSummary;
import com.yazino.platform.table.TableType;
import com.yazino.platform.test.PrintlnRules;
import com.yazino.platform.test.PrintlnStatus;
import com.yazino.platform.util.Visitor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

import static com.google.common.collect.Sets.newHashSet;
import static com.yazino.platform.table.TableSearchOption.*;
import static com.yazino.platform.table.TableType.*;
import static java.math.BigDecimal.valueOf;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.apache.commons.lang3.Validate.notNull;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class GigaspaceTableRepositoryIntegrationTest {
    private static final com.yazino.game.api.GameType GAME_TYPE = new com.yazino.game.api.GameType("PRINTLN", "PrintLn", Collections.<String>emptySet());
    private static final String GAME_VARIATION_TEMPLATE_NAME = "bob-the-template";
    private static final String CLIENT_ID = "testClient";
    private static final int PAGE_SIZE = 20;

    private long tableIdSource;
    private BigDecimal tableId = valueOf(1);
    private Table tableTemplate;
    private Table table;

    @Autowired
    private GigaSpace tableSpace;
    @Autowired
    private GigaSpace referenceSpace;
    @Autowired
    private TableRepository underTest;
    @Autowired
    private PrintlnRules gameRules;

    @Before
    public void init() {
        tableSpace.clear(null);

        referenceSpace.write(new Client(CLIENT_ID));
        table = new Table(GAME_TYPE, null, "test", true);
        table.setTableId(tableId);
        table.setTableStatus(TableStatus.open);
        table.setVariationProperties(new HashMap<String, String>());

        tableTemplate = new Table();
        tableTemplate.setTableId(tableId);
    }

    @Test
    public void persistence_request_is_stored_after_processing() {
        underTest.save(table);
        TablePersistenceRequest tpw = tableSpace.read(new TablePersistenceRequest(tableId));
        Assert.assertNotNull(tpw);
    }

    @Test
    public void lastUpdatedTimeIsSetOnSave() {
        table.setLastUpdated(null);
        final long timestampLowerBound = System.currentTimeMillis();

        underTest.save(table);

        final long timestampUpperBound = System.currentTimeMillis();

        assertThat(table.getLastUpdated(), is(greaterThanOrEqualTo(timestampLowerBound)));
        assertThat(table.getLastUpdated(), is(lessThanOrEqualTo(timestampUpperBound)));
    }

    @Test
    public void noPersistenceRequestIsGeneratedByAQuickSave() {
        underTest.nonPersistentSave(table);
        TablePersistenceRequest tpw = tableSpace.read(new TablePersistenceRequest(tableId));

        assertThat(tpw, is(nullValue()));
    }

    @Test
    public void tableIsSavedToSpaceOnQuickSave() {
        underTest.nonPersistentSave(table);

        final Table savedTable = tableSpace.readById(Table.class, tableId);

        assertThat(savedTable, is(equalTo(table)));
    }


    @Test
    public void tableAndTableInfoAreRemovedOnUnload() {
        tableSpace.write(tableTemplate);
        underTest.unload(tableId);
        Assert.assertNull(tableSpace.read(tableTemplate));
    }

    @Test
    public void testFindByIdReturnsTableFromSpace() {
        tableSpace.write(tableTemplate);
        assertEquals(tableId, underTest.findById(tableId).getTableId());
    }

    @Test
    public void saveStoresTableWithClient() {
        referenceSpace.write(new Client("TestClient"));
        table.setClientId("TestClient");
        underTest.save(table);
        Assert.assertEquals("TestClient", tableSpace.read(tableTemplate).getClient().getClientId());
    }


    @Test
    @Transactional
    public void countTableWithPlayersForAParticularGame() {
        createTableWithPlayers(0, TableStatus.open, false, true, true, true);
        createTableWithPlayers(1, TableStatus.open, false, false, true, true);
        createTableWithPlayers(2, TableStatus.open, false, true, true, true);
        createTableWithPlayers(3, TableStatus.open, false, true, false, true);
        assertEquals(2, underTest.countTablesWithPlayers(GAME_TYPE.getId()));
    }

    @Test
    @Transactional
    public void findByGameTypeAndPlayerIdQueriesGigaspaceWithTemplate() {
        Table expectedTable = new Table();
        expectedTable.setGameTypeId(GAME_TYPE.getId());
        expectedTable.setOwnerId(new BigDecimal(123));
        expectedTable.setTableId(new BigDecimal(321));
        expectedTable.setTableStatus(TableStatus.open);

        tableSpace.write(expectedTable);

        assertEquals(expectedTable.summarise(gameRules), underTest.findTableByGameTypeAndPlayer(GAME_TYPE.getId(), new BigDecimal(123)));
    }

    @Test
    @Transactional
    public void findAllPrivateTablesOwnedByPlayer_shouldReturnEmptySet() {
        Set<TableSummary> actualTables = underTest.findAllTablesOwnedByPlayer(BigDecimal.ONE);
        assertNotNull(actualTables);
        assertTrue(actualTables.isEmpty());
    }

    @Test
    @Transactional
    public void findAllPrivateTablesOwnedByPlayer_shouldAllOwnedTables() {
        BigDecimal expectedOwnerId = BigDecimal.ONE;
        Table[] expectedTables = {
                createPrivateTable(expectedOwnerId, GAME_TYPE.getId(), BigDecimal.ONE, TableStatus.open),
                createPrivateTable(expectedOwnerId, GAME_TYPE.getId(), valueOf(2), TableStatus.open)};
        createPrivateTable(BigDecimal.TEN, GAME_TYPE.getId(), valueOf(3), TableStatus.open);
        createPrivateTable(valueOf(8778), GAME_TYPE.getId(), valueOf(98), TableStatus.open);

        ArrayList<TableSummary> expectedTableSummaries = transform(Arrays.asList(expectedTables), new TableSummaryTransformer());

        Set<TableSummary> actualTableSummaries = underTest.findAllTablesOwnedByPlayer(expectedOwnerId);
        assertNotNull(actualTableSummaries);
        assertEquals(expectedTables.length, actualTableSummaries.size());
        assertTrue(Arrays.asList(expectedTableSummaries.toArray()).containsAll(actualTableSummaries));
    }

    @Test
    public void findOnlyOpenTablesOwnedByPlayer() throws Exception {
        BigDecimal expectedOwnerId = BigDecimal.ONE;

        Table table1 = createPrivateTable(expectedOwnerId, PrintlnRules.GAME_TYPE, valueOf(1), TableStatus.open);
        Table table2 = createPrivateTable(valueOf(4), "ROULETTE", valueOf(2), TableStatus.open);
        Table table3 = createPrivateTable(expectedOwnerId, "TEXAS_HOLDEM", valueOf(3), TableStatus.closed);
        tableSpace.write(table1);
        tableSpace.write(table2);
        tableSpace.write(table3);

        Set<TableSummary> actualTables = underTest.findAllTablesOwnedByPlayer(expectedOwnerId);
        assertEquals(1, actualTables.size());
        assertTrue(actualTables.contains(table1.summarise(gameRules)));

    }

    @Test
    public void findOnlyOpenTablesOnlyForGameType() throws Exception {
        Table table1 = createTableWithPlayers(1, TableStatus.open, true, true, true, true);
        Table table2 = createPrivateTable(valueOf(4), "ROULETTE", valueOf(2), TableStatus.open);
        Table table3 = createPrivateTable(valueOf(1), "TEXAS_HOLDEM", valueOf(3), TableStatus.closed);
        tableSpace.write(table1);
        tableSpace.write(table2);
        tableSpace.write(table3);

        Set<BigDecimal> actualTables = underTest.findAllTablesForGameType(GAME_TYPE.getId());
        assertEquals(1, actualTables.size());
        assertTrue(actualTables.contains(table1.getTableId()));
    }

    @Test
    public void shouldFindAllPlayersOnTables() {
        createTableWithPlayers(1, TableStatus.open, true, true, true, true);
        Set<BigDecimal> playerIds = underTest.findAllLocalTablesWithPlayers();

        assertEquals(1, playerIds.size());
    }

    @Test
    public void shouldFindAllTablesWithPlayers() throws Exception {
        Table table1 = createTableWithPlayers(1, TableStatus.open, true, true, true, true);
        Table table2 = createTableWithPlayers(2, TableStatus.open, false, true, true, true);
        Table table3 = createTableWithPlayers(3, TableStatus.open, false, false, true, false);
        Table table4 = createTableWithPlayers(4, TableStatus.open, true, true, false, true);

        Set<BigDecimal> tableIds = underTest.findAllLocalTablesWithPlayers();
        assertEquals(3, tableIds.size());
        assertTrue(tableIds.contains(table1.getTableId()));
        assertTrue(tableIds.contains(table2.getTableId()));
        assertTrue(tableIds.contains(table3.getTableId()));
        assertFalse(tableIds.contains(table4.getTableId()));
    }


    @Test
    public void shouldSaveAReservationObjectToSpace() throws Exception {
        underTest.makeReservationAtTable(BigDecimal.valueOf(11), BigDecimal.valueOf(3));
        assertEquals(1, tableSpace.count(new TableReservation()));
        underTest.makeReservationAtTable(BigDecimal.valueOf(11), BigDecimal.valueOf(3));
        assertEquals(2, tableSpace.count(new TableReservation()));

        underTest.makeReservationAtTable(BigDecimal.valueOf(12), BigDecimal.valueOf(3));
        assertEquals(3, tableSpace.count(new TableReservation()));
    }

    @Test
    public void shouldRemoveReservation() {
        BigDecimal playerId = BigDecimal.valueOf(3);
        BigDecimal tableId = BigDecimal.valueOf(11);
        underTest.makeReservationAtTable(tableId, playerId);
        assertEquals(1, tableSpace.count(new TableReservation()));
        underTest.removeReservationForTable(tableId, playerId);
        assertEquals(0, tableSpace.count(new TableReservation()));
    }

    @Test
    public void shouldNotBreakIfReservationToBeRemovedIsNotInSpace() {
        BigDecimal playerId = BigDecimal.valueOf(3);
        BigDecimal playerId2 = BigDecimal.valueOf(5);
        BigDecimal tableId = BigDecimal.valueOf(11);
        underTest.makeReservationAtTable(tableId, playerId);
        assertEquals(1, tableSpace.count(new TableReservation()));
        underTest.removeReservationForTable(tableId, playerId2);
        assertEquals(1, tableSpace.count(new TableReservation()));
    }

    @Test(expected = NullPointerException.class)
    public void findByTypesRejectsNullTypes() {
        underTest.findByType(null, 0);
    }

    @Test
    public void countByTypesCountsPublicTables() {
        tableSpace.write(tableWith(1000, PUBLIC));
        tableSpace.write(tableWith(1001, PRIVATE));
        tableSpace.write(tableWith(1002, TOURNAMENT));
        tableSpace.write(tableWith(1003, PUBLIC));

        final int numberOfTables = underTest.countByType(PUBLIC);

        assertThat(numberOfTables, is(equalTo(2)));
    }

    @Test
    public void findByTypesReturnsPublicTables() {
        tableSpace.write(tableWith(1000, PUBLIC));
        tableSpace.write(tableWith(1001, PRIVATE));
        tableSpace.write(tableWith(1002, TOURNAMENT));
        tableSpace.write(tableWith(1003, PUBLIC));

        final PagedData<TableSummary> tables = underTest.findByType(PUBLIC, 0);

        assertThat(tables.getSize(), is(equalTo(2)));
        assertThat(tables, hasItem(tableWith(1000, PUBLIC).summarise(gameRules)));
        assertThat(tables, hasItem(tableWith(1003, PUBLIC).summarise(gameRules)));
    }

    @Test
    public void findByTypesReturnsPagedDataForFirstPage() {
        final int totalSize = 45;
        final int pageSize = 20;
        for (int i = 0; i < totalSize; ++i) {
            tableSpace.write(tableWith(1000 + i, PUBLIC));
        }

        final PagedData<TableSummary> tables = underTest.findByType(PUBLIC, 0);

        assertThat(tables.getStartPosition(), is(equalTo(0)));
        assertThat(tables.getSize(), is(equalTo(pageSize)));
        assertThat(tables.getTotalSize(), is(equalTo(totalSize)));
        for (int i = 0; i < pageSize; ++i) {
            assertThat(tables, hasItem(tableWith(1000 + i, PUBLIC).summarise(gameRules)));
        }
    }

    @Test
    public void findByTypesReturnsPagedDataForMiddlePage() {
        final int totalSize = 45;
        for (int i = 0; i < totalSize; ++i) {
            tableSpace.write(tableWith(1000 + i, PUBLIC));
        }

        final PagedData<TableSummary> tables = underTest.findByType(PUBLIC, 1);

        assertThat(tables.getStartPosition(), is(equalTo(PAGE_SIZE)));
        assertThat(tables.getSize(), is(equalTo(PAGE_SIZE)));
        assertThat(tables.getTotalSize(), is(equalTo(totalSize)));
        for (int i = PAGE_SIZE; i < PAGE_SIZE * 2; ++i) {
            assertThat(tables, hasItem(tableWith(1000 + i, PUBLIC).summarise(gameRules)));
        }
    }

    @Test
    public void findByTypesReturnsPagedDataForLastPage() {
        final int totalSize = 45;
        for (int i = 0; i < totalSize; ++i) {
            tableSpace.write(tableWith(1000 + i, PUBLIC));
        }

        final PagedData<TableSummary> tables = underTest.findByType(PUBLIC, 2);

        assertThat(tables.getStartPosition(), is(equalTo(2 * PAGE_SIZE)));
        assertThat(tables.getSize(), is(equalTo(totalSize % PAGE_SIZE)));
        assertThat(tables.getTotalSize(), is(equalTo(totalSize)));
        for (int i = PAGE_SIZE * 2; i < totalSize; ++i) {
            assertThat(tables, hasItem(tableWith(1000 + i, PUBLIC).summarise(gameRules)));
        }
    }

    @Test
    public void findByTypesReturnsEmptyPagedDataForAnInvalidPage() {
        final int totalSize = 45;
        for (int i = 0; i < totalSize; ++i) {
            tableSpace.write(tableWith(1000 + i, PUBLIC));
        }

        final PagedData<TableSummary> tables = underTest.findByType(PUBLIC, 600);

        assertThat(tables.getStartPosition(), is(equalTo(600 * PAGE_SIZE)));
        assertThat(tables.getSize(), is(equalTo(0)));
        assertThat(tables.getTotalSize(), is(equalTo(totalSize)));
        assertThat(tables.getData().size(), is(equalTo(0)));
    }

    @Test
    public void findByTypesReturnsPrivateTables() {
        tableSpace.write(tableWith(1000, PUBLIC));
        tableSpace.write(tableWith(1001, PRIVATE));
        tableSpace.write(tableWith(1002, TOURNAMENT));
        tableSpace.write(tableWith(1003, PRIVATE));

        final PagedData<TableSummary> tables = underTest.findByType(PRIVATE, 0);

        assertThat(tables.getSize(), is(equalTo(2)));
        assertThat(tables, hasItem(tableWith(1001, PRIVATE).summarise(gameRules)));
        assertThat(tables, hasItem(tableWith(1003, PRIVATE).summarise(gameRules)));
    }

    @Test
    public void countByTypeCountsPrivateTables() {
        tableSpace.write(tableWith(1000, PUBLIC));
        tableSpace.write(tableWith(1001, PRIVATE));
        tableSpace.write(tableWith(1002, TOURNAMENT));
        tableSpace.write(tableWith(1003, PRIVATE));

        final int numberOfTables = underTest.countByType(PRIVATE);

        assertThat(numberOfTables, is(equalTo(2)));
    }

    @Test
    public void findByTypeReturnsTournamentTables() {
        tableSpace.write(tableWith(1000, PUBLIC));
        tableSpace.write(tableWith(1001, PRIVATE));
        tableSpace.write(tableWith(1002, TOURNAMENT));
        tableSpace.write(tableWith(1003, TOURNAMENT));

        final PagedData<TableSummary> tables = underTest.findByType(TOURNAMENT, 0);

        assertThat(tables.getSize(), is(equalTo(2)));
        assertThat(tables, hasItem(tableWith(1002, TOURNAMENT).summarise(gameRules)));
        assertThat(tables, hasItem(tableWith(1003, TOURNAMENT).summarise(gameRules)));
    }

    @Test
    public void countByTypeCountsTournamentTables() {
        tableSpace.write(tableWith(1000, PUBLIC));
        tableSpace.write(tableWith(1001, PRIVATE));
        tableSpace.write(tableWith(1002, TOURNAMENT));
        tableSpace.write(tableWith(1003, TOURNAMENT));

        final int numberOfTables = underTest.countByType(TOURNAMENT);

        assertThat(numberOfTables, is(equalTo(2)));
    }

    @Test
    public void findByTypesOnlyReturnsTablesInAnErrorStateIfTheSearchFlagIsPresent() {
        tableSpace.write(tableWith(1000, PUBLIC));
        tableSpace.write(tableWith(1001, PUBLIC, IN_ERROR_STATE));
        tableSpace.write(tableWith(1002, PRIVATE));
        tableSpace.write(tableWith(1003, PUBLIC, IN_ERROR_STATE));

        final PagedData<TableSummary> tables = underTest.findByType(PUBLIC, 0, IN_ERROR_STATE);

        assertThat(tables.getSize(), is(equalTo(2)));
        assertThat(tables, hasItem(tableWith(1001, PUBLIC, IN_ERROR_STATE).summarise(gameRules)));
        assertThat(tables, hasItem(tableWith(1003, PUBLIC, IN_ERROR_STATE).summarise(gameRules)));
    }

    @Test
    public void findByTypesOnlyReturnsTablesWithPlayersIfTheSearchFlagIsPresent() {
        tableSpace.write(tableWith(1000, PUBLIC));
        tableSpace.write(tableWith(1001, PUBLIC, ONLY_WITH_PLAYERS));
        tableSpace.write(tableWith(1002, PRIVATE));
        tableSpace.write(tableWith(1003, PUBLIC, ONLY_WITH_PLAYERS));

        final PagedData<TableSummary> tables = underTest.findByType(PUBLIC, 0, ONLY_WITH_PLAYERS);

        assertThat(tables.getSize(), is(equalTo(2)));
        assertThat(tables, hasItem(tableWith(1001, PUBLIC, ONLY_WITH_PLAYERS).summarise(gameRules)));
        assertThat(tables, hasItem(tableWith(1003, PUBLIC, ONLY_WITH_PLAYERS).summarise(gameRules)));
    }

    @Test
    public void findByTypesOnlyReturnsTablesThatAreOpenIfTheSearchFlagIsPresent() {
        tableSpace.write(tableWith(1000, PRIVATE, ONLY_OPEN));
        tableSpace.write(tableWith(1001, PUBLIC, ONLY_OPEN));
        tableSpace.write(tableWith(1002, PRIVATE));
        tableSpace.write(tableWith(1003, PUBLIC, ONLY_OPEN));

        final PagedData<TableSummary> tables = underTest.findByType(PUBLIC, 0, ONLY_OPEN);

        assertThat(tables.getSize(), is(equalTo(2)));
        assertThat(tables, hasItem(tableWith(1001, PUBLIC, ONLY_OPEN).summarise(gameRules)));
        assertThat(tables, hasItem(tableWith(1003, PUBLIC, ONLY_OPEN).summarise(gameRules)));
    }

    @Test
    public void findByTypesHonoursSearchFlagsInCombination() {
        tableSpace.write(tableWith(1000, PUBLIC, ONLY_WITH_PLAYERS));
        tableSpace.write(tableWith(1001, PUBLIC, ONLY_OPEN, ONLY_WITH_PLAYERS, IN_ERROR_STATE));
        tableSpace.write(tableWith(1002, PRIVATE));
        tableSpace.write(tableWith(1003, PUBLIC, ONLY_OPEN));

        final PagedData<TableSummary> tables = underTest.findByType(PUBLIC, 0, IN_ERROR_STATE, ONLY_OPEN, ONLY_WITH_PLAYERS);

        assertThat(tables.getSize(), is(equalTo(1)));
        assertThat(tables, hasItem(tableWith(1001, PUBLIC, ONLY_OPEN, ONLY_WITH_PLAYERS, IN_ERROR_STATE).summarise(gameRules)));
    }

    @Test
    public void controlMessagesAreWrittenToTheSpace() {
        underTest.sendControlMessage(BigDecimal.TEN, TableControlMessageType.RESET);

        final TableControlMessage tableControlMessage = new TableControlMessage(BigDecimal.TEN, TableControlMessageType.RESET);
        assertThat(tableControlMessage, is(not(nullValue())));
    }

    @Test(expected = NullPointerException.class)
    public void controlMessagesWithANullTableIdThrowsAnIllegalArgumentException() {
        underTest.sendControlMessage(null, TableControlMessageType.RESET);
    }

    @Test(expected = NullPointerException.class)
    public void controlMessagesWithANullTypeThrowsAnIllegalArgumentException() {
        underTest.sendControlMessage(BigDecimal.ONE, null);
    }

    @Test(expected = NullPointerException.class)
    public void forcingANewGameRejectsANullTableId() {
        final BigDecimal playerId = valueOf(12);
        final com.yazino.game.api.PlayerAtTableInformation playerInfo = new com.yazino.game.api.PlayerAtTableInformation(
                new com.yazino.game.api.GamePlayer(playerId, null, "aPlayer"), singletonMap("aProperty", "aValue"));

        underTest.forceNewGame(null, asList(playerInfo), valueOf(113), "aClientId", singletonMap(playerId, valueOf(104)));
    }

    @Test
    public void forcingANewGameWritesARequestToTheSpace() {
        final BigDecimal playerId = valueOf(12);
        final com.yazino.game.api.PlayerAtTableInformation playerInfo = new com.yazino.game.api.PlayerAtTableInformation(
                new com.yazino.game.api.GamePlayer(playerId, null, "aPlayer"), singletonMap("aProperty", "aValue"));

        underTest.forceNewGame(valueOf(18), asList(playerInfo), valueOf(113), "aClientId", singletonMap(playerId, valueOf(104)));

        final TableRequestWrapper[] requests = tableSpace.readMultiple(new TableRequestWrapper(), Integer.MAX_VALUE);

        assertThat(asList(requests), hasItem(new TableRequestWrapper(new ForceNewGameRequest(
                valueOf(18), asList(playerInfo), valueOf(113), "aClientId", singletonMap(playerId, valueOf(104))))));
    }

    private Table tableWith(final long id,
                            final TableType tableType,
                            final TableSearchOption... options) {
        final Table table = new Table();
        table.setTableId(valueOf(id));
        table.setGameType(GAME_TYPE);

        switch (tableType) {
            case PUBLIC:
                table.setShowInLobby(true);
                break;
            case TOURNAMENT:
                table.setShowInLobby(false);
                table.setHasOwner(false);
                break;
            case PRIVATE:
                table.setHasOwner(true);
                break;
        }

        if (options != null) {
            for (TableSearchOption option : options) {
                switch (option) {
                    case IN_ERROR_STATE:
                        table.setTableStatus(TableStatus.error);
                        break;
                    case ONLY_OPEN:
                        table.setOpen(true);
                        break;
                    case ONLY_WITH_PLAYERS:
                        table.setHasPlayers(true);
                        break;
                }
            }
        }

        return table;
    }

    @Test(expected = NullPointerException.class)
    public void sendCommandShouldRejectANullCommand() {
        underTest.sendRequest(null);
    }

    @Test
    public void sendRequestShouldWriteTheWrappedRequestToTheSpace() {
        final CommandWrapper command = new CommandWrapper(valueOf(100), 200L, valueOf(300), null, "aCommand");

        underTest.sendRequest(command);

        final TableRequestWrapper requestWrapper = tableSpace.read(new TableRequestWrapper(), 5000L);
        assertThat(requestWrapper, is(not(nullValue())));
        assertThat(requestWrapper.getTableRequest(), is(equalTo((TableRequest) command)));
    }

    @Test
    public void sendRequestShouldReturnALeaseIDForTheRequest() {
        final CommandWrapper command = new CommandWrapper(valueOf(100), 200L, valueOf(300), null, "aCommand");

        final String id = underTest.sendRequest(command);

        assertThat(id, is(not(nullValue())));
    }

    @Test
    public void countingOutstandingRequestsShouldQueryGigaSpaces() {
        tableSpace.write(new TableRequestWrapper(new CommandWrapper(valueOf(100), 200L, valueOf(300), null, "aCommand")));
        tableSpace.write(new TableRequestWrapper(new CommandWrapper(valueOf(1200), 200L, valueOf(300), null, "aCommand")));

        final int numberOfUnprocessedRequests = underTest.countOutstandingRequests();

        assertThat(numberOfUnprocessedRequests, is(equalTo(2)));
    }

    @Test(expected = NullPointerException.class)
    public void countingOutstandingRequestsForATableShouldRejectANullTableId() {
        underTest.countOutstandingRequests(null);
    }

    @Test
    public void countingOutstandingRequestsForATableShouldQueryGigaSpaces() {
        tableSpace.write(new TableRequestWrapper(new CommandWrapper(valueOf(100), 200L, valueOf(300), null, "aCommand")));
        tableSpace.write(new TableRequestWrapper(new CommandWrapper(valueOf(1200), 200L, valueOf(300), null, "aCommand")));
        tableSpace.write(new TableRequestWrapper(new CommandWrapper(valueOf(1200), 300L, valueOf(300), null, "aCommand")));

        final int numberOfUnprocessedRequests = underTest.countOutstandingRequests(valueOf(1200));

        assertThat(numberOfUnprocessedRequests, is(equalTo(2)));
    }

    @Test(expected = NullPointerException.class)
    public void visitAllTablesRejectsANullVisitor() {
        underTest.visitAllLocalTables(null);
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void visitAllTablesInvokesTheVisitorOnAllTheTablesInTheSpace() {
        tableSpace.write(tableWith(1000, PUBLIC));
        tableSpace.write(tableWith(1001, PRIVATE));
        tableSpace.write(tableWith(1002, TOURNAMENT));
        tableSpace.write(tableWith(1003, PUBLIC));

        final Visitor<Table> tableVisitor = mock(Visitor.class);

        underTest.visitAllLocalTables(tableVisitor);

        verify(tableVisitor).visit(tableWith(1000, PUBLIC));
        verify(tableVisitor).visit(tableWith(1001, PRIVATE));
        verify(tableVisitor).visit(tableWith(1002, TOURNAMENT));
        verify(tableVisitor).visit(tableWith(1003, PUBLIC));
    }


    @Test
    @Transactional
    public void getOpenTableCountReturnsZeroOnNoMatches() {
        assertEquals(0, underTest.countOpenTables(newHashSet(bd(0), bd(1), bd(2))));
    }

    @Test
    @Transactional
    public void getOpenTableCountShouldQueryOpenTables() {
        tableSpace.write(aTableWithState(3, TableStatus.open));
        tableSpace.write(aTableWithState(4, TableStatus.open));
        tableSpace.write(aTableWithState(5, TableStatus.open));
        tableSpace.write(aTableWithState(0, TableStatus.closing));

        // mismatches
        tableSpace.write(aTableWithState(0, TableStatus.closed));
        tableSpace.write(aTableWithState(7, null));
        tableSpace.write(aTableWithState(3, TableStatus.open)); // outside queried ID range

        final int count = underTest.countOpenTables(newHashSet(seqBigDecimal(0, 6)));

        assertThat(count, is(equalTo(4)));
    }

    @Test
    @Transactional
    public void getActivePlayersShouldQueryPlayerNumbersSizeToReadSmallerThanBatchSize() {
        tableSpace.write(aTableWithState(3, TableStatus.open));
        tableSpace.write(aTableWithState(4, TableStatus.open));
        tableSpace.write(aTableWithState(5, TableStatus.open));
        tableSpace.write(aTableWithState(0, TableStatus.closed));
        tableSpace.write(aTableWithState(7, null));
        tableSpace.write(aTableWithState(0, TableStatus.closing));
        tableSpace.write(aTableWithState(3, TableStatus.open)); // outside queried ID range

        final Set<com.yazino.game.api.PlayerAtTableInformation> activePlayers = underTest.findLocalActivePlayers(newHashSet(seqBigDecimal(0, 6)));

        assertThat(activePlayers.size(), is(equalTo(19)));
    }

    @Test
    @Transactional
    public void getActivePlayersShouldQueryPlayerNumbersSizeToReadLargerThanBatchSize() {
        final int batchSize = 1000;
        final int sizeToRead = 2 * batchSize + 521;
        final int playersPerTable = 3;

        for (int i = 0; i < sizeToRead; i++) {
            tableSpace.write(aTableWithState(playersPerTable, TableStatus.open));
        }

        final Set<com.yazino.game.api.PlayerAtTableInformation> activePlayers = underTest.findLocalActivePlayers(newHashSet(seqBigDecimal(0, sizeToRead)));

        assertThat(activePlayers.size(), is(equalTo(sizeToRead * playersPerTable)));
    }

    public static BigDecimal[] seqBigDecimal(final int min, final int max) {
        final BigDecimal[] seq = new BigDecimal[max - min];

        for (int i = min; i < max; ++i) {
            seq[i - min] = new BigDecimal(i);
        }

        return seq;
    }

    private Table aTableWithState(final int players,
                                  final TableStatus status) {
        final long tableId = tableIdSource++;
        final Table table = new Table();
        table.setTableId(bd(tableId));
        table.setTableStatus(status);
        table.setGameType(GAME_TYPE);

        final PrintlnStatus gameStatus = new PrintlnStatus();
        if (players > 0) {
            final Set<BigDecimal> generatedIds = newHashSet(seqBigDecimal((int) tableId * 1000, (int) tableId * 1000 + players));
            for (BigDecimal generatedId : generatedIds) {
                gameStatus.addPlayer(new com.yazino.game.api.GamePlayer(generatedId, null, "Player " + generatedId));
            }
        }

        table.setCurrentGame(new GameStatus(gameStatus));
        return table;
    }

    private Table createPrivateTable(BigDecimal ownerId, String gameType, BigDecimal tableId, TableStatus tableStatus) {
        Table table = new Table();
        table.setGameTypeId(gameType);
        table.setOwnerId(ownerId);
        table.setTableId(tableId);
        table.setTableStatus(tableStatus);
        tableSpace.write(table);
        return table;
    }

    private Table createTableWithPlayers(final int id, final TableStatus status, final boolean full, final boolean tablePublic, boolean hasPlayers, boolean availableForJoining) {
        final Table table = new Table();
        table.setTableId(valueOf(id));
        table.setHasPlayers(hasPlayers);
        table.setFull(full);
        table.setClientId(CLIENT_ID);
        table.setTemplateName(GAME_VARIATION_TEMPLATE_NAME);
        table.setGameType(GAME_TYPE);
        table.setTableStatus(status);
        table.setShowInLobby(tablePublic);
        table.setAvailableForPlayersJoining(availableForJoining);
        tableSpace.write(table);
        return table;
    }

    public static BigDecimal bd(final long value) {
        return BigDecimal.valueOf(value);
    }

    public <T, V> ArrayList<V> transform(final Collection<T> data, final Function<T, V> transformer) {
        notNull(transformer, "transformer may not be null");
        final ArrayList<V> transformedList = new ArrayList<>();
        if (data != null) {
            for (T item : data) {
                transformedList.add(transformer.apply(item));
            }
        }
        return transformedList;
    }

    private class TableSummaryTransformer implements Function<Table, TableSummary> {
        @Override
        public TableSummary apply(final Table table) {
            if (table == null) {
                return null;
            }
            return table.summarise(gameRules);
        }
    }
}
