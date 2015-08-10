package senet.server.table;

import com.yazino.game.api.*;
import com.yazino.game.api.document.DocumentBuilder;
import com.yazino.game.api.document.Documentable;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.model.table.TableControlMessageType;
import com.yazino.platform.model.table.TableRequest;
import com.yazino.platform.model.table.TableReservation;
import com.yazino.platform.repository.table.TableRepository;
import com.yazino.platform.table.TableSearchOption;
import com.yazino.platform.table.TableSummary;
import com.yazino.platform.table.TableType;
import com.yazino.platform.util.Visitor;
import org.apache.commons.lang3.ObjectUtils;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.commons.lang3.Validate.notNull;

public class FitTableRepository implements TableRepository {

    private final Map<BigDecimal, Table> repository = new HashMap<BigDecimal, Table>();
    private final Map<BigDecimal, AtomicInteger> reservations = new HashMap<BigDecimal, AtomicInteger>();
    private long idSource = 0;

    public void clear() {
        repository.clear();
    }

    public void clear(final String gameType) {
        Map<BigDecimal, Table> copy = new HashMap<BigDecimal, Table>(repository);
        for (BigDecimal key : copy.keySet()) {
            if (ObjectUtils.equals(copy.get(key).getGameTypeId(), gameType)) {
                repository.remove(key);
            }
        }
    }

    public Set<Table> getByGameType(final String gameType) {
        final Set<Table> results = new HashSet<Table>();

        for (final Table table : repository.values()) {
            if (ObjectUtils.equals(gameType, table.getGameTypeId())) {
                results.add(table);
            }
        }

        return results;
    }

    @Override
    public void sendControlMessage(final BigDecimal tableId,
                                   final TableControlMessageType messageType) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public void forceNewGame(final BigDecimal tableId,
                             final Collection<PlayerAtTableInformation> playersAtTable,
                             final BigDecimal variationTemplateId,
                             final String clientId,
                             final Map<BigDecimal, BigDecimal> accountIds) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    public void addPlayerToTable(final BigDecimal tableId) {
        for (Table table : repository.values()) {
            if (table.getTableId().compareTo(tableId) == 0) {
                final int numberOfPlayers = table.numberOfPlayers(new DummyGameRules());
                new DummyGameStatus(table.getCurrentGame()).addPlayer(
                        new GamePlayer(BigDecimal.valueOf(numberOfPlayers), null, "Player " + numberOfPlayers));

                break;
            }
        }
    }

    public void addPlayerToTable(BigDecimal playerId, BigDecimal tableId) {
        for (Table table : repository.values()) {
            if (table.getTableId().compareTo(tableId) == 0) {
                new DummyGameStatus(table.getCurrentGame()).addPlayer(
                        new GamePlayer(playerId, null, "Player " + playerId));
                break;
            }
        }

    }

    public int getNumberOfTables() {
        return repository.size();
    }

    @Override
    public Table findById(final BigDecimal tableId) {
        return repository.get(tableId);
    }

    @Override
    public Table findById(final BigDecimal tableId, final int waitTimeout) {
        return findById(tableId);
    }

    @Override
    public void save(final Table table) {
        table.setCurrentGame(new GameStatus(new DummyGameStatus()));
        if (table.getTableId() == null) {
            table.setTableId(BigDecimal.valueOf(idSource++));
        }
        repository.put(table.getTableId(), table);
    }

    @Override
    public void nonPersistentSave(final Table table) {
        save(table);
    }

    @Override
    public void unload(final BigDecimal tableId) {
        repository.remove(tableId);
    }


    public Set<Table> findAvailableToJoinByGameTypeAndTemplate(final String gameType,
                                                               final String gameVariationTemplateName,
                                                               final String partnerId,
                                                               final String clientId) {

        final Set<Table> results = new HashSet<>();

        for (final Table table : repository.values()) {
            if (ObjectUtils.equals(gameType, table.getGameTypeId())
                    && ObjectUtils.equals(gameVariationTemplateName, table.getTemplateName())
                    && ObjectUtils.equals(clientId, table.getClientId())
                    && (table.getFull() == null || !table.getFull())) {
                results.add(table);
            }
        }

        return results;
    }

    public int countTablesWithPlayers(String gameType) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public TableSummary findTableByGameTypeAndPlayer(String gameType, BigDecimal ownerId) {
        throw new UnsupportedOperationException("Not implement");
    }

    @Override
    public Set<BigDecimal> findAllTablesForGameType(String gameType) {
        throw new UnsupportedOperationException("Not implement");
    }

    @Override
    public Set<BigDecimal> findAllLocalPlayersForGameType(String gameType) {
        throw new UnsupportedOperationException("Not implement");
    }

    @Override
    public void makeReservationAtTable(BigDecimal tableId, BigDecimal playerId) {
        if (!reservations.containsKey(tableId)) {
            reservations.put(tableId, new AtomicInteger(0));
        }
        reservations.get(tableId).incrementAndGet();
    }

    @Override
    public void removeReservationForTable(BigDecimal tableId, BigDecimal playerId) {
    }

