package com.yazino.platform.service.tournament;

import com.gigaspaces.async.AsyncResult;
import com.yazino.game.api.PlayerAtTableInformation;
import com.yazino.platform.grid.Executor;
import com.yazino.platform.model.table.Client;
import com.yazino.platform.model.tournament.PlayerGroup;
import com.yazino.platform.repository.table.ClientRepository;
import com.yazino.platform.repository.table.TableRepository;
import com.yazino.platform.service.table.InternalTableService;
import com.yazino.platform.table.TableException;
import org.openspaces.core.executor.AutowireTask;
import org.openspaces.core.executor.DistributedTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

@Service
public class GigaspaceTournamentTableService implements TournamentTableService {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceTournamentTableService.class);

    private final TableRepository tableGlobalRepository;
    private final InternalTableService internalTableService;
    private final ClientRepository clientRepository;
    private final Executor executor;

    @Autowired
    public GigaspaceTournamentTableService(final InternalTableService internalTableService,
                                           final TableRepository tableGlobalRepository,
                                           final ClientRepository clientRepository,
                                           final Executor executor) {
        notNull(internalTableService, "internalTableService may not be null");
        notNull(tableGlobalRepository, "tableGlobalRepository may not be null");
        notNull(clientRepository, "clientRepository may not be null");
        notNull(executor, "executor may not be null");

        this.internalTableService = internalTableService;
        this.tableGlobalRepository = tableGlobalRepository;
        this.clientRepository = clientRepository;
        this.executor = executor;
    }

    public List<BigDecimal> createTables(
            final int numberOfTables,
            final String gameType,
            final BigDecimal templateId,
            final String clientId,
            final String partnerId,
            final String tableName) {
        LOG.debug("Creating {} tables: game type: {}, template: {}, client: {}, partner: {}, name: {}",
                numberOfTables, gameType, templateId, clientId, partnerId, tableName);

        notBlank(gameType, "Game type may not be blank");
        notNull(templateId, "Template ID may not be null");
        notBlank(clientId, "Client ID may not be blank");
        notBlank(partnerId, "Partner ID may not be blank");
        notBlank(tableName, "Table Name may not be blank");

        final List<BigDecimal> tableIds = new ArrayList<>();
        for (int i = 1; i <= numberOfTables; ++i) {
            LOG.debug("Creating table: game type: {}, template: {}, partner: {},"
                    + " client: {}, show in lobby: {}, name: {}",
                    gameType, templateId, partnerId, clientId, false, tableName);

            try {
                final BigDecimal tableId = internalTableService.createTournamentTable(
                        gameType, templateId, clientId, tableName);
                tableIds.add(tableId);
            } catch (TableException e) {
                LOG.error("Table creation failed for: game type: {}, template: {}, partner: {}, client: {}, show in lobby: {}, name: {}",
                        gameType, templateId, partnerId, clientId, false, tableName, e);
                throw new IllegalStateException("Cannot create table", e);
            }
        }

        LOG.debug("Table creation complete, created tables: {}");

        return tableIds;
    }

    @Override
    public void removeTables(final Collection<BigDecimal> tableIds) {
        LOG.debug("Removing tables: {}", tableIds);

        if (tableIds == null || tableIds.size() == 0) {
            return;
        }

        for (final BigDecimal tableId : tableIds) {
            LOG.debug("Table ID {}: requesting unload", tableId);

            internalTableService.unload(tableId);
        }
    }

    public void requestClosing(final Collection<BigDecimal> tableIds) {
        LOG.debug("Closing tables: {}", tableIds);
        if (tableIds == null || tableIds.size() == 0) {
            return;
        }

        for (final BigDecimal tableId : tableIds) {
            LOG.debug("Table ID {}: requesting close", tableId);
            internalTableService.closeTable(tableId);
        }
    }

    public void reopenAndStartNewGame(final BigDecimal tableId,
                                      final PlayerGroup playerGroup,
                                      final BigDecimal variationTemplateId,
                                      final String clientId) {
        LOG.debug("Reopening and starting new game: table {}, variation: {}, client: {}, players: {}",
                tableId, variationTemplateId, clientId, playerGroup);

        notNull(tableId, "Table ID may not be null");
        notNull(variationTemplateId, "Variation Template ID may not be null");
        notNull(clientId, "Client ID may not be null");

        internalTableService.forceNewGame(tableId, playerGroup.asPlayerInformationCollection(),
                variationTemplateId, clientId, playerGroup.asAccountIdList());
    }

    public int getOpenTableCount(final Collection<BigDecimal> tableIds) {
        if (tableIds == null) {
            return 0;
        }

        return tableGlobalRepository.countOpenTables(tableIds);
    }

    @Override
    public Client findClientById(final String clientId) {
        return clientRepository.findById(clientId);
    }

    @Override
    public Set<PlayerAtTableInformation> getActivePlayers(final Collection<BigDecimal> tableIds) {
        if (tableIds == null) {
            return Collections.emptySet();
        }

        return executor.mapReduce(new FindActivePlayersTask(tableIds));
    }

    @AutowireTask
    public static class FindActivePlayersTask implements DistributedTask<HashSet<PlayerAtTableInformation>, Set<PlayerAtTableInformation>> {
        private static final long serialVersionUID = -6577499756667538251L;

        @Resource(name = "tableRepository")
        private transient TableRepository tableRepository;

        private final Collection<BigDecimal> tableIds;

        public FindActivePlayersTask(final Collection<BigDecimal> tableIds) {
            this.tableIds = tableIds;
        }

        @Override
        public HashSet<PlayerAtTableInformation> execute() throws Exception {
            if (tableIds == null || tableIds.isEmpty()) {
                return new HashSet<>();
            }

            return new HashSet<>(tableRepository.findLocalActivePlayers(tableIds));
        }

        @Override
        public Set<PlayerAtTableInformation> reduce(final List<AsyncResult<HashSet<PlayerAtTableInformation>>> asyncResults) throws Exception {
            final Set<PlayerAtTableInformation> activePlayers = new HashSet<>();
            for (AsyncResult<HashSet<PlayerAtTableInformation>> activePlayerResult : asyncResults) {
                if (activePlayerResult.getException() == null) {
                    if (activePlayerResult.getResult() != null) {
                        activePlayers.addAll(activePlayerResult.getResult());
                    }
                } else {
                    LOG.error("Active player result retrieval failed", activePlayerResult.getException());
                }
            }
            return activePlayers;
        }
    }
}
