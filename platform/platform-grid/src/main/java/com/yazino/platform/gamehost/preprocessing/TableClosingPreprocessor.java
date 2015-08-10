package com.yazino.platform.gamehost.preprocessing;

import com.yazino.game.api.*;
import com.yazino.platform.messaging.ObservableDocumentContext;
import com.yazino.platform.messaging.destination.DestinationFactory;
import com.yazino.platform.messaging.host.ErrorHostDocument;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.table.TableStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

public class TableClosingPreprocessor implements CommandPreprocessor {
    private static final Logger LOG = LoggerFactory.getLogger(TableClosingPreprocessor.class);

    private final DestinationFactory destinationFactory;

    @Autowired(required = true)
    public TableClosingPreprocessor(final DestinationFactory destinationFactory) {
        notNull(destinationFactory, "destinationFactory may not be null");

        this.destinationFactory = destinationFactory;
    }

    public boolean preProcess(final GameRules gameRules,
                              final Command command,
                              final BigDecimal balance,
                              final Table table,
                              final String auditLabel,
                              final List<HostDocument> documentsToSend) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Table {}: entering preprocess", table.getTableId());
        }
        final GameStatus currentGame = table.getCurrentGame();

        if (table.readyToBeClosed(gameRules)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Table {}: ready to be closed. Rejecting command", table.getTableId());
            }
            return false;
        }

        if (table.getTableStatus() == TableStatus.closing
                && currentGame != null
                && !gameRules.isAPlayer(currentGame, command.getPlayer())) {
            final GamePlayer player = command.getPlayer();
            final BigDecimal playerId = player.getId();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Table {}: table {} and player ({}) not a player: sending doc and stopping execution",
                        table.getTableId(), table.getTableStatus(), playerId);
            }
            final ObservableStatus observableStatus = table.buildObservableStatus(gameRules, player, balance);

            final ObservableDocumentContext context = new ObservableDocumentContext.Builder(
                    table.getTableId(), table.getGameId(), observableStatus,
                    table.incrementDefaultedToOne(), table.incrementOfGameStartDefaultedToOne())
                    .withCommand(command)
                    .withIsAPlayer(false)
                    .withPlayerIds(table.playerIds(gameRules))
                    .withPlayerBalance(balance)
                    .withMessage(new ParameterisedMessage("this table is closing"))
                    .build();
            documentsToSend.add(new ErrorHostDocument(context, destinationFactory.player(playerId)));

            return false;
        }
        return true;
    }
}
