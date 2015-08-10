package com.yazino.platform.processor.statistic.opengraph;

import com.google.common.base.Function;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.opengraph.OpenGraphAction;
import com.yazino.platform.opengraph.OpenGraphActionMessage;
import com.yazino.platform.playerstatistic.StatisticEvent;
import com.yazino.platform.processor.statistic.PlayerStatisticEventConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;

@Component("openGraphStatisticEventsConsumer")
@Qualifier("playerStatisticEventConsumers")
public class OpenGraphStatisticEventConsumer implements PlayerStatisticEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(OpenGraphStatisticEventConsumer.class);

    private final QueuePublishingService<OpenGraphActionMessage> openGraphActionQueuePublishingService;
    private final Function<StatisticEvent, OpenGraphAction> transformer;
    private final Boolean enabled;

    @Autowired
    public OpenGraphStatisticEventConsumer(
            @Qualifier("statisticEventOpenGraphActionTransformer")
            final Function<StatisticEvent, OpenGraphAction> transformer,
            @Qualifier("openGraphActionQueuePublishingService")
            final QueuePublishingService<OpenGraphActionMessage> openGraphActionQueuePublishingService,
            @Value("${opengraph.publishing.enabled}") final String enabled) {
        this.transformer = transformer;
        this.openGraphActionQueuePublishingService = openGraphActionQueuePublishingService;
        this.enabled = Boolean.valueOf(enabled);
    }

    @Override
    public void processEvents(final BigDecimal playerId,
                              final String gameType,
                              final Collection<StatisticEvent> events) {
        if (!enabled) {
            return;
        }
        for (StatisticEvent event : events) {
            processEventSafely(playerId, gameType, event);
        }
    }

    private void processEventSafely(final BigDecimal playerId,
                                    final String gameType,
                                    final StatisticEvent event) {
        try {
            processEvent(playerId, gameType, event);
        } catch (Exception e) {
            LOG.warn("Error processing event.", e);
        }
    }

    private void processEvent(final BigDecimal playerId,
                              final String gameType,
                              final StatisticEvent event) {
        final OpenGraphAction action = transformer.apply(event);
        if (action != null) {
            LOG.debug("Event {} transformed to action {}", event, action);
            final OpenGraphActionMessage message = new OpenGraphActionMessage(
                    playerId.toBigInteger(), gameType, action);
            openGraphActionQueuePublishingService.send(message);
        } else {
            LOG.debug("No action for event: {}", event);
        }
    }
}
