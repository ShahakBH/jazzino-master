package com.yazino.host.table;

import com.thoughtworks.xstream.XStream;
import com.yazino.host.table.game.StandaloneGameRepository;
import com.yazino.model.ReadableGameStatusSource;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.model.table.TableControlMessageType;
import com.yazino.platform.model.table.TableRequest;
import com.yazino.platform.repository.table.GameVariationRepository;
import com.yazino.platform.repository.table.TableRepository;
import com.yazino.platform.table.TableSearchOption;
import com.yazino.platform.table.TableStatus;
import com.yazino.platform.table.TableSummary;
import com.yazino.platform.table.TableType;
import com.yazino.platform.util.Visitor;
import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.yazino.game.api.GameRules;
import com.yazino.game.api.GameStatus;
import com.yazino.game.api.GameType;
import com.yazino.game.api.PlayerAtTableInformation;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Component("tableRepository")
public class StandaloneTableRepository implements TableRepository, ReadableGameStatusSource {
    private final Table table;
    private final StandaloneGameRepository gameRepository;

    @Autowired
    public StandaloneTableRepository(@Value("${standalone-server.game-type}") final String gameType,
                                     final GameVariationRepository gameVariationRepository,
                                     final StandaloneGameRepository gameRepository) {
        this.gameRepository = gameRepository;

        table = new Table();
        table.setTableId(BigDecimal.ONE);
        table.setTableStatus(TableStatus.open);
        table.setClientId("defaut");
        table.setTableName(gameType);
        table.setTemplateName("default");
        table.setGameType(new GameType(gameType, gameType, Collections.<String>emptySet()));
        gameVariationRepository.populateProperties(table);
    }

    @Override
    public Table findById(final BigDecimal tableId) {
        return table;
    }

    @Override
    public Table findById(final BigDecimal tableId, final int waitTimeout) {
        return table;
    }

    @Override
    public void save(final Table tableToBeSaved) {
        //do nothing
    }

    @Override
    public void nonPersistentSave(final Table tableToBeSaved) {
        //do nothing
    }

    @Override
    public void sendControlMessage(final BigDecimal tableId, final TableControlMessageType messageType) {
    }

    @Override
    public void forceNewGame(final BigDecimal tableId,
                             final Collection<PlayerAtTableInformation> playersAtTable,
                             final BigDecimal variationTemplateId,
                             final String clientId,
                             final Map<BigDecimal, BigDecimal> accountIds) {
    }

    @Override
    public void unload(final BigDecimal tableId) {
    }

    @Override
    public int countTablesWithPlayers(final String gameType) {
        return 1;
    }

    @Override
    public TableSummary findTableByGameTypeAndPlayer(final String gameType, final BigDecimal ownerId) {
        final GameRules gameRules = gameRepository.getGameRules(table.getGameTypeId());
        return table.summarise(gameRules);
    }

    @Override
    public Set<BigDecimal> findAllTablesForGameType(final String gameType) {
        throw new UnsupportedOperationException("Not implement");

    }

    @Override
    public Set<BigDecimal> findAllLocalPlayersForGameType(final String gameType) {
        throw new UnsupportedOperationException("Not implement");
    }

    @Override
    public Set<TableSummary> findAllTablesOwnedByPlayer(final BigDecimal playerId) {
        return null;
    }

    @Override
    public void visitAllLocalTables(final Visitor<Table> visitor) {
    }

    @Override
    public Set<BigDecimal> findAllLocalPlayers() {
        final GameRules gameRules = gameRepository.getGameRules(table.getGameTypeId());
        return table.playerIds(gameRules);
    }

    @Override
    public Set<BigDecimal> findAllLocalTablesWithPlayers() {
        return Collections.singleton(BigDecimal.ONE);
    }

    @Override
    public void makeReservationAtTable(final BigDecimal tableId, final BigDecimal playerId) {
    }

    @Override
    public void removeReservationForTable(final BigDecimal tableId, final BigDecimal playerId) {
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
    public String sendRequest(final TableRequest request) {
        return null;
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
    public int countOpenTables(final Collection<BigDecimal> tableIds) {
        return 0;
    }

    @Override
    public Set<PlayerAtTableInformation> findLocalActivePlayers(final Collection<BigDecimal> tableIds) {
        return null;
    }

    @Override
    public String getStatus() {
        final GameStatus gameStatus = table.getCurrentGame();
        if (gameStatus == null) {
            return "(no game status)";
        }
        return StringEscapeUtils.escapeHtml4(new XStream().toXML(gameStatus));
    }
}
