package com.yazino.platform.gamehost.postprocessing;

import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.service.statistic.GameStatisticsPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.yazino.game.api.Command;
import com.yazino.game.api.ExecutionResult;

import java.math.BigDecimal;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

public class GameStatisticsPublishingPostProcessor implements Postprocessor {
    private static final Logger LOG = LoggerFactory.getLogger(GameStatisticsPublishingPostProcessor.class);

    private final GameStatisticsPublisher publisher;

    @Autowired
    public GameStatisticsPublishingPostProcessor(final GameStatisticsPublisher publisher) {
        notNull(publisher, "gameStatisticsPublisher is null");
        this.publisher = publisher;
    }

    @Override
    public void postProcess(final ExecutionResult executionResult,
                            final Command command,
                            final Table table,
                            final String auditLabel,
                            final List<HostDocument> documentsToSend,
                            final BigDecimal initiatingPlayerId) {
        LOG.debug("Table {}: entering postprocess", table.getTableId());
        if (executionResult == null) {
            return;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Table {}: publishing game statistics: {}",
                    table.getTableId(), executionResult.getGameStatistics());
        }
        publisher.publish(table.getTableId(), table.getGameTypeId(),
                table.getClientId(), executionResult.getGameStatistics());
    }
}
