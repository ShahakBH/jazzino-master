package com.yazino.platform.gamehost.postprocessing;

import com.yazino.platform.messaging.destination.DestinationFactory;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.messaging.host.MessageHostDocument;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.repository.table.GameRepository;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.yazino.game.api.Command;
import com.yazino.game.api.ExecutionResult;
import com.yazino.game.api.GameRules;
import com.yazino.game.api.ParameterisedMessage;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

public class GameDisabledPostprocessor implements Postprocessor {
    private static final Logger LOG = LoggerFactory.getLogger(GameDisabledPostprocessor.class);

    private final GameRepository gameRepository;
    private final DestinationFactory destinationFactory;

    @Autowired
    public GameDisabledPostprocessor(final GameRepository gameRepository,
                                     final DestinationFactory destinationFactory) {
        notNull(gameRepository, "Game Repository may not be null");
        notNull(destinationFactory, "Destination Factory may not be null");

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
        if (executionResult == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Table {}: No execution result, returning", table.getTableId());
            }
            return;
        }
        final GameRules gameRules = gameRepository.getGameRules(table.getGameTypeId());
        final boolean canBeDisabled = executionResult.getGameStatus() == null || gameRules.canBeClosed(executionResult.getGameStatus());
        if (!canBeDisabled) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Table {}: Game {} is not complete, returning", table.getTableId(), table.getGameId());
            }
            return;
        }
        if (!gameRepository.isGameAvailable(table.getGameTypeId())) {
            final Set<BigDecimal> playersAtTable = table.playerIds(gameRules);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Table {}: Game is disabled, sending message to {}", table.getTableId(),
                        ReflectionToStringBuilder.reflectionToString(playersAtTable));
            }

            if (!playersAtTable.isEmpty()) {
                documentsToSend.add(new MessageHostDocument(table.getGameId(), null, table.getTableId(),
                        new ParameterisedMessage("This game has been disabled"),
                        destinationFactory.players(playersAtTable)));
            }

        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Table {}: Game is not disabled, returning", table.getTableId());
            }
        }
    }
}
