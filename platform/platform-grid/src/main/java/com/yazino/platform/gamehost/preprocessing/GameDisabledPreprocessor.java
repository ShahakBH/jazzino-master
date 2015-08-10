package com.yazino.platform.gamehost.preprocessing;

import com.yazino.platform.messaging.destination.DestinationFactory;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.messaging.host.MessageHostDocument;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.repository.table.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.yazino.game.api.Command;
import com.yazino.game.api.GameRules;
import com.yazino.game.api.ParameterisedMessage;
import com.yazino.game.api.ScheduledEvent;

import java.math.BigDecimal;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

public class GameDisabledPreprocessor implements CommandPreprocessor, EventPreprocessor {
    private static final Logger LOG = LoggerFactory.getLogger(GameDisabledPreprocessor.class);

    private final DestinationFactory destinationFactory;
    private final GameRepository gameRepository;

    @Autowired
    public GameDisabledPreprocessor(final GameRepository gameRepository,
                                    final DestinationFactory destinationFactory) {
        notNull(gameRepository, "Game Repository may not be null");
        notNull(destinationFactory, "Destination Factory may not be null");

        this.gameRepository = gameRepository;
        this.destinationFactory = destinationFactory;
    }

    public boolean preprocess(final ScheduledEvent event,
                              final Table table) {
        return isGameEnabled(table)
                || !canBeDisabled(gameRepository.getGameRules(table.getGameTypeId()), table)
                || event.isNoProcessingEvent();
    }

    public boolean preProcess(final GameRules gameRules,
                              final Command command,
                              final BigDecimal playerBalance,
                              final Table table,
                              final String auditLabel,
                              final List<HostDocument> documentsToSend) {
        final boolean gameEnabled = isGameEnabled(table)
                || (!canBeDisabled(gameRules, table) && gameRules.isAPlayer(table.getCurrentGame(), command.getPlayer()));
        if (!gameEnabled) {
            documentsToSend.add(new MessageHostDocument(table.getGameId(), command.getUuid(), table.getTableId(),
                    new ParameterisedMessage("This game has been disabled"),
                    destinationFactory.player(command.getPlayer().getId())));
        }
        return gameEnabled;

    }

    private boolean isGameEnabled(final Table table) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("game repository = {}", gameRepository.getClass().getName());
        }
        if (gameRepository.isGameAvailable(table.getGameTypeId())) {
            return true;
        }
        LOG.info("Table {}: game has been marked as inactive", table.getTableId());
        return false;
    }

    private boolean canBeDisabled(final GameRules gameRules, final Table table) {
        return table.getCurrentGame() == null || gameRules.canBeClosed(table.getCurrentGame());
    }
}
