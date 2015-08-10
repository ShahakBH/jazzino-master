package com.yazino.platform.service.table;

import com.yazino.game.api.GameRules;
import com.yazino.game.api.GameType;
import com.yazino.game.api.PlayerAtTableInformation;
import com.yazino.platform.model.table.Client;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.persistence.SequenceGenerator;
import com.yazino.platform.repository.table.ClientRepository;
import com.yazino.platform.repository.table.GameRepository;
import com.yazino.platform.repository.table.GameVariationRepository;
import com.yazino.platform.repository.table.TableRepository;
import com.yazino.platform.table.*;
import org.apache.commons.lang3.StringUtils;
import org.openspaces.remoting.Routing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.yazino.platform.model.table.TableControlMessageType.CLOSE;
import static com.yazino.platform.model.table.TableControlMessageType.UNLOAD;
import static org.apache.commons.lang3.Validate.notNull;

@Service
public class InternalTableService {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceRemotingTableService.class);

    private static final MessageFormat DEFAULT_TABLE_NAME = new MessageFormat("{0} {1,number,#}");
    private static final String DEFAULT_NAME_PREFIX = "Table";

    private final SequenceGenerator sequenceGenerator;
    private final ClientRepository clientRepository;
    private final TableRepository tableGlobalRepository;
    private final GameRepository gameRepository;
    private final GameVariationRepository gameTemplateRepository;

    @Autowired
    public InternalTableService(final SequenceGenerator sequenceGenerator,
                                final ClientRepository clientRepository,
                                final TableRepository tableGlobalRepository,
                                final GameRepository gameRepository,
                                final GameVariationRepository gameTemplateRepository) {

        notNull(sequenceGenerator, "sequenceGenerator may not be null");
        notNull(clientRepository, "clientRepository may not be null");
        notNull(tableGlobalRepository, "tableGlobalRepository may not be null");
        notNull(gameRepository, "gameRepository may not be null");
        notNull(gameTemplateRepository, "gameTemplateRepository may not be null");

        this.sequenceGenerator = sequenceGenerator;
        this.clientRepository = clientRepository;
        this.tableGlobalRepository = tableGlobalRepository;
        this.gameRepository = gameRepository;
        this.gameTemplateRepository = gameTemplateRepository;
    }

    public TableSummary createPublicTable(final String gameType,
                                          final String gameVariationName,
                                          final String clientId,
                                          final String tableName,
                                          final Set<String> tags)
            throws TableException {
        notNull(gameType, "gameTypeId may not be null");

        final GameVariation variation = variationFor(gameVariationName, gameType);
        return createTable(gameType, variation, clientId, tableName, null, true, true, tags);
    }

    public TableSummary createPublicTable(final String gameType,
                                          final String tableName, final Set<String> tags)
            throws TableException {
        notNull(gameType, "gameTypeId may not be null");

        return createTable(gameType, variationFor(null, gameType), null, tableName, null, true, true, tags);
    }


    public BigDecimal createTournamentTable(final String gameType,
                                            final BigDecimal gameVariationId,
                                            final String clientId,
                                            final String tableName)
            throws TableException {
        notNull(gameType, "gameTypeId may not be null");
        notNull(gameVariationId, "gameVariationId may not be null");
        notNull(clientId, "clientId may not be null");

        final GameVariation variation = getGameVariation(gameVariationId);
        return createTable(gameType, variation, clientId, tableName, null, false, false, null).getId();
    }


    public TableSummary createPrivateTableForPlayer(final String gameType,
                                                    final String gameVariationName,
                                                    final String clientId,
                                                    final String tableName,
                                                    final BigDecimal owningPlayerId)
            throws TableException {
        notNull(gameType, "gameTypeId may not be null");
        notNull(gameVariationName, "gameVariationName may not be null");
        notNull(clientId, "clientId may not be null");
        notNull(tableName, "tableName may not be null");
        notNull(owningPlayerId, "owningPlayerId may not be null");

        final GameVariation variation = variationFor(gameVariationName, gameType);
        return createTable(gameType, variation, clientId, tableName, owningPlayerId, true, false, null);
    }

    public void closeTable(@Routing final BigDecimal tableId) {
        notNull(tableId, "tableId may not be null");

        tableGlobalRepository.sendControlMessage(tableId, CLOSE);
    }

    public void unload(final BigDecimal tableId) {
        notNull(tableId, "tableId may not be null");

        tableGlobalRepository.sendControlMessage(tableId, UNLOAD);
    }

    public void forceNewGame(final BigDecimal tableId,
                             final Collection<PlayerAtTableInformation> playersAtTable,
                             final BigDecimal variationTemplateId,
                             final String clientId,
                             final Map<BigDecimal, BigDecimal> accountIds) {
        notNull(tableId, "tableId may not be null");

        tableGlobalRepository.forceNewGame(tableId, playersAtTable, variationTemplateId, clientId, accountIds);
    }

    private TableSummary createTable(final String gameTypeId,
                                     final GameVariation gameVariation,
                                     final String clientId,
                                     final String desiredTableName,
                                     final BigDecimal owningPlayerId,
                                     final boolean openTable,
                                     final boolean publicTable,
                                     final Set<String> tags)
            throws TableException {
        LOG.debug("Creating table: gameTypeId={}; variation={}; "
                        + "clientId={}; partnerId={}; tableName={}; owningPlayerId={}; open={}; public={}; tags={}",
                gameTypeId, gameVariation, clientId, desiredTableName,
                owningPlayerId, openTable, publicTable, tags
        );

        try {
            final Client client = clientFor(clientId, gameTypeId);

            ensureClientMatchesGameType(client, gameTypeId);
            ensureNoMatchingTableExistsForPlayer(gameTypeId, owningPlayerId);

            ensureGameTypeIsAvailable(gameTypeId);
            final GameType gameType = gameRepository.getGameTypeFor(gameTypeId);
            if (gameType == null) {
                throw new IllegalArgumentException("Invalid game type: " + gameType);
            }

            final BigDecimal tableId = sequenceGenerator.next();

            final Table table = new Table(gameType, gameVariation.getId(),
                    client.getClientId(), publicTable);
            table.setTableId(tableId);
            table.setTableName(generateTableNameIfRequired(gameVariation.getName(), desiredTableName, tableId));
            table.setFull(false);
            table.setClient(client);
            table.setOwnerId(owningPlayerId);
            table.setTags(tags);

            if (openTable) {
                table.setTableStatus(TableStatus.open);
            } else {
                table.setTableStatus(TableStatus.closed);
            }

            table.setTemplateName(gameVariation.getName());
            table.setVariationProperties(new HashMap<>(gameVariation.getProperties()));

            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Created table: tableId=%s; gameTypeId=%s; template=%s; "
                                + "clientId=%s; tableName=%s; owningPlayerId=%s; open=%s; public=%s",
                        tableId, gameTypeId, gameVariation, client.getClientId(), desiredTableName,
                        owningPlayerId, openTable, publicTable
                ));
            }

            tableGlobalRepository.save(table);

            return summaryOf(table);

        } catch (TableException e) {
            throw e;

        } catch (Exception e) {
            LOG.error("An error occurred when creating table", e);
            throw new TableException(TableOperationResult.FAILURE);
        }
    }

    private String generateTableNameIfRequired(final String templateName,
                                               final String desiredTableName,
                                               final BigDecimal tableId) {
        String actualTableName = desiredTableName;
        if (StringUtils.isBlank(actualTableName)) {
            String namePrefix = templateName;
            if (namePrefix == null) {
                namePrefix = DEFAULT_NAME_PREFIX;
            }
            actualTableName = DEFAULT_TABLE_NAME.format(new Object[]{namePrefix, tableId});
        }
        return actualTableName;
    }

    private GameVariation variationFor(final String templateName,
                                       final String gameType)
            throws TableException {
        if (templateName == null) {
            return firstTemplateFor(gameType);
        }

        final BigDecimal templateId = gameTemplateRepository.getIdForName(templateName, gameType);
        if (templateId == null) {
            LOG.warn("Template {} for game type {} does not exist", templateName, gameType);
            throw new TableException(TableOperationResult.UNKNOWN_TEMPLATE);
        }
        return getGameVariation(templateId);
    }

    private GameVariation firstTemplateFor(final String gameType) throws TableException {
        final Set<GameVariation> gameVariations = gameTemplateRepository.variationsFor(gameType);
        if (gameVariations != null && !gameVariations.isEmpty()) {
            return gameVariations.iterator().next();
        }
        LOG.error("No templates for game type {} exist", gameType);
        throw new TableException(TableOperationResult.UNKNOWN_TEMPLATE);
    }

    private Client clientFor(final String clientId, final String gameTypeId) throws TableException {
        if (clientId == null) {
            return getFirstClientFor(gameTypeId);
        }

        final Client client = clientRepository.findById(clientId);
        if (client == null) {
            return clientIfOnlyChoiceFor(gameTypeId, clientId);
        }
        return client;
    }

    private Client clientIfOnlyChoiceFor(final String gameTypeId, final String clientId) throws TableException {
        final Client[] allClients = clientRepository.findAll(gameTypeId);
        if (allClients != null && allClients.length == 1) {
            LOG.warn("Invalid client ID received: {}; defaulting to only available client: {}",
                    clientId, allClients[0].getClientId());
            return allClients[0];
        } else {
            LOG.warn("Client ID {} does not exist and multiple clients are available; aborting", clientId);
            throw new TableException(TableOperationResult.UNKNOWN_CLIENT);
        }
    }

    private Client getFirstClientFor(final String gameTypeId) throws TableException {
        final Client[] allClients = clientRepository.findAll(gameTypeId);
        if (allClients != null && allClients.length > 0) {
            return allClients[0];
        }
        LOG.error("Game type does not have any clients: {}", gameTypeId);
        throw new TableException(TableOperationResult.UNKNOWN_CLIENT);
    }

    GameVariation getGameVariation(@Routing final BigDecimal gameVariationId) {
        notNull(gameVariationId, "gameVariationId may not be null");

        return gameTemplateRepository.findById(gameVariationId);
    }

    private void ensureGameTypeIsAvailable(final String gameType) throws TableException {
        if (!gameRepository.isGameAvailable(gameType)) {
            throw new TableException(TableOperationResult.GAME_TYPE_UNAVAILABLE);
        }
    }

    private void ensureNoMatchingTableExistsForPlayer(final String gameType,
                                                      final BigDecimal owningPlayerId) throws TableException {
        if (owningPlayerId != null) {
            final TableSummary tableSummary
                    = tableGlobalRepository.findTableByGameTypeAndPlayer(gameType, owningPlayerId);

            if (tableSummary != null) {
                throw new TableException(TableOperationResult.TABLE_ALREADY_EXISTS_FOR_GAMETYPE);
            }
        }
    }

    private void ensureClientMatchesGameType(final Client client,
                                             final String gameType)
            throws TableException {
        notNull(client, "client may not be null");
        notNull(client.getGameType(), "client.gameTypeId may not be null");
        notNull(gameType, "gameTypeId may not be null");

        if (!client.getGameType().equals(gameType)) {
            final TableException tableException = new TableException(TableOperationResult.INVALID_CLIENT_FOR_GAMETYPE);
            LOG.error("Table creation request received for gameTypeId " + gameType
                    + " with invalid client " + client.getClientId(), tableException);
            throw tableException;
        }
    }

    private TableSummary summaryOf(final Table table) {
        final GameRules gameRules = gameRepository.getGameRules(table.getGameTypeId());
        return new TableSummary(table.getTableId(),
                table.getTableName(),
                table.getTableStatus(),
                table.getGameTypeId(),
                table.getGameType(),
                table.getOwnerId(),
                table.getClient().getClientId(),
                table.getClient().getClientFile(),
                table.getTemplateName(),
                table.getMonitoringMessage(),
                table.playerIds(gameRules),
                table.getTags());
    }

}
