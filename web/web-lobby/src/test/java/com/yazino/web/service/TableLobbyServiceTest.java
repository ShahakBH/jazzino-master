package com.yazino.web.service;

import com.yazino.platform.Partner;
import com.yazino.platform.table.*;
import com.yazino.web.domain.TableReservationConfiguration;
import com.yazino.web.domain.social.PlayerInformation;
import com.yazino.web.domain.social.PlayerInformationType;
import com.yazino.web.domain.social.PlayersInformationService;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import com.yazino.game.api.GameType;

import java.math.BigDecimal;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class TableLobbyServiceTest {
    private static final Set<String> EMPTY_SET = Collections.<String>emptySet();
    private static final GameType GAME_TYPE = new GameType("BLACKJACK", "Blackjack", EMPTY_SET);
    public static final String GAME_TYPE_ID = "aGameTypeId";
    private static final String GAME_VARIATION_TEMPLATE_NAME = "jim-the-template";
    private static final Partner PARTNER_ID = Partner.YAZINO;
    private static final String CLIENT_ID = "testClient";
    private static final String TABLE_NAME = "test private table name";
    private static final String CLIENT_FILE = "clientFile";
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(177345);
    private static final String SLOTS = "SLOTS";
    private static final String SLOTS_LOW = "Slots Low";
    private static final String SLOTS_MED = "Slots Med";

    private long tableIdSource = 1;

    private final TableService tableService = mock(TableService.class);
    private final TableSearchService tableSearchService = mock(TableSearchService.class);
    private final TableReservationConfiguration tableReservationConfiguration = mock(TableReservationConfiguration.class);
    private PlayersInformationService playersInformationService = mock(PlayersInformationService.class);

    private TableLobbyService underTest;

    private final GameClient client = new GameClient("aClient", 7, CLIENT_FILE, null, null);

    @Before
    public void setUp() {
        when(tableService.findClientById(CLIENT_ID)).thenReturn(client);

        underTest = new TableLobbyService(tableSearchService, tableService, tableReservationConfiguration, playersInformationService, 10, "Slots Low");
    }

    @Test(expected = IllegalArgumentException.class)
    public void findOrCreateRejectsInvalidClientId() throws TableException {
        TableSearchCriteria criteria = new TableSearchCriteria(GAME_TYPE.getId(),
                GAME_VARIATION_TEMPLATE_NAME,
                "bob",
                EMPTY_SET);

        when(tableSearchService.findTables(any(BigDecimal.class), eq(criteria))).thenThrow(new IllegalArgumentException());
        underTest.findOrCreateTableByGameTypeAndVariation(GAME_TYPE.getId(),
                GAME_VARIATION_TEMPLATE_NAME,
                "bob",
                PLAYER_ID,
                EMPTY_SET);
    }

    @Test
    public void findOrCreateSimilarTableInterrogatesTableForDetailsAndPassesToFindOrCreate() throws TableException {
        final TableSummary originalTable = createTable();
        final BigDecimal similarTableId = BigDecimal.valueOf(45);
        final TableSearchResult similarResult = createSearchResult(similarTableId, 1, 0);

        when(tableService.findSummaryById(originalTable.getId())).thenReturn(originalTable);
        when(tableService.findSummaryById(similarTableId)).thenReturn(tableWithId(similarTableId));
        when(tableSearchService.findTables(PLAYER_ID, new TableSearchCriteria(
                GAME_TYPE.getId(),
                GAME_VARIATION_TEMPLATE_NAME,
                CLIENT_ID,
                EMPTY_SET,
                originalTable.getId())))
                .thenReturn(newHashSet(similarResult));

        final TableSummary result = underTest.findOrCreateSimilarTable(originalTable.getId(), PLAYER_ID);

        assertEquals(similarTableId, result.getId());
    }

    @Test
    public void shouldRetrieveGameByTags() throws TableException {
        BigDecimal bestMatchTableId = BigDecimal.valueOf(1);
        TableSearchCriteria expectedCriteria = new TableSearchCriteria(
                GAME_TYPE.getId(), GAME_VARIATION_TEMPLATE_NAME, CLIENT_ID, newHashSet("aTag", "anotherTag"));
        TableSearchResult bestMatch = new TableSearchResult(bestMatchTableId, 1, 10, 0);
        when(tableSearchService.findTables(any(BigDecimal.class), eq(expectedCriteria))).thenReturn(Arrays.asList(bestMatch));
        when(tableService.findSummaryById(bestMatchTableId)).thenReturn(tableWithId(bestMatchTableId));

        final TableSummary result = underTest.findOrCreateTableByGameTypeAndVariation(
                GAME_TYPE.getId(), GAME_VARIATION_TEMPLATE_NAME, CLIENT_ID, PLAYER_ID, newHashSet("aTag", "anotherTag"));

        assertEquals(bestMatchTableId, result.getId());
        verify(tableService, times(0)).createPublicTable(anyString(), anyString(), anyString(), anyString(), anySet());
        verify(tableService, times(0)).makeReservationAtTable(bestMatchTableId, PLAYER_ID);
    }

    @Test
    public void shouldRetrieveGameByGameTypeAndVariation() throws TableException {
        BigDecimal bestMatchTableId = BigDecimal.valueOf(1);
        searchForTableReturns(bestMatchTableId);
        when(tableService.findSummaryById(bestMatchTableId)).thenReturn(tableWithId(bestMatchTableId));

        final TableSummary result = underTest.findOrCreateTableByGameTypeAndVariation(
                GAME_TYPE.getId(), GAME_VARIATION_TEMPLATE_NAME, CLIENT_ID, PLAYER_ID, EMPTY_SET);

        assertEquals(bestMatchTableId, result.getId());
        verify(tableService, times(0)).createPublicTable(anyString(), anyString(), anyString(), anyString(), anySet());
        verify(tableService, times(0)).makeReservationAtTable(bestMatchTableId, PLAYER_ID);
    }

    @Test
    public void shouldRetrieveGameByGameTypeAndVariationAndReserve() throws TableException {
        BigDecimal bestMatchTableId = BigDecimal.valueOf(1);
        searchForTableReturns(bestMatchTableId);
        when(tableReservationConfiguration.supportsReservation(GAME_TYPE.getId())).thenReturn(true);
        when(tableService.findSummaryById(bestMatchTableId)).thenReturn(tableWithId(bestMatchTableId));

        final TableSummary result = underTest.findOrCreateTableByGameTypeAndVariation(
                GAME_TYPE.getId(), GAME_VARIATION_TEMPLATE_NAME, CLIENT_ID, PLAYER_ID, EMPTY_SET);

        assertEquals(bestMatchTableId, result.getId());
        verify(tableService, times(0)).createPublicTable(anyString(), anyString(), anyString(), anyString(), anySet());
        verify(tableService, times(1)).makeReservationAtTable(bestMatchTableId, PLAYER_ID);
    }

    @Test
    public void shouldCreateTableIfNotFoundByGameByGameTypeAndVariation() throws TableException {
        searchForTableReturnsNothing();
        final TableSummary table = createTable();
        when(tableService.createPublicTable(GAME_TYPE.getId(),
                GAME_VARIATION_TEMPLATE_NAME,
                CLIENT_ID,
                null,
                EMPTY_SET))
                .thenReturn(table);

        final TableSummary result = underTest.findOrCreateTableByGameTypeAndVariation(GAME_TYPE.getId(),
                GAME_VARIATION_TEMPLATE_NAME,
                CLIENT_ID,
                PLAYER_ID,
                EMPTY_SET);
        assertEquals(table.getId(), result.getId());
        verify(tableService, times(0)).makeReservationAtTable(table.getId(), PLAYER_ID);
    }

    @Test
    public void shouldCreateTableIfNotFoundByTags() throws TableException {
        final TableSummary table = createTable();
        when(tableService.createPublicTable(GAME_TYPE.getId(),
                GAME_VARIATION_TEMPLATE_NAME,
                CLIENT_ID,
                null,
                newHashSet("aTag", "anotherTag")))
                .thenReturn(table);

        final TableSummary result = underTest.findOrCreateTableByGameTypeAndVariation(GAME_TYPE.getId(),
                GAME_VARIATION_TEMPLATE_NAME, CLIENT_ID, PLAYER_ID, newHashSet("aTag", "anotherTag"));
        assertEquals(table.getId(), result.getId());
        verify(tableService, times(0)).makeReservationAtTable(table.getId(), PLAYER_ID);
    }

    @Test
    public void shouldCreateTableIfNotFoundByGameByGameTypeAndVariationAndReserve() throws TableException {
        searchForTableReturnsNothing();
        when(tableReservationConfiguration.supportsReservation(GAME_TYPE.getId())).thenReturn(true);
        final TableSummary table = createTable();
        when(tableService.createPublicTable(GAME_TYPE.getId(),
                GAME_VARIATION_TEMPLATE_NAME,
                CLIENT_ID,
                null,
                EMPTY_SET)).thenReturn(table);
        final TableSummary result = underTest.findOrCreateTableByGameTypeAndVariation(GAME_TYPE.getId(),
                GAME_VARIATION_TEMPLATE_NAME,
                CLIENT_ID,
                PLAYER_ID,
                EMPTY_SET);
        assertEquals(table.getId(), result.getId());
        verify(tableService, times(1)).makeReservationAtTable(table.getId(), PLAYER_ID);
    }

    @Test
    public void ShouldCreateTableWithLowLevelTagIfLowLevelPlayerAndLowStake() throws TableException {
        setupPlayerWithLevel(5);
        searchForTableReturnsNothing();
        shouldCreateTableWithTagAndReturnSummary("lowLevel");

        underTest.findOrCreateTableByGameTypeAndVariation(SLOTS, SLOTS_LOW, CLIENT_ID, PLAYER_ID, EMPTY_SET);

        verifyThatItSearchedForTableWithTag("lowLevel");
    }

    @Test
    public void shouldCreateTableWithHiLevelTagIfHighLevelPlayerAndLowStake() throws TableException {
        setupPlayerWithLevel(12);
        searchForTableReturnsNothing();
        shouldCreateTableWithTagAndReturnSummary("highLevel");

        underTest.findOrCreateTableByGameTypeAndVariation(SLOTS, SLOTS_LOW, CLIENT_ID, PLAYER_ID, EMPTY_SET);

        verifyThatItSearchedForTableWithTag("highLevel");
    }

    @Test
    public void shouldCreateTableWithNoTagsIfNotSlotsLow() throws TableException {
        setupPlayerWithLevel(5);
        searchForTableReturnsNothing();
        final TableSummary tableSummary = tableWithId(BigDecimal.ONE);
        when(tableService.createPublicTable(SLOTS, SLOTS_MED, CLIENT_ID, null, EMPTY_SET)).thenReturn(tableSummary);

        underTest.findOrCreateTableByGameTypeAndVariation(SLOTS, SLOTS_MED, CLIENT_ID, PLAYER_ID, EMPTY_SET);

    }

    @Test
    public void ShouldJoinTableWithLowLevelTagIfLowLevelPlayerAndLowStake() throws TableException {
        setupPlayerWithLevel(5);
        searchForTableReturnsTableWithIdAndTag(BigDecimal.TEN, "lowLevel");

        underTest.findOrCreateTableByGameTypeAndVariation(SLOTS, SLOTS_LOW, CLIENT_ID, PLAYER_ID, EMPTY_SET);

        verifyThatItSearchedForTableWithTag("lowLevel");
    }

    private void searchForTableReturnsTableWithIdAndTag(final BigDecimal tableId, final String tag) {
        TableSearchCriteria criteria = new TableSearchCriteria(SLOTS,
                SLOTS_LOW,
                CLIENT_ID,
                newHashSet(tag));
        TableSearchResult bestMatch = new TableSearchResult(tableId, 1, 10, 0);
        when(tableSearchService.findTables(any(BigDecimal.class), eq(criteria))).thenReturn(Arrays.asList(bestMatch));
    }

    private void verifyThatItSearchedForTableWithTag(String tag) {
        final ArgumentCaptor<TableSearchCriteria> argumentCaptor = ArgumentCaptor.forClass(TableSearchCriteria.class);
        verify(tableSearchService).findTables(eq(PLAYER_ID), argumentCaptor.capture());
        final TableSearchCriteria value = argumentCaptor.getValue();
        assertThat(value.getTags(), CoreMatchers.hasItem(tag));
    }

    private void shouldCreateTableWithTagAndReturnSummary(String tag) throws TableException {
        final HashSet<String> noobSet = newHashSet(tag);
        final TableSummary tableSummary = tableWithIdAndTags(BigDecimal.ONE, noobSet);
        when(tableService.createPublicTable(SLOTS, SLOTS_LOW, CLIENT_ID, null, noobSet)).thenReturn(tableSummary);
    }

    private void setupPlayerWithLevel(int level) {
        final List<PlayerInformation> playerInfo = new ArrayList<PlayerInformation>();
        playerInfo.add(new PlayerInformation.Builder(PLAYER_ID).withField(PlayerInformationType.LEVEL, level).build());
        when(playersInformationService.retrieve(newArrayList(PLAYER_ID), SLOTS, PlayerInformationType.LEVEL)).thenReturn(playerInfo);
    }


    private void searchForTableReturnsNothing() {
        TableSearchCriteria criteria = new TableSearchCriteria(GAME_TYPE.getId(),
                GAME_VARIATION_TEMPLATE_NAME,
                CLIENT_ID,
                EMPTY_SET);
        when(tableSearchService.findTables(any(BigDecimal.class), eq(criteria))).thenReturn(Collections.<TableSearchResult>emptyList());
    }

    private void searchForTableReturns(BigDecimal tableId) {
        TableSearchCriteria criteria = new TableSearchCriteria(GAME_TYPE.getId(),
                GAME_VARIATION_TEMPLATE_NAME,
                CLIENT_ID,
                EMPTY_SET);
        TableSearchResult bestMatch = new TableSearchResult(tableId, 1, 10, 0);
        when(tableSearchService.findTables(any(BigDecimal.class), eq(criteria))).thenReturn(Arrays.asList(bestMatch));
    }

    @Test
    public void closeTable_shouldThrowTableException() {
        TableSummary table = createTableWithOwner(PLAYER_ID);

        when(tableService.findSummaryById(table.getId())).thenReturn(table);
    }

    private TableSearchResult createSearchResult(BigDecimal tableId, int maxSeats, int spareSeats) {
        TableSearchResult result = new TableSearchResult();
        result.setMaxSeats(maxSeats);
        result.setTableId(tableId);
        result.setSpareSeats(spareSeats);
        return result;
    }

    private TableSummary createTable() {
        return tableWithId(tableIdSource++);
    }

    private TableSummary tableWithId(final Number id) {
        return new TableSummary(BigDecimal.valueOf(id.longValue()),
                TABLE_NAME, TableStatus.open, GAME_TYPE_ID, GAME_TYPE,
                 null, CLIENT_ID, CLIENT_FILE, GAME_VARIATION_TEMPLATE_NAME, null, null, EMPTY_SET);
    }

    private TableSummary tableWithIdAndTags(final Number id, final Set tags) {
        return new TableSummary(BigDecimal.valueOf(id.longValue()),
                TABLE_NAME, TableStatus.open, GAME_TYPE_ID, GAME_TYPE,
                null, CLIENT_ID, CLIENT_FILE, GAME_VARIATION_TEMPLATE_NAME, null, null, tags);
    }

    private TableSummary createTableWithOwner(final BigDecimal owner) {
        return new TableSummary(BigDecimal.valueOf(tableIdSource++),
                TABLE_NAME, TableStatus.open, GAME_TYPE_ID, GAME_TYPE,
                owner, CLIENT_ID, CLIENT_FILE, GAME_VARIATION_TEMPLATE_NAME, null, null, EMPTY_SET);
    }

}
