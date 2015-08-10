package senet.server.table;


import com.yazino.platform.model.PagedData;
import com.yazino.platform.model.table.Client;
import com.yazino.platform.table.*;
import org.openspaces.remoting.Routing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import com.yazino.game.api.GameStatus;
import com.yazino.game.api.PlayerAtTableInformation;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.model.table.CommandWrapper;
import com.yazino.platform.repository.table.ClientRepository;
import com.yazino.platform.repository.table.GameRepository;

import java.math.BigDecimal;
import java.util.*;

public class FitTableService implements TableService {
    @Autowired(required = true)
    @Qualifier("tableRepository")
    private FitTableRepository tableRepository;

    @Autowired(required = true)
    private GameRepository gameRepository;

    @Autowired(required = true)
    private ClientRepository clientRepository;


    @Override
    public TableSummary createPublicTable(final String gameType,
                                          final String templateName,
                                          final String clientId,
                                          final String tableName,
                                          final Set<String> tags) {
        final Table table = new Table();
        table.setClientId(TableLaunching.CLIENT_ID);
        table.setTableName(tableName);
        table.setTemplateName(templateName);
        table.setGameTypeId(gameType);

        tableRepository.save(table);

        return findSummaryById(table.getTableId());
    }

    @Override
    public TableSummary createPublicTable(final String gameType,
                                          final String tableName,
                                          final Set<String> tags) throws TableException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public TableSummary createPrivateTableForPlayer(String gameType,
                                                    String templateName,
                                                    String clientId,
                                                    String tableName,
                                                    BigDecimal playerId)
            throws TableException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public TableGameSummary findGameSummaryById(final BigDecimal tableId) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public TableSummary findSummaryById(@Routing final BigDecimal tableId) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public TableSummary findTableByGameTypeAndPlayerId(String gameType, BigDecimal playerId) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public PagedData<TableSummary> findByType(final TableType tableType,
                                              final int page,
                                              final TableSearchOption... options) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public int countByType(final TableType tableType,
                           final TableSearchOption... options) {
        throw new UnsupportedOperationException("Unimplemented");
    }


    @Override
    public Map<String, TableSummary> findAllTablesOwnedByPlayer(BigDecimal arg0) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void closeTable(BigDecimal tableId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void asyncCloseTable(@Routing final BigDecimal tableId) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public void loadAll() {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public void asyncLoadAll() {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public void unload(@Routing final BigDecimal tableId) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public void asyncUnload(@Routing final BigDecimal tableId) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public void shutdown(@Routing final BigDecimal tableId) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public void asyncShutdown(@Routing final BigDecimal tableId) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public void shutdownGame(String gameTypeId) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public void asyncShutdownGame(final String gameTypeId) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public void reOpen(@Routing final BigDecimal tableId) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public void asyncReOpen(@Routing final BigDecimal tableId) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public void reset(@Routing final BigDecimal tableId) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public void asyncReset(@Routing final BigDecimal tableId) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public int countTablesWithPlayers(final String gameType) {
        return tableRepository.countTablesWithPlayers(gameType);
    }

    @Override
    public void forceNewGame(@Routing final BigDecimal tableId,
                             final Collection<PlayerAtTableInformation> playersAtTable,
                             final BigDecimal variationTemplateId,
                             final String clientId,
                             final Map<BigDecimal, BigDecimal> accountIds) {
        tableRepository.forceNewGame(tableId, playersAtTable, variationTemplateId, clientId, accountIds);
    }

    @Override
    public void sendCommand(@Routing("getTableId") final Command command) {
        tableRepository.sendRequest(new CommandWrapper(command));
    }

    @Override
    public void asyncSendCommand(@Routing("getTableId") final Command command) {
        tableRepository.sendRequest(new CommandWrapper(command));
    }

    @Override
    public int countOutstandingRequests() {
        return tableRepository.countOutstandingRequests();
    }

    @Override
    public int countOutstandingRequests(final BigDecimal tableId) {
        return tableRepository.countOutstandingRequests(tableId);
    }


    @Override
    public Set<GameTypeInformation> getGameTypes() {
        final Set<GameTypeInformation> availableGameTypes = gameRepository.getAvailableGameTypes();
        if (availableGameTypes != null) {
            return availableGameTypes;
        }
        return Collections.emptySet();
    }

    @Override
    public void testReplaceGame(@Routing final BigDecimal tableId,
                                final GameStatus gameStatus) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public GameClient findClientById(final String clientId) {
        final Client client = clientRepository.findById(clientId);
        if (client != null) {
            return new GameClient(client.getClientId(), client.getNumberOfSeats(),
                    client.getClientFile(), client.getGameType(), client.getClientProperties());
        }
        return null;
    }

    @Override

    public Set<GameClient> findAllClientsFor(final String gameTypeId) {
        final Client[] clients = clientRepository.findAll(gameTypeId);
        if (clients != null) {
            final HashSet<GameClient> gameClients = new HashSet<GameClient>();
            for (Client client : clients) {
                gameClients.add(new GameClient(client.getClientId(), client.getNumberOfSeats(),
                        client.getClientFile(), client.getGameType(), client.getClientProperties()));
            }
            return gameClients;
        }
        return Collections.emptySet();
    }

    public void makeReservationAtTable(@Routing BigDecimal tableId, BigDecimal playerId) {
        tableRepository.makeReservationAtTable(tableId, playerId);
    }

    @Override
    public Collection<GameConfiguration> getGameConfigurations() {
        throw new UnsupportedOperationException("Not implemented");
    }
}
