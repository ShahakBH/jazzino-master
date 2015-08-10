package com.yazino.host.table;

import com.yazino.host.TableRequestWrapperQueue;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.table.*;
import org.openspaces.remoting.Routing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.yazino.game.api.GameStatus;
import com.yazino.game.api.PlayerAtTableInformation;
import com.yazino.platform.model.table.TableRequestWrapper;
import com.yazino.platform.model.table.CommandWrapper;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

@Component
public class StandaloneTableService implements TableService {

    private final TableRequestWrapperQueue tableRequestWrapperQueue;

    @Autowired
    public StandaloneTableService(final TableRequestWrapperQueue tableRequestWrapperQueue) {
        this.tableRequestWrapperQueue = tableRequestWrapperQueue;
    }

    @Override
    public TableSummary createPublicTable(final String gameType,
                                          final String templateName,
                                          final String clientId,
                                          final String tableName, final Set<String> tags) throws TableException {
        return null;
    }

    @Override
    public TableSummary createPublicTable(final String gameType,
                                          final String tableName, final Set<String> tags)
            throws TableException {
        return null;
    }

    @Override
    public TableSummary createPrivateTableForPlayer(final String gameType,
                                                    final String templateName,
                                                    final String clientId,
                                                    final String tableName,
                                                    final BigDecimal playerId) throws TableException {
        return null;
    }

    @Override
    public TableGameSummary findGameSummaryById(@Routing final BigDecimal tableId) {
        return null;
    }

    @Override
    public TableSummary findSummaryById(@Routing final BigDecimal tableId) {
        return null;
    }

    @Override
    public TableSummary findTableByGameTypeAndPlayerId(final String gameType, final BigDecimal playerId) {
        return null;
    }

    @Override
    public PagedData<TableSummary> findByType(final TableType tableType,
                                              final int page,
                                              final TableSearchOption... options) {
        return null;
    }

    @Override
    public int countByType(final TableType tableType, final TableSearchOption... options) {
        return 0;
    }

    @Override
    public Map<String, TableSummary> findAllTablesOwnedByPlayer(final BigDecimal playerId) {
        return null;
    }

    @Override
    public void closeTable(@Routing final BigDecimal tableId) {
    }

    @Override
    public void asyncCloseTable(@Routing final BigDecimal tableId) {
    }

    @Override
    public void loadAll() {
    }

    @Override
    public void asyncLoadAll() {
    }

    @Override
    public void unload(@Routing final BigDecimal tableId) {
    }

    @Override
    public void asyncUnload(@Routing final BigDecimal tableId) {
    }

    @Override
    public void shutdown(@Routing final BigDecimal tableId) {
    }

    @Override
    public void asyncShutdown(@Routing final BigDecimal tableId) {
    }

    @Override
    public void shutdownGame(final String gameTypeId) {
    }

    @Override
    public void asyncShutdownGame(final String gameTypeId) {
    }

    @Override
    public void reOpen(@Routing final BigDecimal tableId) {
    }

    @Override
    public void asyncReOpen(@Routing final BigDecimal tableId) {
    }

    @Override
    public void reset(@Routing final BigDecimal tableId) {
    }

    @Override
    public void asyncReset(@Routing final BigDecimal tableId) {
    }

    @Override
    public void testReplaceGame(@Routing final BigDecimal tableId, final GameStatus gameStatus) {
    }

    @Override
    public int countTablesWithPlayers(final String gameType) {
        return 0;
    }

    @Override
    public void forceNewGame(@Routing final BigDecimal tableId,
                             final Collection<PlayerAtTableInformation> playersAtTable,
                             final BigDecimal variationTemplateId,
                             final String clientId,
                             final Map<BigDecimal, BigDecimal> accountIds) {
    }

    @Override
    public void sendCommand(@Routing("getTableId") final Command command) {
        tableRequestWrapperQueue.addRequest(new TableRequestWrapper(new CommandWrapper(command)));
    }

    @Override
    public void asyncSendCommand(@Routing("getTableId") final Command command) {
        tableRequestWrapperQueue.addRequest(new TableRequestWrapper(new CommandWrapper(command)));
    }

    @Override
    public int countOutstandingRequests() {
        return 0;
    }

    @Override
    public int countOutstandingRequests(final BigDecimal tableId) {
        return 0;
    }

    @Override
    public void makeReservationAtTable(@Routing final BigDecimal tableId, final BigDecimal playerId) {
    }

    @Override
    public Set<GameTypeInformation> getGameTypes() {
        return null;
    }

    @Override
    public GameClient findClientById(final String clientId) {
        return null;
    }

    @Override
    public Set<GameClient> findAllClientsFor(final String gameTypeId) {
        return null;
    }

    @Override
    public Collection<GameConfiguration> getGameConfigurations() {
        return null;
    }
}
