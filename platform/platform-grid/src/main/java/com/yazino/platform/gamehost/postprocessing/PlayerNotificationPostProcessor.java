package com.yazino.platform.gamehost.postprocessing;

import com.yazino.game.api.*;
import com.yazino.platform.messaging.DocumentContext;
import com.yazino.platform.messaging.DocumentType;
import com.yazino.platform.messaging.ObservableDocumentContext;
import com.yazino.platform.messaging.destination.DestinationFactory;
import com.yazino.platform.messaging.host.GameStatusHostDocument;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.repository.table.GameRepository;
import com.yazino.platform.table.PlayerInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

public class PlayerNotificationPostProcessor implements Postprocessor {
    private static final Logger LOG = LoggerFactory.getLogger(PlayerNotificationPostProcessor.class);

    private final GameRepository gameRepository;
    private final DestinationFactory destinationFactory;

    @Autowired
    public PlayerNotificationPostProcessor(final DestinationFactory destinationFactory,
                                           final GameRepository gameRepository) {
        notNull(destinationFactory, "destinationFactory must not be null");
        notNull(gameRepository, "gameRepository must not be null");

        this.gameRepository = gameRepository;
        this.destinationFactory = destinationFactory;
    }

    public void postProcess(final ExecutionResult executionResult,
                            final Command command,
                            final Table table,
                            final String auditLabel,
                            final List<HostDocument> documentsToSend,
                            final BigDecimal initiatingPlayerId) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Table {}: entering postprocess", table.getTableId());
        }

        if (executionResult == null || executionResult.getGameStatus() == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Table {}: executionresult is null or new game status, returning", table.getTableId());
            }
            return;
        }
        if (command == null && executionResult.getGameStatus().equals(table.getCurrentGame())) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Table %s: gamestatus is unchanged and is a command, returning", table.getTableId());
            }
            return;
        }

        final GameRules gameRules = gameRepository.getGameRules(table.getGameTypeId());

        final Set<BigDecimal> playersInCurrentGame = new HashSet<>(executionResult.playerIds());
        playersInCurrentGame.addAll(table.playerIds(gameRules));

        if (playersInCurrentGame.isEmpty()) {
            LOG.debug("Table {}: no players at table, returning", table.getTableId());
            return;
        }

        final DocumentContext documentContext = new DocumentContext.Builder(DocumentType.GAME_STATUS)
                .withTableId(table.getTableId())
                .withGameId(table.getGameId())
                .withGameType(table.getGameTypeId())
                .withIncrement(table.incrementDefaultedToOne() + 1)
                .withIncrementOfGameStart(table.incrementOfGameStartDefaultedToOne())
                .withCommand(command)
                .withPlayerInformation(table.allPlayersAtTable())
                .withPreviousGame(table.getLastGame())
                .withPlayersInPreviousGame(table.playerIds(gameRules))
                .withExecutionResult(executionResult)
                .build();

        sendDocuments(gameRules, documentContext, documentsToSend, playersInCurrentGame,
                initiatingPlayerId, table.incrementDefaultedToOne());
    }

    private void sendDocuments(final GameRules gameRules,
                               final DocumentContext documentContext,
                               final List<HostDocument> documentsToSend,
                               final Set<BigDecimal> playersInCurrentGame,
                               final BigDecimal initiatingPlayerId,
                               final long currentIncrement) {
        for (PlayerInformation player : documentContext.getPlayers()) {
            final BigDecimal playerId = player.getPlayerId();
            if (!playersInCurrentGame.contains(playerId)) {
                continue;
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Table {}: notifying player {}", documentContext.getTableId(), playerId);
            }

            final BigDecimal playerBalance = player.getCachedBalance();

            final boolean skipIfPossible = !playerId.equals(initiatingPlayerId);

            final ObservableContext playerContext = new ObservableContext(
                    player.toGamePlayer(), playerBalance, skipIfPossible, currentIncrement);

            final ObservableStatus status = gameRules.getObservableStatus(documentContext.getExecutionResult().getGameStatus(), playerContext);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Table {}: observable status for player {}: {}", documentContext.getTableId(), playerId, status);
            }

            if (status != null) {
                final ObservableContext previousGameContext = new ObservableContext(
                        player.toGamePlayer(), playerBalance);

                final List<ObservableChange> lastGameChanges = buildLastGameChanges(
                        documentContext.getPreviousGame(), previousGameContext, gameRules);
                final ObservableDocumentContext context = new ObservableDocumentContext.Builder(
                        documentContext.getTableId(), documentContext.getGameId(), status,
                        documentContext.getIncrement(), documentContext.getIncrementOfGameStart())
                        .withLastGameChanges(lastGameChanges)
                        .withAcknowlegedIncrement(player.getAcknowledgedIncrement())
                        .withIsAPlayer(gameRules.isAPlayer(documentContext.getExecutionResult().getGameStatus(), player.toGamePlayer()))
                        .withPlayerIds(playersInCurrentGame)
                        .withPlayerBalance(playerBalance)
                        .withCommand(documentContext.getCommand())
                        .build();

                if (context.getMergedGameChanges().size() > 0
                        || isInitiatingPlayer(documentContext.getCommand(), player)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Table {}: dispatching game status document to player {}",
                                documentContext.getTableId(), playerId);
                    }

                    documentsToSend.add(new GameStatusHostDocument(context,
                            destinationFactory.player(player.getPlayerId())));

                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Table {}: no changes to send and player {} is not initiator, not sending document",
                                documentContext.getTableId(), playerId);
                    }
                }
            }
        }
    }

    private boolean isInitiatingPlayer(final Command command, final PlayerInformation player) {
        return (command != null && player != null
                && command.getPlayer() != null
                && player.getPlayerId().equals(command.getPlayer().getId()));
    }

    private List<ObservableChange> buildLastGameChanges(final GameStatus previousGame,
                                                        final ObservableContext context,
                                                        final GameRules gameRules) {
        if (previousGame == null || !gameRules.isAPlayer(previousGame, context.getPlayer())) {
            return Collections.emptyList();
        }
        return gameRules.getObservableStatus(previousGame, context).getObservableChanges();
    }
}
