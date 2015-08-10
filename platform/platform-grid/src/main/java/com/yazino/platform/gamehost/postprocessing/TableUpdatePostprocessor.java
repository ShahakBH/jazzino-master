package com.yazino.platform.gamehost.postprocessing;

import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.repository.table.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.yazino.game.api.Command;
import com.yazino.game.api.ExecutionResult;
import com.yazino.game.api.GameRules;
import com.yazino.game.api.GameStatus;
import com.yazino.game.api.time.TimeSource;

import java.math.BigDecimal;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

public class TableUpdatePostprocessor implements Postprocessor {
    private static final Logger LOG = LoggerFactory.getLogger(TableUpdatePostprocessor.class);

    private final GameRepository gameRepository;
    private final TimeSource timeSource;

    @Autowired
    public TableUpdatePostprocessor(final GameRepository gameRepository,
                                    final TimeSource timeSource) {
        notNull(gameRepository, "gameRepository may not be null");
        notNull(timeSource, "timeSource may not be null");

        this.gameRepository = gameRepository;
        this.timeSource = timeSource;
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
                LOG.debug("Table {}: No execution result: returning", table.getTableId());
            }
            return;
        }
        final GameRules gameRules = gameRepository.getGameRules(table.getGameTypeId());
        table.addEvents(timeSource, executionResult.getScheduledEvents());
        table.nextIncrement();
        final GameStatus updatedGameStatus = executionResult.getGameStatus();
        table.setAvailableForPlayersJoining(gameRules.isAvailableForPlayerJoining(executionResult.getGameStatus()));
        table.setCurrentGame(updatedGameStatus);
        table.updateFreeSeats(gameRules.getNumberOfSeatsTaken(executionResult.getGameStatus()));
    }
}
