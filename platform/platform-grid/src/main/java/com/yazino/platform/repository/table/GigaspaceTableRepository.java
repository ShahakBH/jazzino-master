package com.yazino.platform.repository.table;

import com.gigaspaces.async.AsyncFuture;
import com.gigaspaces.client.CountModifiers;
import com.gigaspaces.client.ReadModifiers;
import com.gigaspaces.client.WriteModifiers;
import com.j_spaces.core.LeaseContext;
import com.j_spaces.core.client.SQLQuery;
import com.yazino.platform.grid.Routing;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.model.table.*;
import com.yazino.platform.table.TableSearchOption;
import com.yazino.platform.table.TableStatus;
import com.yazino.platform.table.TableSummary;
import com.yazino.platform.table.TableType;
import com.yazino.platform.util.Visitor;
import net.jini.core.lease.Lease;
import org.joda.time.DateTimeConstants;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import com.yazino.game.api.GameRules;
import com.yazino.game.api.PlayerAtTableInformation;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

@Component("tableRepository")
public class GigaspaceTableRepository implements TableRepository {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceTableRepository.class);

    private static final int BATCH_SIZE = 1000;
    private static final TableIdComparator TABLE_ID_COMPARATOR = new TableIdComparator();
    private static final int THIRTY_SECONDS = 30 * DateTimeConstants.MILLIS_PER_SECOND;
    private static final long ONE_HUNDRED_MS = 100L;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int QUERY_TIMEOUT = 5;

    private final GameRepository gameRepository;
    private final ClientRepository clientRepository;
    private final GigaSpace localGigaSpace;
    private final GigaSpace globalGigaSpace;
    private final Routing routing;

    private long timeout = ONE_HUNDRED_MS;
    private int pageSize = DEFAULT_PAGE_SIZE;
    private long reservationLeaseTime = THIRTY_SECONDS;

    @Autowired
    public GigaspaceTableRepository(@Qualifier("gigaSpace") final GigaSpace localGigaSpace,
                                    @Qualifier("globalGigaSpace") final GigaSpace globalGigaSpace,
                                    final Routing routing,
                                    final ClientRepository clientRepository,
                                    final GameRepository gameRepository) {
        notNull(localGigaSpace, "localGigaSpace may not be null");
        notNull(globalGigaSpace, "globalGigaSpace may not be null");
        notNull(routing, "routing may not be null");
        notNull(clientRepository, "clientRepository may not be null");
        notNull(gameRepository, "gameRepository may not be null");

        this.localGigaSpace = localGigaSpace;
        this.globalGigaSpace = globalGigaSpace;
        this.routing = routing;
        this.clientRepository = clientRepository;
        this.gameRepository = gameRepository;
    }

    public void setReservationLeaseTime(final long reservationLeaseTime) {
        this.reservationLeaseTime = reservationLeaseTime;
    }

    public void setPageSize(final int pageSize) {
        this.pageSize = pageSize;
    }

    private Table readById(final BigDecimal id,
                           final long readTimeout,
                           final ReadModifiers modifiers) {
        return spaceFor(id).readById(Table.class, id, id, readTimeout, modifiers);
    }

    public void save(final Table table) {
        fillClient(table);
        LOG.debug("Entering save for Table with ID: {}", table.getTableId());
        table.setLastUpdated(System.currentTimeMillis());
        final GigaSpace spaceReference = spaceFor(table.getTableId());
        spaceReference.write(table, Lease.FOREVER, timeout, WriteModifiers.UPDATE_OR_WRITE);
        spaceReference.write(new TablePersistenceRequest(table.getTableId()));
    }

    @Override
    public void nonPersistentSave(final Table table) {
        fillClient(table);
        LOG.debug("Entering non-persistent save for Table with ID: {}", table.getTableId());
        spaceFor(table.getTableId()).write(table, Lease.FOREVER, timeout, WriteModifiers.UPDATE_OR_WRITE);
    }

    @Override
    public void sendControlMessage(final BigDecimal tableId,
                                   final TableControlMessageType messageType) {
        notNull(tableId, "tableId may not be null");
        notNull(messageType, "messageType may not be null");

        spaceFor(tableId).write(new TableRequestWrapper(new TableControlMessage(tableId, messageType)));
    }

    @Override
    public void forceNewGame(final BigDecimal tableId,
                             final Collection<PlayerAtTableInformation> playersAtTable,
                             final BigDecimal variationTemplateId,
                             final String clientId,
                             final Map<BigDecimal, BigDecimal> accountIds) {
        notNull(tableId, "tableId may not be null");

        spaceFor(tableId).write(new TableRequestWrapper(new ForceNewGameRequest(
                tableId, playersAtTable, variationTemplateId, clientId, accountIds)));
    }

    public void unload(final BigDecimal tableId) {
        LOG.debug("Entering unload for Table with ID: {}", tableId);
        spaceFor(tableId).takeById(Table.class, tableId);
    }

    public Table findById(final BigDecimal tableId) {
        return findById(tableId, 0);
    }

    @Override
    public void makeReservationAtTable(final BigDecimal tableId,
                                       final BigDecimal playerId) {
        LOG.debug("Made reservation at table [{}] for player [{}]", tableId, playerId);
        final TableReservation reservation = new TableReservation(tableId, playerId);
        spaceFor(tableId).write(reservation, reservationLeaseTime);
    }

    @Override
    public void removeReservationForTable(final BigDecimal tableId,
                                          final BigDecimal playerId) {
        LOG.debug("Removed reservation for table [{}] for player [{}]", tableId, playerId);
        spaceFor(tableId).takeIfExists(new TableReservation(tableId, playerId));
    }


    public Table findById(final BigDecimal tableId,
                          final int waitTimeout) {
        LOG.debug("Entering findById for Table with ID: {}", tableId);
        return readById(tableId, waitTimeout, ReadModifiers.DIRTY_READ);
    }

    private Table fillClient(final Table table) {
        if (table == null) {
            return null;
        }
        if (table.getClientId() == null) {
            if (table.getClient() != null) {
                table.setClient(null);
            }

            LOG.debug("No client set on table");
            return table;
        }

        if (table.getClient() != null && table.getClientId().equals(table.getClient().getClientId())) {
            LOG.debug("Client is cached on table");
            return table;
        }

        LOG.debug("Updating client on table");
        table.setClient(clientRepository.findById(table.getClientId()));
        return table;
    }

    @Override
    public int countTablesWithPlayers(final String gameType) {
        final Table template = new Table();
        template.setGameTypeId(gameType);
        template.setHasPlayers(true);
        template.setShowInLobby(true);
        return globalGigaSpace.count(template, CountModifiers.DIRTY_READ);
    }

    @Override
    public TableSummary findTableByGameTypeAndPlayer(final String gameType, final BigDecimal ownerId) {
        try {
            final AsyncFuture<TableSummary> future = globalGigaSpace.execute(
                    new TableByGameTypeAndPlayerTask(gameType, ownerId));
            return future.get(QUERY_TIMEOUT, TimeUnit.SECONDS);

        } catch (Exception e) {
            final String msg = String.format("Cannot find table for game type %s and owner ID %s. Reason was:",
                    gameType, ownerId);
            LOG.error(msg, e);
            throw new IllegalStateException(msg, e);
        }
    }

    @Override
    public Set<BigDecimal> findAllTablesForGameType(final String gameType) {
        try {
            final AsyncFuture<ArrayList<TableSummary>> future
                    = globalGigaSpace.execute(new TableByGameTypeTask(gameType));
            final Set<BigDecimal> tableIds = new HashSet<>();
            for (TableSummary tableSummary : future.get(QUERY_TIMEOUT, TimeUnit.SECONDS)) {
                tableIds.add(tableSummary.getId());
            }
            return tableIds;

        } catch (Exception e) {
            final String msg = String.format("Cannot find table for game type %s. Reason was:", gameType);
            LOG.error(msg, e);
            throw new IllegalStateException(msg, e);
        }
    }

    @Override
    public Set<BigDecimal> findAllLocalPlayersForGameType(final String gameType) {
        final Set<BigDecimal> ids = new HashSet<>();
        final Set<Table> tables = findAllLocalTablesFor(gameType);
        for (Table table : tables) {
            final GameRules gameRules = gameRepository.getGameRules(table.getGameTypeId());
            ids.addAll(table.playerIds(gameRules));
        }
        return ids;
    }

    @Override
    public Set<TableSummary> findAllTablesOwnedByPlayer(final BigDecimal playerId) {
        try {
            final AsyncFuture<ArrayList<TableSummary>> future
                    = globalGigaSpace.execute(new TableByOwnershipTask(playerId));
            return new HashSet<>(future.get(QUERY_TIMEOUT, TimeUnit.SECONDS));

        } catch (Exception e) {
            final String msg = String.format("Cannot find tables for type player with ID %s. Reason was:", playerId);
            LOG.error(msg, e);
            throw new IllegalStateException(msg, e);
        }
    }

    @Override
    public Set<BigDecimal> findAllLocalPlayers() {
        final Table template = new Table();
        template.setHasPlayers(true);
        final Table[] tables = localGigaSpace.readMultiple(template, Integer.MAX_VALUE, ReadModifiers.DIRTY_READ);
        final Set<BigDecimal> ids = new HashSet<>();
        for (Table table : tables) {
            final GameRules gameRules = gameRepository.getGameRules(table.getGameTypeId());
            ids.addAll(table.playerIds(gameRules));
        }
        return ids;
    }

    @Override
    public Set<BigDecimal> findAllLocalTablesWithPlayers() {
        final Table template = new Table();
        template.setHasPlayers(true);
        final Table[] tables = localGigaSpace.readMultiple(template, Integer.MAX_VALUE, ReadModifiers.DIRTY_READ);
        final Set<BigDecimal> ids = new HashSet<>(tables.length);
        for (Table table : tables) {
            ids.add(table.getTableId());
        }
        return ids;
    }

    @Override
    public PagedData<TableSummary> findByType(final TableType tableType,
                                              final int page,
                                              final TableSearchOption... options) {
        notNull(tableType, "tableType may not be null");
        try {
            final AsyncFuture<ArrayList<TableSummary>> future
                    = globalGigaSpace.execute(new TableByTypeTask(tableType, options));
            final ArrayList<TableSummary> tables = future.get(QUERY_TIMEOUT, TimeUnit.SECONDS);
            if (tables == null || tables.isEmpty()) {
                return PagedData.empty();
            }
            Collections.sort(tables, TABLE_ID_COMPARATOR);

            final List<TableSummary> pagedTables = new ArrayList<>(pageSize);
            final int startIndex = page * pageSize;
            for (int i = startIndex; i < startIndex + pageSize && i < tables.size(); ++i) {
                pagedTables.add(tables.get(i));
            }

            return new PagedData<>(startIndex, pagedTables.size(), tables.size(), pagedTables);

        } catch (Exception e) {
            final String msg = String.format(
                    "Cannot find tables for type %s. Page was %s and search options were %s. Reason was:",
                    tableType.name(), page, Arrays.toString(options));
            LOG.error(msg, e);
            throw new IllegalStateException(msg, e);
        }
    }

    @Override
    public int countByType(final TableType tableType,
                           final TableSearchOption... options) {
        notNull(tableType, "tableType may not be null");

        return globalGigaSpace.count(templateFor(tableType, options));
    }

    @Override
    public String sendRequest(final TableRequest request) {
        notNull(request, "request may not be null");

        final LeaseContext<TableRequestWrapper> lc = spaceFor(request.getTableId()).write(new TableRequestWrapper(request));
        return lc.getUID();
    }

    @Override
    public int countOutstandingRequests() {
        return globalGigaSpace.count(new TableRequestWrapper());
    }

    @Override
    public int countOutstandingRequests(final BigDecimal tableId) {
        notNull(tableId, "tableId may not be null");

        final TableRequestWrapper template = new TableRequestWrapper();
        template.setTableId(tableId);
        return spaceFor(tableId).count(template);
    }

    @Override
    public void visitAllLocalTables(final Visitor<Table> visitor) {
        notNull(visitor, "visitor may not be null");

        final Table[] tables = localGigaSpace.readMultiple(new Table(), Integer.MAX_VALUE);
        if (tables != null) {
            for (Table table : tables) {
                visitor.visit(table);
            }
        }
    }

    /*
      * Yes OR is ugly but check bug: http://forum.openspaces.org/message.jspa?messageID=11246
      */
    private String constructSQLOrString(final int numberOfIds) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (int count = 0; count < numberOfIds - 1; count++) {
            stringBuilder.append("tableId = ? OR ");
        }
        stringBuilder.append("tableId = ?");
        return stringBuilder.toString();
    }

    @SuppressWarnings("SuspiciousToArrayCall")
    private int getOpenTableCountForBatch(final List<BigDecimal> tableIds) {
        LOG.debug("Get Open Table Count for Batch: {}", tableIds);

        if (tableIds.size() > BATCH_SIZE) {
            throw new UnsupportedOperationException("cannot process batch size " + tableIds.size());
        }

        final String query = "(" + constructSQLOrString(tableIds.size()) + ") AND openOrClosing=true";
        final SQLQuery<Table> sqlQuery = new SQLQuery<>(Table.class, query,
                (Object[]) tableIds.toArray(new BigDecimal[tableIds.size()]));

        final int tableCount = globalGigaSpace.count(sqlQuery, CountModifiers.DIRTY_READ);

        LOG.debug("Querying open tables: \"{}\" with parameters: \"{}\" found {} results",
                query, tableIds, tableCount);

        return tableCount;
    }

    public int countOpenTables(final Collection<BigDecimal> tableIds) {
        notNull(tableIds, "tableIds may not be null");

        LOG.debug("Get Open Table Count: {}", tableIds);

        if (tableIds.size() == 0) {
            return 0;
        }

        final List<BigDecimal> longList = new ArrayList<>(tableIds);
        int fromIndex = 0;
        int total = 0;
        while (fromIndex < longList.size()) {
            int toIndex = fromIndex + BATCH_SIZE;
            if (toIndex > longList.size()) {
                toIndex = longList.size();
            }
            total += getOpenTableCountForBatch(longList.subList(fromIndex, toIndex));
            fromIndex = toIndex;
        }

        LOG.debug("Open table count for {} is {}", tableIds, total);

        return total;
    }

    private Set<Table> findAllLocalTablesFor(final String gameType) {
        final Table template = new Table();
        template.setGameTypeId(gameType);
        template.setHasPlayers(true);
        return new HashSet<>(asList(localGigaSpace.readMultiple(template, Integer.MAX_VALUE, ReadModifiers.DIRTY_READ)));
    }


    @SuppressWarnings("SuspiciousToArrayCall")
    private Set<PlayerAtTableInformation> getLocallyActivePlayersForBatch(final Collection<BigDecimal> tableIds) {
        notEmpty(tableIds, "Table IDs must contain at least one table ID");

        LOG.debug("Get active players: {}", tableIds);

        final SQLQuery<Table> sqlQuery = new SQLQuery<>(Table.class,
                constructSQLOrString(tableIds.size()), (Object[]) tableIds.toArray(new BigDecimal[tableIds.size()]));
        final Table[] tableInfos = localGigaSpace.readMultiple(sqlQuery, BATCH_SIZE, ReadModifiers.DIRTY_READ);
        final Set<PlayerAtTableInformation> activePlayers = new HashSet<>();
        for (final Table tableInfo : tableInfos) {
            final GameRules gameRules = gameRepository.getGameRules(tableInfo.getGameTypeId());
            activePlayers.addAll(tableInfo.playersAtTableInformation(gameRules));
        }

        LOG.debug("Active player count for {} is {}", tableIds, activePlayers);
        return activePlayers;
    }


    @Override
    public Set<PlayerAtTableInformation> findLocalActivePlayers(final Collection<BigDecimal> tableIds) {
        notNull(tableIds, "tableIds may not be null");

        final Set<PlayerAtTableInformation> activePlayers = new HashSet<>();

        if (tableIds.size() == 0) {
            return activePlayers;
        }

        final List<BigDecimal> longList = new ArrayList<>(tableIds);
        int fromIndex = 0;

        while (fromIndex < longList.size()) {
            int toIndex = fromIndex + BATCH_SIZE;
            if (toIndex > longList.size()) {
                toIndex = longList.size();
            }
            activePlayers.addAll(getLocallyActivePlayersForBatch(longList.subList(fromIndex, toIndex)));
            fromIndex = toIndex;
        }

        LOG.debug("Retrieved {} activePlayers for {} tableIds", activePlayers.size(), tableIds.size());
        return activePlayers;
    }

    private Table templateFor(final TableType tableType,
                              final TableSearchOption... options) {
        final Table template = new Table();

        switch (tableType) {
            case ALL:
                break;
            case PUBLIC:
                template.setShowInLobby(true);
                break;
            case TOURNAMENT:
                template.setShowInLobby(false);
                template.setHasOwner(false);
                break;
            case PRIVATE:
                template.setHasOwner(true);
                break;
            default:
                throw new IllegalArgumentException("Unknown table type: " + tableType);
        }

        if (options != null) {
            for (TableSearchOption option : options) {
                switch (option) {
                    case IN_ERROR_STATE:
                        template.setTableStatus(TableStatus.error);
                        break;
                    case ONLY_OPEN:
                        template.setOpen(true);
                        break;
                    case ONLY_WITH_PLAYERS:
                        template.setHasPlayers(true);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown search option: " + option);
                }
            }
        }

        return template;
    }

    private GigaSpace spaceFor(final Object spaceId) {
        if (routing.isRoutedToCurrentPartition(spaceId)) {
            return localGigaSpace;
        }
        return globalGigaSpace;
    }

    private static class TableIdComparator implements Comparator<TableSummary> {
        @Override
        public int compare(final TableSummary table1, final TableSummary table2) {
            return table1.getId().compareTo(table2.getId());
        }
    }
}
