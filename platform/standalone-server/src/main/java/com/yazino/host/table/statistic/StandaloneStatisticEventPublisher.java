package com.yazino.host.table.statistic;

import org.springframework.stereotype.Component;
import com.yazino.platform.model.statistic.PlayerStatisticEvent;
import com.yazino.platform.service.statistic.PlayerStatisticEventsPublisher;

import java.util.Collection;

@Component
public class StandaloneStatisticEventPublisher implements PlayerStatisticEventsPublisher {
    @Override
    public void publishEvents(final Collection<PlayerStatisticEvent> events) {
    }
}
