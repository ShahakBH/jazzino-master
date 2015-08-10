package com.yazino.platform.gamehost.preprocessing;

import com.yazino.game.api.*;
import com.yazino.platform.messaging.ObservableDocumentContext;
import com.yazino.platform.messaging.destination.DestinationFactory;
import com.yazino.platform.messaging.host.ErrorHostDocument;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.messaging.host.InitialGameStatusHostDocument;
import com.yazino.platform.model.table.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

public class TableClosedPreprocessor implements CommandPreprocessor, EventPreprocessor {
    private static final Logger LOG = LoggerFactory.getLogger(TableClosedPreprocessor.class);

    private final DestinationFactory destinationFactory;

    @Autowired(required = true)
    public TableClosedPreprocessor(final DestinationFactory destinationFactory) {
        notNull(destinationFactory, "destinationFactory may not be null");

        this.destinationFactory = destinationFactory;
    }

    public boolean preProcess(final GameRules gameRules,
                              final Command command,
                              final BigDecimal playerBalance,
                              final Table table,
                              final String auditLabel,
                              final List<HostDocument> documentsToSend) {
        if (!table.isClosed()) {
            return true;
        }
        final GamePlayer player = command.getPlayer();
        final ObservableStatus observableStatus = table.buildObservableStatus(gameRules, player, playerBalance);

        if (LOG.isDebugEnabled()) {
            LOG.info("Table {}: table is closed, sending document to player {}", table.getTableId(), player.getId());
        }
        final ObservableDocumentContext context = new ObservableDocumentContext.Builder(
                table.getTableId(), table.getGameId(),
                observableStatus, table.incrementDefaultedToOne(), table.incrementOfGameStartDefaultedToOne())
                .withCommand(command)
                .withIsAPlayer(false)
                .withPlayerBalance(playerBalance)
                .withMessage(new ParameterisedMessage("the table is closed"))
                .build();

        if (Command.CommandType.InitialGetStatus == command.getCommandType()) {
            documentsToSend.add(new InitialGameStatusHostDocument(context, destinationFactory.player(player.getId())));
        } else {
            documentsToSend.add(new ErrorHostDocument(context, destinationFactory.player(player.getId())));
        }

        return false;
    }

    public boolean preprocess(final ScheduledEvent event,
                              final Table table) {
        final boolean notClosedOrNop = !table.isClosed() || event.isNoProcessingEvent();
        if (LOG.isDebugEnabled()) {
            LOG.info("Table {}: table is closed? {}, nop event? {}: continuing? {}",
                    table.getTableId(), table.isClosed(), event.isNoProcessingEvent(), notClosedOrNop);
        }
        return notClosedOrNop;
    }
}
