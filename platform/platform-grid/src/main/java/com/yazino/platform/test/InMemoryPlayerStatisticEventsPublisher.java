package com.yazino.platform.test;

import com.yazino.platform.model.statistic.PlayerStatisticEvent;
import com.yazino.platform.service.statistic.PlayerStatisticEventsPublisher;

import java.util.Collection;

public class InMemoryPlayerStatisticEventsPublisher implements PlayerStatisticEventsPublisher {
    @Override
    public void publishEvents(final Collection<PlayerStatisticEvent> events) {
    }
}
