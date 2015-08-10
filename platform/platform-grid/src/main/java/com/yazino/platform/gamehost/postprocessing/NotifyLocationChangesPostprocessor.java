package com.yazino.platform.gamehost.postprocessing;

import com.yazino.game.api.Command;
import com.yazino.game.api.ExecutionResult;
import com.yazino.game.api.GamePlayerWalletFactory;
import com.yazino.game.api.GameRules;
import com.yazino.platform.chat.ChatRequestArgument;
import com.yazino.platform.chat.ChatRequestType;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.chat.ChatChannel;
import com.yazino.platform.model.chat.GigaspaceChatRequest;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.repository.chat.ChatRepository;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.table.GameRepository;
import com.yazino.platform.service.community.LocationService;
import com.yazino.platform.session.Location;
import com.yazino.platform.session.LocationChangeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.*;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

public class NotifyLocationChangesPostprocessor implements Postprocessor, PlayerRemovalProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(NotifyLocationChangesPostprocessor.class);

    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;
    private final ChatRepository chatRepository;
    private final LocationService locationService;

    @Autowired
    public NotifyLocationChangesPostprocessor(final GameRepository gameRepository,
                                              final PlayerRepository playerRepository,
                                              final ChatRepository chatRepository,
                                              final LocationService locationService) {
        notNull(gameRepository, "gameRepository may not be null");
        notNull(playerRepository, "playerRepository may not be null");
        notNull(chatRepository, "chatRepository may not be null");
        notNull(locationService, "locationService may not be null");

        this.gameRepository = gameRepository;
        this.playerRepository = playerRepository;
        this.chatRepository = chatRepository;
        this.locationService = locationService;
    }

    public void postProcess(final ExecutionResult executionResult,
                            final Command command,
                            final Table table,
                            final String auditLabel,
                            final List<HostDocument> documentsToSend,
                            final BigDecimal initiatingPlayerId) {
        LOG.debug("Table {}: entering postprocess", table.getTableId());

        if (executionResult == null) {
            LOG.debug("Table {}: execution result is null, returning", table.getTableId());
            return;
        }

        final GameRules gameRules = gameRepository.getGameRules(table.getGameTypeId());
        final boolean gameDisabled = !gameRepository.isGameAvailable(table.getGameTypeId())
                && (executionResult.getGameStatus() == null || gameRules.canBeClosed(executionResult.getGameStatus()));
        if (table.isClosed() || table.readyToBeClosed(gameRules) || gameDisabled) {
            LOG.debug("Table {}: is closed or game is disabled, all players leaving on game id {}",
                    table.getTableId(), table.getGameId());
            notifyAllPlayersLeaving(gameRules, table);
            return;
        }

        notify(table, table.playerIdsToSessions(gameRules), executionResult.playerIdsToSessions());
    }

    @Override
    public void removeAllPlayers(final Table table,
                                 final GamePlayerWalletFactory gamePlayerWalletFactory) {
        final GameRules gameRules = gameRepository.getGameRules(table.getGameTypeId());
        notifyAllPlayersLeaving(gameRules, table);
    }

    private void notifyAllPlayersLeaving(final GameRules gameRules, final Table table) {
        final Map<BigDecimal, BigDecimal> playerIds = table.playerIdsToSessions(gameRules);
        LOG.debug("Table {}: sending leaving notifications to  {}", table.getTableId(), playerIds);
        notify(table, playerIds, Collections.<BigDecimal, BigDecimal>emptyMap());
    }

    private void notify(final Table table,
                        final Map<BigDecimal, BigDecimal> playersBeforeExecution,
                        final Map<BigDecimal, BigDecimal> playersAfterExecution) {
        LOG.debug("Table {}: sending notification changes; before = {}, after = {}",
                table.getTableId(), playersBeforeExecution, playersAfterExecution);

        if (playersBeforeExecution.keySet().equals(playersAfterExecution)) {
            LOG.debug("Table {}: no players leaving, returning", table.getTableId());
            return;
        }

        LOG.debug("Table {}: using community service to send notifications", table.getTableId());

        final Location location = new Location(table.getTableId().toString(),
                table.getTableName(), table.getGameTypeId(), table.getOwnerId(), table.resolveType());

        notifySetDifference(playersBeforeExecution, playersAfterExecution,
                LocationChangeType.ADD, location);
        notifySetDifference(playersAfterExecution, playersBeforeExecution,
                LocationChangeType.REMOVE, location);
    }

    private void notifySetDifference(final Map<BigDecimal, BigDecimal> startingPlayers,
                                     final Map<BigDecimal, BigDecimal> newPlayers,
                                     final LocationChangeType notificationType,
                                     final Location location) {
        final Set<BigDecimal> addedPlayers = new HashSet<>(newPlayers.keySet());
        addedPlayers.removeAll(startingPlayers.keySet());
        if (addedPlayers.isEmpty()) {
            return;
        }
        LOG.debug("addedPlayers are {}", addedPlayers);
        for (BigDecimal addedPlayerId : addedPlayers) {
            locationService.notify(addedPlayerId, newPlayers.get(addedPlayerId), notificationType, location);
            createChatRequest(addedPlayerId, notificationType, location);
        }
    }

    private void createChatRequest(final BigDecimal playerId,
                                   final LocationChangeType notificationType,
                                   final Location location) {
        final Player player = playerRepository.findById(playerId);
        if (player == null) {
            return;
        }

        final ChatRequestType chatRequestType = ChatRequestType.parse(notificationType);
        if (chatRequestType != null) {
            final String channelId = channelForLocation(location.getLocationId());
            final HashMap<ChatRequestArgument, String> args = new HashMap<>();
            args.put(ChatRequestArgument.NICKNAME, player.getName());
            args.put(ChatRequestArgument.PLAYER_ID, player.getPlayerId().toString());
            chatRepository.request(new GigaspaceChatRequest(chatRequestType,
                    player.getPlayerId(), channelId, location.getLocationId(), args));
        }
    }

    private String channelForLocation(final String locationId) {
        notBlank(locationId, "locationId may not be null/blank");

        final ChatChannel channel = chatRepository.getOrCreateForLocation(locationId);
        if (channel == null) {
            throw new IllegalStateException("Unable to fetch chat channel for location " + locationId);
        }
        return channel.getChannelId();
    }


}
