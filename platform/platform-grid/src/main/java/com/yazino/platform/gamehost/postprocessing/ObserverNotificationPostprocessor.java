package com.yazino.platform.gamehost.postprocessing;

import com.yazino.game.api.*;
import com.yazino.platform.messaging.ObservableDocumentContext;
import com.yazino.platform.messaging.destination.DestinationFactory;
import com.yazino.platform.messaging.host.GameStatusHostDocument;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.repository.table.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

public class ObserverNotificationPostprocessor implements Postprocessor {
    private static final Logger LOG = LoggerFactory.getLogger(ObserverNotificationPostprocessor.class);

    private final GameRepository gameRepository;
    private final DestinationFactory destinationFactory;

    @Autowired
    public ObserverNotificationPostprocessor(final GameRepository gameRepository,
                                             final DestinationFactory destinationFactory) {
        notNull(gameRepository, "gameRepository may not be null");
        notNull(destinationFactory, "destinationFactory may not be null");

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
                LOG.debug("Table {}: executionresult is null, returning", table.getTableId());
            }
            return;
        }
        final GameStatus newGameStatus = executionResult.getGameStatus();
        if (newGameStatus == null || newGameStatus.equals(table.getCurrentGame())) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Table {}: new game status is null or unchanged, returning", table.getTableId());
            }
            return;
        }

        final GameRules gameRules = gameRepository.getGameRules(table.getGameTypeId());
        final ObservableStatus status = gameRules.getObservableStatus(executionResult.getGameStatus(), new ObservableContext(null, null));
        final long newAcknowlegedIncrement;
        if (table.getIncrementOfGameStart() == null) {
            newAcknowlegedIncrement = (long) 0 - 1;
        } else {
            newAcknowlegedIncrement = table.getIncrementOfGameStart() - 1;
        }
        final ObservableDocumentContext context = new ObservableDocumentContext.Builder(
                table.getTableId(), table.getGameId(), status, table.incrementDefaultedToOne() + 1,
                table.incrementOfGameStartDefaultedToOne())
                .withCommand(command)
                .withPlayerIds(executionResult.playerIds())
                .withAcknowlegedIncrement(newAcknowlegedIncrement)
                .build();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Table {}: preparing observer document", table.getTableId());
        }
        documentsToSend.add(new GameStatusHostDocument(context, destinationFactory.observers()));

    }
}
