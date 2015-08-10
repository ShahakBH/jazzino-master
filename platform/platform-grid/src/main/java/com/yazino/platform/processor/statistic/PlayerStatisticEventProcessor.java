package com.yazino.platform.processor.statistic;

import com.yazino.platform.model.statistic.PlayerStatistics;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Collection;

import static org.apache.commons.lang3.Validate.notNull;

@EventDriven
@Polling(gigaSpace = "gigaSpace", concurrentConsumers = 5, maxConcurrentConsumers = 15)
public class PlayerStatisticEventProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(PlayerStatisticEventProcessor.class);

    private static final PlayerStatistics TEMPLATE = new PlayerStatistics();

    private final Collection<PlayerStatisticEventConsumer> consumers;

    @Autowired
    public PlayerStatisticEventProcessor(@Qualifier("playerStatisticEventConsumers") final Collection<PlayerStatisticEventConsumer> consumers) {
        notNull(consumers, "consumers is null");

        this.consumers = consumers;
    }

    @EventTemplate
    public PlayerStatistics template() {
        return TEMPLATE;
    }

    @SpaceDataEvent
    public void process(final PlayerStatistics event) {
        LOG.debug("Received message {}", event);
        if (event == null) {
            return;
        }

        for (PlayerStatisticEventConsumer consumer : consumers) {
            try {
                consumer.processEvents(event.getPlayerId(), event.getGameType(), event.getEvents());

            } catch (Exception e) {
                LOG.error("Error consuming message {}", event, e);
            }

        }
    }
}
