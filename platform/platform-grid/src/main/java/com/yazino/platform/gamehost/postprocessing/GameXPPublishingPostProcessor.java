package com.yazino.platform.gamehost.postprocessing;


import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.statistic.PlayerStatisticEvent;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.service.statistic.PlayerStatisticEventsPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import com.yazino.game.api.Command;
import com.yazino.game.api.ExecutionResult;
import com.yazino.game.api.statistic.GameXPEvents;
import com.yazino.game.api.statistic.StatisticEvent;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

public class GameXPPublishingPostProcessor implements Postprocessor {
    private final PlayerStatisticEventsPublisher playerStatisticEventsPublisher;

    @Autowired
    public GameXPPublishingPostProcessor(final PlayerStatisticEventsPublisher playerStatisticEventsPublisher) {
        notNull(playerStatisticEventsPublisher, "playerStatisticEventsPublisher is null");
        this.playerStatisticEventsPublisher = playerStatisticEventsPublisher;
    }

    @Override
    public void postProcess(final ExecutionResult executionResult,
                            final Command command,
                            final Table table,
                            final String auditLabel,
                            final List<HostDocument> documentsToSend,
                            final BigDecimal initiatingPlayerId) {
        if (executionResult == null || executionResult.getGameXPEvents() == null) {
            return;
        }
        final GameXPEvents xpEvents = executionResult.getGameXPEvents();
        final Set<PlayerStatisticEvent> playerEvents = new HashSet<PlayerStatisticEvent>();
        final Map<BigDecimal, Set<StatisticEvent>> allEvents = xpEvents.getPlayerXPEvents();
        for (BigDecimal playerId : allEvents.keySet()) {
            playerEvents.add(new PlayerStatisticEvent(playerId, table.getGameTypeId(), allEvents.get(playerId)));
        }
        playerStatisticEventsPublisher.publishEvents(playerEvents);
    }
}
