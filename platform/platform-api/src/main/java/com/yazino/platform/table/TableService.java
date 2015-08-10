package com.yazino.platform.table;

import com.yazino.game.api.GameStatus;
import com.yazino.game.api.PlayerAtTableInformation;
import com.yazino.platform.model.PagedData;
import org.openspaces.remoting.Routing;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface TableService {

    TableSummary createPublicTable(String gameType,
                                   String templateName,
                                   String clientId,
                                   String tableName, final Set<String> tags)
            throws TableException;

    TableSummary createPublicTable(String gameType,
                                   String tableName, final Set<String> tags)
            throws TableException;

    TableSummary createPrivateTableForPlayer(String gameType,
                                             String templateName,
                                             String clientId,
                                             String tableName,
                                             BigDecimal playerId)
            throws TableException;

    TableGameSummary findGameSummaryById(@Routing BigDecimal tableId);

    TableSummary findSummaryById(@Routing BigDecimal tableId);

    TableSummary findTableByGameTypeAndPlayerId(String gameType,
                                                BigDecimal playerId);

    PagedData<TableSummary> findByType(TableType tableType,
                                       int page,
                                       TableSearchOption... options);

    int countByType(TableType tableType,
                    TableSearchOption... options);

    /**
     * Gets all private tables owned by given player keyed on game type.
     *
     * @param playerId the ID of the owning player.
     * @return map of tables owned by player. An empty map is returned if player
     * does not own any tables.
     */
    Map<String, TableSummary> findAllTablesOwnedByPlayer(BigDecimal playerId);

    void closeTable(@Routing BigDecimal tableId);

    void asyncCloseTable(@Routing BigDecimal tableId);

    void loadAll();

    void asyncLoadAll();

    void unload(@Routing BigDecimal tableId);

    void asyncUnload(@Routing BigDecimal tableId);

    void shutdown(@Routing BigDecimal tableId);

    void asyncShutdown(@Routing BigDecimal tableId);

    void shutdownGame(final String gameTypeId);

    void asyncShutdownGame(final String gameTypeId);

    void reOpen(@Routing BigDecimal tableId);

    void asyncReOpen(@Routing BigDecimal tableId);

    void reset(@Routing BigDecimal tableId);

    void asyncReset(@Routing BigDecimal tableId);

    /**
     * This method is for test purposes only and has a good probability of wiping out
     * game changes. Use with extreme care.
     *
     * @param tableId    the table ID.
     * @param gameStatus the new game status.
     */
    void testReplaceGame(@Routing BigDecimal tableId,
                         final GameStatus gameStatus);

    int countTablesWithPlayers(String gameType);

    void forceNewGame(@Routing BigDecimal tableId,
                      Collection<PlayerAtTableInformation> playersAtTable,
                      BigDecimal variationTemplateId,
                      String clientId,
                      Map<BigDecimal, BigDecimal> accountIds);

    void sendCommand(@Routing("getTableId") Command command);

    void asyncSendCommand(@Routing("getTableId") Command command);

    int countOutstandingRequests();

    int countOutstandingRequests(BigDecimal tableId);

    void makeReservationAtTable(@Routing BigDecimal tableId, BigDecimal playerId);

    Set<GameTypeInformation> getGameTypes();

    GameClient findClientById(String clientId);

    Set<GameClient> findAllClientsFor(String gameTypeId);

    Collection<GameConfiguration> getGameConfigurations();
}