    public void removeReservation(BigDecimal playerId, BigDecimal tableId) {
        if (reservations.containsKey(tableId)) {
            reservations.get(tableId).decrementAndGet();
        }
    }

    private static class TimedTableReservation {
        private final long creationTime;
        private final TableReservation reservation;

        private TimedTableReservation(long creationTime, TableReservation reservation) {
            this.creationTime = creationTime;
            this.reservation = reservation;
        }

        public long getCreationTime() {
            return creationTime;
        }

        public TableReservation getReservation() {
            return reservation;
        }

        public boolean isExpired(long toCompare) {
            return toCompare >= creationTime;
        }
    }

    private static class DummyGameStatus implements Serializable, Documentable {
        private static final long serialVersionUID = -292380214846744836L;

        Collection<PlayerAtTableInformation> playerAtTableInformationCollection;

        private DummyGameStatus() {
        }

        @SuppressWarnings("unchecked")
        public DummyGameStatus(final Map<String, Object> document) {
            notNull(document, "document may not be null");

            playerAtTableInformationCollection = (Collection<PlayerAtTableInformation>) document.get("playerAtTableInformationCollection");
        }

        @Override
        public Map<String, Object> toDocument() {
            return new DocumentBuilder()
                    .withCollectionOf("playerAtTableInformationCollection", playerAtTableInformationCollection)
                    .toDocument();
        }

        public void addPlayer(final GamePlayer player) {
            playerAtTableInformationCollection.add(new PlayerAtTableInformation(player, Collections.<String, String>emptyMap()));
        }
    }

    public static class DummyGameRules implements GameRules {
        @Override
        public String getGameType() {
            return null;
        }

        @Override
        public GameMetaData getMetaData() {
            return null;
        }

        @Override
        public ExecutionResult execute(final ExecutionContext executionContext, final Command c) throws GameException {
            return null;
        }

        @Override
        public ExecutionResult execute(final ExecutionContext executionContext, final ScheduledEvent evt) throws GameException, IllegalAccessException, ClassNotFoundException, InstantiationException, NoSuchMethodException, InvocationTargetException {
            return null;
        }

        @Override
        public ExecutionResult startNewGame(final GameCreationContext creationContext) {
            return null;
        }

        @Override
        public ExecutionResult startNextGame(final ExecutionContext executionContext) {
            return null;
        }

        @Override
        public ExecutionResult processTransactionResult(final ExecutionContext context, final TransactionResult result) throws GameException {
            return null;
        }

        @Override
        public ExecutionResult processExternalCallResult(ExecutionContext context, ExternalCallResult result) throws GameException {
            return null;
        }

        @Override
        public Collection<PlayerAtTableInformation> getPlayerInformation(final GameStatus gameStatus) {
            if ((new DummyGameStatus(gameStatus)).playerAtTableInformationCollection == null) {
                new DummyGameStatus(gameStatus).playerAtTableInformationCollection = new ArrayList<PlayerAtTableInformation>();
            }
            return new DummyGameStatus(gameStatus).playerAtTableInformationCollection;
        }

        @Override
        public boolean isAPlayer(final GameStatus gameStatus, final GamePlayer player) {
            if (player == null) {
                return false;
            }
            for (PlayerAtTableInformation playerAtTableInformation : new DummyGameStatus(gameStatus).playerAtTableInformationCollection) {
                if (player.equals(playerAtTableInformation.getPlayer())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isComplete(final GameStatus gameStatus) {
            return false;
        }

        @Override
        public boolean isAvailableForPlayerJoining(final GameStatus gameStatus) {
            return true;
        }

        public boolean canBeClosed(final GameStatus gameStatus) {
            return false;
        }

        @Override
        public ObservableStatus getObservableStatus(final GameStatus gameStatus, final ObservableContext context) {
            return null;
        }

        @Override
        public int getNumberOfSeatsTaken(final GameStatus gameStatus) {
            return 0;
        }

        @Override
        public int getJoiningDesirability(final GameStatus gameStatus) {
            return 0;
        }

        public String toAuditString(final GameStatus gameStatus) {
            return "";
        }
    }

    @Override
    public Set<TableSummary> findAllTablesOwnedByPlayer(BigDecimal arg0) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Set<BigDecimal> findAllLocalPlayers() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Set<BigDecimal> findAllLocalTablesWithPlayers() {
        throw new UnsupportedOperationException("Not implemented");
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
    public String sendRequest(final TableRequest request) {
        throw new UnsupportedOperationException("Unimplemented");
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
    public void visitAllLocalTables(final Visitor<Table> visitor) {
        for (Table table : repository.values()) {
            visitor.visit(table);
        }
    }

    @Override
    public int countOpenTables(final Collection<BigDecimal> tableIds) {
        throw new UnsupportedOperationException("Unimplemented");
    }


    @Override
    public Set<PlayerAtTableInformation> findLocalActivePlayers(final Collection<BigDecimal> tableIds) {
        throw new UnsupportedOperationException("Unimplemented");
    }
}
