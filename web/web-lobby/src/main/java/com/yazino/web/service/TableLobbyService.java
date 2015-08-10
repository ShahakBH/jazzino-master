package com.yazino.web.service;

import com.google.common.collect.Sets;
import com.yazino.platform.table.*;
import com.yazino.web.domain.TableReservationConfiguration;
import com.yazino.web.domain.social.PlayerInformation;
import com.yazino.web.domain.social.PlayerInformationType;
import com.yazino.web.domain.social.PlayersInformationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang3.Validate.notNull;

@Service("tableLobbyService")
public class TableLobbyService {
    private static final Logger LOG = LoggerFactory.getLogger(TableLobbyService.class);

    private final TableSearchService tableSearchService;
    private final TableService tableService;
    private final TableReservationConfiguration tableReservationConfiguration;

    private final PlayersInformationService playersInformationService;
    private final Integer slotsSplit;
    private final String gameVariationToSplit;

    @Autowired
    public TableLobbyService(final TableSearchService tableSearchService,
                             final TableService tableService,
                             final TableReservationConfiguration tableReservationConfiguration,
                             final PlayersInformationService playersInformationService,
                             @Value("${strata.tables.slots.split}") final Integer slotsSplit,
                             @Value("${strata.tables.variation.split}") final String gameVariationToSplit) {
        notNull(tableSearchService, "tableSearchService may not be null");
        notNull(tableService, "tableService may not be null");
        notNull(tableReservationConfiguration, "tableReservationConfiguration may not be null");
        notNull(playersInformationService, "playersInformationService may not be null");

        this.gameVariationToSplit = gameVariationToSplit; //"slots low";
        this.slotsSplit = slotsSplit;
        this.tableSearchService = tableSearchService;
        this.playersInformationService = playersInformationService;
        this.tableService = tableService;
        this.tableReservationConfiguration = tableReservationConfiguration;
    }

    public TableSummary findOrCreateTableByGameTypeAndVariation(final String gameType,
                                                                final String gameVariationTemplateName,
                                                                final String clientId,
                                                                final BigDecimal playerId,
                                                                final Set<String> tags,
                                                                final BigDecimal... excludeTableIds)
            throws TableException {
        notNull(gameType, "Game Type may not be null");
        notNull(playerId, "Player ID may not be null");

        LOG.debug("Find/creating table with game type: {}, template: {}, client: {} for player: {}",
                gameType, gameVariationTemplateName, clientId, playerId);

        Set<String> updatedTags = Sets.newHashSet(tags);
        if (gameTypeSplitByLevel(gameVariationTemplateName)) {
            if (getPlayerLevel(gameType, playerId) < slotsSplit) {
                updatedTags.add("lowLevel");
            } else {
                updatedTags.add("highLevel");
            }
        }

        return findOrCreateTable(gameType, gameVariationTemplateName, clientId, playerId, updatedTags, excludeTableIds);
    }

    private boolean gameTypeSplitByLevel(final String gameType) {
        return (gameVariationToSplit.equalsIgnoreCase(gameType));
    }

    private TableSummary findOrCreateTable(final String gameType,
                                           final String gameVariationTemplateName,
                                           final String clientId,
                                           final BigDecimal playerId,
                                           final Set<String> updatedTags,
                                           final BigDecimal... excludeTableIds) throws TableException {
        final TableSearchCriteria searchCriteria = new TableSearchCriteria(
                gameType, gameVariationTemplateName, clientId, updatedTags, excludeTableIds);
        final Collection<TableSearchResult> tables = tableSearchService.findTables(playerId, searchCriteria);
        if (tables.isEmpty()) {
            final TableSummary tableSummary = tableService.createPublicTable(
                    gameType, gameVariationTemplateName, clientId, null, updatedTags);
            LOG.debug("No existing tables were found, so created a new one [{}]", tableSummary);
            reserveIfPossible(gameType, playerId, tableSummary.getId());
            return tableSummary;

        } else {
            final BigDecimal tableId = tables.iterator().next().getTableId();
            LOG.debug("Existing table was found [{}]", tableId);
            reserveIfPossible(gameType, playerId, tableId);
            return tableService.findSummaryById(tableId);
        }
    }

    private Integer getPlayerLevel(final String gameType, final BigDecimal playerId) {
        final List<PlayerInformation> playerInfo = playersInformationService.retrieve(newArrayList(playerId),
                gameType,
                PlayerInformationType.LEVEL);
        if (playerInfo.size() > 0) {
            return (Integer) playerInfo.get(0).get(PlayerInformationType.LEVEL.name().toLowerCase());
        }
        return -1;
    }

    private void reserveIfPossible(final String gameType,
                                   final BigDecimal playerId,
                                   final BigDecimal tableId) {
        if (tableReservationConfiguration.supportsReservation(gameType)) {
            tableService.makeReservationAtTable(tableId, playerId);
        }
    }

    public TableSummary findOrCreateSimilarTable(final BigDecimal tableId,
                                                 final BigDecimal playerId)
            throws TableException {
        notNull(tableId, "Table ID may not be null");
        notNull(playerId, "Player ID may not be null");

        final TableSummary table = tableService.findSummaryById(tableId);
        if (table == null) {
            throw new IllegalArgumentException("Table ID is invalid: " + tableId);
        }

        String gameType = null;
        if (table.getGameType() != null) {
            gameType = table.getGameType().getId();
        }

        return findOrCreateTable(gameType,
                table.getTemplateName(), table.getClientId(), playerId,
                table.getTags(), tableId);
    }

    public TableSummary findPrivateTable(final BigDecimal playerId, String gameType, String templateName, String clientId) throws TableException {
        LOG.debug("Finding private table for player {}, gameType {} (template={}, client={)", playerId, gameType, templateName, clientId);
        final TableSummary table = tableService.findTableByGameTypeAndPlayerId(gameType, playerId);
        if (table != null) {
            LOG.debug("Found private table {}", table);
            return table;
        }
        LOG.debug("Private table could not be found. Creating...");
        return tableService.createPrivateTableForPlayer(gameType, templateName, clientId, "Private Table", playerId);
    }

    public boolean isGameTypeAvailable(final String gameType) {
        final Set<GameTypeInformation> gameTypes = tableService.getGameTypes();
        for (GameTypeInformation type : gameTypes) {
            if (type.getId().equals(gameType)) {
                return type.isAvailable();
            }
        }
        return false;
    }
}
