package com.yazino.platform.gamehost.preprocessing;

import com.yazino.game.api.*;
import com.yazino.platform.messaging.ObservableDocumentContext;
import com.yazino.platform.messaging.destination.DestinationFactory;
import com.yazino.platform.messaging.host.GameStatusHostDocument;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.table.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

public class ObserverGetStatusPreprocessor implements CommandPreprocessor {
    private static final Logger LOG = LoggerFactory.getLogger(ObserverGetStatusPreprocessor.class);

    private final DestinationFactory destinationFactory;

    @Autowired
    public ObserverGetStatusPreprocessor(final DestinationFactory destinationFactory) {
        notNull(destinationFactory, "destinationFactory is null");

        this.destinationFactory = destinationFactory;
    }

    @SuppressWarnings("UnusedAssignment")
    @Override
    public boolean preProcess(final GameRules gameRules,
                              final Command command,
                              final BigDecimal playerBalance,
                              final Table table,
                              final String auditLabel,
                              final List<HostDocument> documentsToSend) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Table {}: entering preprocess", table.getTableId());
        }

        if (Command.CommandType.ObserverGetStatus != command.getCommandType() || table.getCurrentGame() == null) {
            return true;
        }

        Long fromIncrement = null;
        if (command.getArgs() != null && command.getArgs().length > 0) {
            try {
                fromIncrement = Long.parseLong(command.getArgs()[0]);
            } catch (Throwable t) {
                LOG.error("Error processing InitialGetStatus arg " + command.getArgs()[0]);
            }
        }

        final ObservableContext previousGameContext = new ObservableContext(null, null);

        final GamePlayer player = command.getPlayer();

        final ObservableStatus status = gameRules.getObservableStatus(table.getCurrentGame(), new ObservableContext(null, null));

        final long newAcknowlegedIncrement;
        if (fromIncrement == null) {
            newAcknowlegedIncrement = table.getIncrementOfGameStart() - 1;
        } else {
            newAcknowlegedIncrement = fromIncrement;
        }
        final ObservableDocumentContext context = new ObservableDocumentContext.Builder(
                table.getTableId(), table.getGameId(), status, table.incrementDefaultedToOne(),
                table.incrementOfGameStartDefaultedToOne())
                .withCommand(command)
                .withPlayerIds(table.playerIds(gameRules))
                .withLastGameChanges(buildLastGameChanges(gameRules, table.getLastGame(), previousGameContext))
                .withAcknowlegedIncrement(newAcknowlegedIncrement)
                .build();

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Table %s: preparing observer document", table.getTableId()));
        }

        documentsToSend.add(new GameStatusHostDocument(context, destinationFactory.player(player.getId())));
        return false;
    }

    private List<ObservableChange> buildLastGameChanges(final GameRules gameRules,
                                                        final GameStatus previousGame,
                                                        final ObservableContext context) {
        if (previousGame == null) {
            return Collections.emptyList();
        }
        return gameRules.getObservableStatus(previousGame, context).getObservableChanges();
    }
}
