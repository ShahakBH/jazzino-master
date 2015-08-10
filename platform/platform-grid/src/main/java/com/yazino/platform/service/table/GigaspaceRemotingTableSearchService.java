package com.yazino.platform.service.table;

import com.gigaspaces.client.ReadModifiers;
import com.google.common.base.Function;
import com.yazino.platform.model.table.Client;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.model.table.TableReservation;
import com.yazino.platform.repository.table.ClientRepository;
import com.yazino.platform.repository.table.GameRepository;
import com.yazino.platform.table.TableSearchCriteria;
import com.yazino.platform.table.TableSearchResult;
import com.yazino.platform.table.TableSearchService;
import com.yazino.platform.table.TableStatus;
import org.openspaces.core.GigaSpace;
import org.openspaces.remoting.RemotingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import com.yazino.game.api.GameRules;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.Validate.notNull;

@RemotingService
public class GigaspaceRemotingTableSearchService implements TableSearchService {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceRemotingTableSearchService.class);

    private final GigaSpace tableSpace;
    private final ClientRepository clientRepository;
    private final GameRepository gameRepository;

    @Autowired
    public GigaspaceRemotingTableSearchService(@Qualifier("gigaSpace") final GigaSpace tableSpace,
                                               final ClientRepository clientRepository,
                                               final GameRepository gameRepository) {
        notNull(tableSpace, "tableSpace is null");
        notNull(clientRepository, "clientRepository is null");
        notNull(gameRepository, "gameRepository is null");

        this.tableSpace = tableSpace;
        this.clientRepository = clientRepository;
        this.gameRepository = gameRepository;
    }

    @Override
    public Collection<TableSearchResult> findTables(final BigDecimal playerId,
                                                    final TableSearchCriteria searchCriteria) {
        LOG.debug("findTables PlayerId:{}, criteria:{}", playerId, searchCriteria);

        final Table tableTemplate = buildTemplate(searchCriteria);

        final Table[] tables = tableSpace.readMultiple(tableTemplate, Integer.MAX_VALUE, ReadModifiers.DIRTY_READ);

        if (tables.length == 0) {
            LOG.debug("No tables found matching criteria");
            return Collections.emptyList();
        }

        final TableTransformer tableTransformer = new TableTransformer(
                playerId, searchCriteria.getExcludedTables());

        final Set<TableSearchResult> results = newHashSet(transform(asList(tables), tableTransformer));

        // a 'null' value is added if the transformer cannot transform the table due to it not being a viable candidate
        results.remove(TableSearchResult.NULL);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Found {} tables [{}]", results.size(), results);
        }
        return results;
    }

    private int countNumberOfReservationsForTable(final BigDecimal tableId) {
        final TableReservation reservation = new TableReservation();
        reservation.setTableId(tableId);
        return tableSpace.count(reservation);
    }

    private static Table buildTemplate(final TableSearchCriteria searchCriteria) {
        final Table tableTemplate = new Table();
        tableTemplate.setGameTypeId(searchCriteria.getGameType());
        tableTemplate.setClientId(searchCriteria.getClientId());
        tableTemplate.setTemplateName(searchCriteria.getVariation());
        tableTemplate.setTableStatus(TableStatus.open);
        tableTemplate.setFull(false);
        tableTemplate.setShowInLobby(true);
        tableTemplate.setAvailableForPlayersJoining(true);
        if (searchCriteria.getTags() != null && !searchCriteria.getTags().isEmpty()) {
            tableTemplate.setTags(searchCriteria.getTags());
        }
        return tableTemplate;
    }

    private final class TableTransformer implements Function<Table, TableSearchResult> {

        private final BigDecimal playerId;
        private final Collection<BigDecimal> exclusions;

        private TableTransformer(final BigDecimal playerId,
                                 final Set<BigDecimal> exclusions) {
            this.playerId = playerId;
            if (exclusions != null) {
                this.exclusions = new HashSet<>(exclusions);
            } else {
                this.exclusions = Collections.emptyList();
            }
        }

        @Override
        public TableSearchResult apply(final Table table) {
            final BigDecimal tableId = table.getTableId();
            final int maxSeats = seatsFor(table);
            if (isExcludedTable(tableId) || playerAlreadyAtTable(table) || alreadyFull(table, maxSeats)) {
                return TableSearchResult.NULL;
            }

            final GameRules gameRules = gameRepository.getGameRules(table.getGameTypeId());
            final int currentPlayers = table.numberOfPlayers(gameRules);
            final int reservations = countNumberOfReservationsForTable(tableId);
            final int takenSeats = currentPlayers + reservations;
            if (takenSeats >= maxSeats) {
                return TableSearchResult.NULL;
            }

            final TableSearchResult result = new TableSearchResult();
            result.setTableId(tableId);
            result.setSpareSeats(maxSeats - takenSeats);
            result.setMaxSeats(maxSeats);
            result.setJoiningDesirability(table.getJoiningDesirability(gameRules));

            return result;
        }

        private int seatsFor(final Table table) {
            if (table.getClient() == null && table.getClientId() == null) {
                throw new IllegalStateException("Malformed table: missing client on: " + table);
            }

            if (table.getClient() != null && table.getClient().getNumberOfSeats() != null) {
                return table.getClient().getNumberOfSeats();
            }

            if (table.getClientId() != null) {
                final Client client = clientRepository.findById(table.getClientId());
                if (client != null) {
                    return client.getNumberOfSeats();
                }
            }

            throw new IllegalStateException("Cannot determine maximum number of seats for table " + table);
        }

        private boolean alreadyFull(final Table table, final int maxSeats) {
            final GameRules gameRules = gameRepository.getGameRules(table.getGameTypeId());
            return (table.numberOfPlayers(gameRules) >= maxSeats);
        }

        private boolean playerAlreadyAtTable(final Table table) {
            return table.playerAtTable(playerId) != null;
        }

        private boolean isExcludedTable(final BigDecimal tableId) {
            return !exclusions.isEmpty() && exclusions.contains(tableId);
        }

    }

}
