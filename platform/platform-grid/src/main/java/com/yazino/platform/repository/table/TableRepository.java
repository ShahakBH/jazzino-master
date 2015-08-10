package com.yazino.platform.repository.table;

import com.yazino.platform.model.PagedData;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.model.table.TableControlMessageType;
import com.yazino.platform.model.table.TableRequest;
import com.yazino.platform.table.TableSearchOption;
import com.yazino.platform.table.TableSummary;
import com.yazino.platform.table.TableType;
import com.yazino.platform.util.Visitor;
import com.yazino.game.api.PlayerAtTableInformation;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface TableRepository {

    Table findById(BigDecimal tableId);

    Table findById(BigDecimal tableId, int waitTimeout);

    void save(Table table);

    void nonPersistentSave(Table table);

    void sendControlMessage(BigDecimal tableId,
                            TableControlMessageType messageType);

    void forceNewGame(BigDecimal tableId,
                      Collection<PlayerAtTableInformation> playersAtTable,
                      BigDecimal variationTemplateId,
                      String clientId,
                      Map<BigDecimal, BigDecimal> accountIds);

    /**
     * Remove the Table And TableInfo objects identified by the table id from the Space.
     *
     * @param tableId the id of the table to be removed
     */
    void unload(BigDecimal tableId);

    int countTablesWithPlayers(String gameType);

    TableSummary findTableByGameTypeAndPlayer(String gameType, BigDecimal ownerId);

    Set<BigDecimal> findAllTablesForGameType(String gameType);

    Set<BigDecimal> findAllLocalPlayersForGameType(String gameType);

    /**
     * Gets all private tables owned by given player.
     *
     * @param playerId player id
     * @return set of tables owned by player. An empty set is returned if player
     *         does not own any tables.
     */
    Set<TableSummary> findAllTablesOwnedByPlayer(BigDecimal playerId);

    void visitAllLocalTables(Visitor<Table> visitor);

    Set<BigDecimal> findAllLocalPlayers();

    /**
     * Returns all the tables with players on.
     *
     * @return a set of table ids, never null.
     */
    Set<BigDecimal> findAllLocalTablesWithPlayers();

    void makeReservationAtTable(BigDecimal tableId, BigDecimal playerId);

    void removeReservationForTable(BigDecimal tableId, BigDecimal playerId);

    /**
     * Returns a paged subset of tables that match the given criteria.
     *
     * @param tableType all, public, private or tournament
     * @param page      used to determine the start index and number of results returned
     * @param options   further criteria to specify if the tables have players or are in error state
     * @return a page of table summaries
     * @throws IllegalStateException when there is a problem executing a remote task. See ReadMultipleWithTemplateTask
     */
    PagedData<TableSummary> findByType(TableType tableType,
                                       final int page,
                                       TableSearchOption... options);

    int countByType(TableType tableType,
                    TableSearchOption... options);

    String sendRequest(TableRequest request);

    int countOutstandingRequests();

    int countOutstandingRequests(BigDecimal tableId);

    int countOpenTables(Collection<BigDecimal> tableIds);

    Set<PlayerAtTableInformation> findLocalActivePlayers(Collection<BigDecimal> tableIds);
}
