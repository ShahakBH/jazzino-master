package com.yazino.platform.gamehost.postprocessing;

import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.repository.table.GameRepository;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.yazino.game.api.Command;
import com.yazino.game.api.ExecutionResult;
import com.yazino.game.api.GameRules;
import com.yazino.game.api.ScheduledEvent;
import com.yazino.game.api.time.TimeSource;

import java.math.BigDecimal;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

public class GameCompletePostprocessor implements Postprocessor {
    private static final Logger LOG = LoggerFactory.getLogger(GameCompletePostprocessor.class);

    private final GameRepository gameRepository;
    private final TimeSource timeSource;

    @Autowired
    public GameCompletePostprocessor(final GameRepository gameRepository,
                                     final TimeSource timeSource) {
        notNull(timeSource, "timeSource may not be null");
        notNull(gameRepository, "gameRepository may not be null");

        this.timeSource = timeSource;
        this.gameRepository = gameRepository;
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
                LOG.debug("Table {}: No execution result or game status: returning", table.getTableId());
            }
            return;
        }
        final GameRules gameRules = gameRepository.getGameRules(table.getGameTypeId());
        if (gameRules.isComplete(executionResult.getGameStatus())) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Table {}: Game is complete, scheduling event", table.getTableId());
            }
            final ScheduledEvent evt = ScheduledEvent.noProcessingEvent(table.getGameId());

            if (LOG.isDebugEnabled()) {
                LOG.debug("Table {}: scheduling no processing event %s",
                        table.getTableId(), ReflectionToStringBuilder.reflectionToString(evt));
            }
            table.addEvent(timeSource, evt);
        }
    }
}
