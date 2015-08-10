package com.yazino.platform.service.table;

import com.google.common.base.Function;
import com.yazino.game.api.GameRules;
import com.yazino.game.api.GameStatus;
import com.yazino.game.api.PlayerAtTableInformation;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.model.table.Client;
import com.yazino.platform.model.table.CommandWrapper;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.model.table.TestAlterGameRequest;
import com.yazino.platform.persistence.table.TableDAO;
import com.yazino.platform.repository.table.*;
import com.yazino.platform.table.*;
import com.yazino.platform.util.Visitor;
import org.openspaces.remoting.RemotingService;
import org.openspaces.remoting.Routing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.*;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Sets.newHashSet;
import static com.yazino.platform.model.table.TableControlMessageType.*;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.Validate.notNull;

@RemotingService
public class GigaspaceRemotingTableService implements TableService {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceRemotingTableService.class);

    private static final GameClientTransformer GAME_CLIENT_TRANSFORMER = new GameClientTransformer();

    private final TableDAO tableDao;
    private final ClientRepository clientRepository;
    private final TableRepository tableRepository;
    private final GameRepository gameRepository;
    private final GameConfigurationRepository gameConfigurationRepository;
    private final GameVariationRepository gameTemplateRepository;
    private final InternalTableService internalTableService;

    @Autowired(required = true)
    public GigaspaceRemotingTableService(final TableDAO tableDao,
                                         final ClientRepository clientRepository,
                                         final TableRepository tableRepository,
                                         final GameRepository gameRepository,
                                         final GameConfigurationRepository gameConfigurationRepository,
                                         final GameVariationRepository gameTemplateRepository,
                                         final InternalTableService internalTableService) {
        notNull(tableDao, "tableDao may not be null");
        notNull(clientRepository, "clientRepository may not be null");
        notNull(tableRepository, "tableRepository may not be null");
        notNull(gameRepository, "gameRepository may not be null");
        notNull(gameTemplateRepository, "gameTemplateRepository may not be null");
        notNull(internalTableService, "internalTableService may not be null");

        this.tableDao = tableDao;
        this.clientRepository = clientRepository;
        this.tableRepository = tableRepository;
        this.gameRepository = gameRepository;
        this.gameConfigurationRepository = gameConfigurationRepository;
        this.gameTemplateRepository = gameTemplateRepository;
        this.internalTableService = internalTableService;
    }


    @Override
    public TableSummary createPublicTable(final String gameType,
                                          final String gameVariationName,
                                          final String clientId,
                                          final String tableName,
                                          final Set<String> tags)
            throws TableException {
        return internalTableService.createPublicTable(gameType, gameVariationName, clientId, tableName, tags);
    }


    @Override
    public TableSummary createPublicTable(final String gameType,
                                          final String tableName,
                                          final Set<String> tags)
            throws TableException {
        return internalTableService.createPublicTable(gameType, tableName, tags);
    }

    @Override
    public TableSummary createPrivateTableForPlayer(final String gameType,
                                                    final String gameVariationName,
                                                    final String clientId,
                                                    final String tableName,
                                                    final BigDecimal owningPlayerId)
            throws TableException {
        return internalTableService.createPrivateTableForPlayer(gameType, gameVariationName, clientId, tableName, owningPlayerId);
    }

    @Override
    public TableGameSummary findGameSummaryById(@Routing final BigDecimal tableId) {
        notNull(tableId, "tableId may not be null");

        final Table table = tableRepository.findById(tableId);
        if (table != null) {
            return table.summariseGame();
        }
        return null;
    }

    @Override
    public TableSummary findSummaryById(@Routing final BigDecimal tableId) {
        notNull(tableId, "tableId may not be null");

        final Table table = tableRepository.findById(tableId);
        if (table != null) {
            final GameRules gameRules = gameRepository.getGameRules(table.getGameTypeId());
            return table.summarise(gameRules);
        }
        return null;
    }


    @Override
    public Map<String, TableSummary> findAllTablesOwnedByPlayer(final BigDecimal playerId) {
        notNull(playerId, "playerId may not be null");

        final Map<String, TableSummary> privateTableDetails = new HashMap<>();
        final Set<TableSummary> privateTables = tableRepository.findAllTablesOwnedByPlayer(playerId);
        if (privateTables != null) {
            for (final TableSummary summary : privateTables) {
                privateTableDetails.put(summary.getGameTypeId(), summary);
            }
        }
        return privateTableDetails;
    }

    @Override
    public TableSummary findTableByGameTypeAndPlayerId(final String gameType,
                                                       final BigDecimal playerId) {
        notNull(gameType, "gameTypeId may not be null");
        notNull(playerId, "playerId may not be null");

        final TableSummary tableSummary = tableRepository.findTableByGameTypeAndPlayer(gameType, playerId);
        if (tableSummary == null) {
            return null;
        }
        return tableSummary;
    }

    @Override
    public PagedData<TableSummary> findByType(final TableType tableType,
                                              final int page,
                                              final TableSearchOption... options) {
        notNull(tableType, "tableType may not be null");

        return tableRepository.findByType(tableType, page, options);
    }

    @Override
    public int countByType(final TableType tableType,
                           final TableSearchOption... options) {
        notNull(tableType, "tableType may not be null");

        return tableRepository.countByType(tableType, options);
    }

    @Override
    public void closeTable(@Routing final BigDecimal tableId) {
        notNull(tableId, "tableId may not be null");

        tableRepository.sendControlMessage(tableId, CLOSE);
    }

    @Override
    public void asyncCloseTable(@Routing final BigDecimal tableId) {
        // as per http://www.gigaspaces.com/wiki/display/XAP7/Executor+Based+Remoting, the
        // implementation is empty and #closeTable will be invoked
    }

    @Override
    public void loadAll() {
        tableDao.visitTables(TableStatus.open, new Visitor<Table>() {
            public void visit(final Table table) {
                tableRepository.sendControlMessage(table.getTableId(), LOAD);
            }
        });
    }

    @Override
    public void asyncLoadAll() {
        // as per http://www.gigaspaces.com/wiki/display/XAP7/Executor+Based+Remoting, the
        // implementation is empty and #loadAll will be invoked
    }

    @Override
    public void unload(@Routing final BigDecimal tableId) {
        notNull(tableId, "tableId may not be null");

        tableRepository.sendControlMessage(tableId, UNLOAD);
    }

    @Override
    public void asyncUnload(@Routing final BigDecimal tableId) {
        // as per http://www.gigaspaces.com/wiki/display/XAP7/Executor+Based+Remoting, the
        // implementation is empty and #unload will be invoked
    }

    @Override
    public void shutdown(@Routing final BigDecimal tableId) {
        notNull(tableId, "tableId may not be null");

        tableRepository.sendControlMessage(tableId, SHUTDOWN);
    }

    @Override
    public void asyncShutdown(@Routing final BigDecimal tableId) {
        // as per http://www.gigaspaces.com/wiki/display/XAP7/Executor+Based+Remoting, the
        // implementation is empty and #shutdown will be invoked
    }

    @Override
    public void shutdownGame(final String gameTypeId) {
        notNull(gameTypeId, "gameTypeId may not be null");
        final Set<BigDecimal> tablesForGame = tableRepository.findAllTablesForGameType(gameTypeId);
        for (BigDecimal tableId : tablesForGame) {
            tableRepository.sendControlMessage(tableId, SHUTDOWN);
        }
    }

    @Override
    public void asyncShutdownGame(final String gameTypeId) {
        // as per http://www.gigaspaces.com/wiki/display/XAP7/Executor+Based+Remoting, the
        // implementation is empty and #shutdownGame will be invoked
    }

    @Override
    public void reOpen(@Routing final BigDecimal tableId) {
        notNull(tableId, "tableId may not be null");

        tableRepository.sendControlMessage(tableId, REOPEN);
    }

    @Override
    public void asyncReOpen(@Routing final BigDecimal tableId) {
        // as per http://www.gigaspaces.com/wiki/display/XAP7/Executor+Based+Remoting, the
        // implementation is empty and #reOpen will be invoked
    }

    @Override
    public void reset(@Routing final BigDecimal tableId) {
        notNull(tableId, "tableId may not be null");

        tableRepository.sendControlMessage(tableId, RESET);
    }

    @Override
    public void asyncReset(@Routing final BigDecimal tableId) {
        // as per http://www.gigaspaces.com/wiki/display/XAP7/Executor+Based+Remoting, the
        // implementation is empty and #reset will be invoked
    }

    @Override
    public void testReplaceGame(@Routing final BigDecimal tableId,
                                final GameStatus gameStatus) {
        notNull(tableId, "tableId may not be null");
        notNull(gameStatus, "gameStatus may not be null");

        tableRepository.sendRequest(new TestAlterGameRequest(tableId, gameStatus));
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
        notNull(tableId, "tableId may not be null");

        tableRepository.forceNewGame(tableId, playersAtTable, variationTemplateId, clientId, accountIds);
    }

    @Override
    public void sendCommand(@Routing("getTableId") final Command command) {
        notNull(command, "command may not be null");

        final String commandId = tableRepository.sendRequest(new CommandWrapper(command));
        LOG.debug("Submitted command to the table service: id={}, command={}", commandId, command);
    }

    @Override
    public void asyncSendCommand(@Routing("getTableId") final Command command) {
        // as per http://www.gigaspaces.com/wiki/display/XAP7/Executor+Based+Remoting, the
        // implementation is empty and #sendCommand will be invoked
    }

    @Override
    public int countOutstandingRequests() {
        return tableRepository.countOutstandingRequests();
    }

    @Override
    public int countOutstandingRequests(final BigDecimal tableId) {
        notNull(tableId, "tableId may not be null");

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
    public GameClient findClientById(final String clientId) {
        final Client client = clientRepository.findById(clientId);
        if (client != null) {
            return client.asGameClient();
        }
        return null;
    }

    @Override
    public Set<GameClient> findAllClientsFor(final String gameTypeId) {
        notNull(gameTypeId, "gameTypeId may not be null");

        final Client[] clientsForType = clientRepository.findAll(gameTypeId);
        if (clientsForType != null) {
            return newHashSet(transform(asList(clientsForType), GAME_CLIENT_TRANSFORMER));
        }

        return Collections.emptySet();
    }

    @Override
    public Collection<GameConfiguration> getGameConfigurations() {
        return gameConfigurationRepository.retrieveAll();
    }

    public void makeReservationAtTable(final BigDecimal tableId,
                                       final BigDecimal playerId) {
        tableRepository.makeReservationAtTable(tableId, playerId);
    }

    GameVariation getGameVariation(@Routing final BigDecimal gameVariationId) {
        notNull(gameVariationId, "gameVariationId may not be null");

        return gameTemplateRepository.findById(gameVariationId);
    }

    private static class GameClientTransformer implements Function<Client, GameClient> {
        @Override
        public GameClient apply(final Client input) {
            if (input != null) {
                return input.asGameClient();
            }
            return null;
        }
    }
}
