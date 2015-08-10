package com.yazino.platform.processor.statistic;

import com.yazino.platform.model.statistic.PlayerStatisticEvent;
import com.yazino.platform.service.statistic.StatisticEventService;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import com.yazino.game.api.statistic.StatisticEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

@EventDriven
@Polling(gigaSpace = "gigaSpace")
public class PlayerStatisticEventPublishingProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(PlayerStatisticEventPublishingProcessor.class);

    private static final PlayerStatisticEvent TEMPLATE = new PlayerStatisticEvent();
    private final StatisticEventService statisticEventService;

    /*
      * CGLib constructor.
      */
    protected PlayerStatisticEventPublishingProcessor() {
        this.statisticEventService = null;
    }

    @Autowired(required = true)
    public PlayerStatisticEventPublishingProcessor(
            @Qualifier("statisticEventService") final StatisticEventService statisticEventService) {
        notNull(statisticEventService, "statisticEventService must not be null");
        this.statisticEventService = statisticEventService;
    }


    @EventTemplate
    public PlayerStatisticEvent template() {
        return TEMPLATE;
    }

    @SpaceDataEvent
    public void processRequest(final PlayerStatisticEvent request) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("ProcessRequest: %s", request));
        }
        statisticEventService.publishStatistics(request.getPlayerId(), request.getGameType(),
                convertEvents(request.getEvents()));
    }

    private Collection<com.yazino.platform.playerstatistic.StatisticEvent> convertEvents(
            final Collection<StatisticEvent> events) {
        final List<com.yazino.platform.playerstatistic.StatisticEvent> result
                = new ArrayList<com.yazino.platform.playerstatistic.StatisticEvent>();
        for (StatisticEvent event : events) {
            result.add(new com.yazino.platform.playerstatistic.StatisticEvent(event.getEvent(),
                    event.getDelay(), event.getMultiplier(), event.getParameters().toArray()));
        }
        return result;
    }


}
