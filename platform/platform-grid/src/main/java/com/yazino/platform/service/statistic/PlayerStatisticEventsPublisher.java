package com.yazino.platform.service.statistic;

import com.yazino.platform.model.statistic.PlayerStatisticEvent;

import java.util.Collection;

public interface PlayerStatisticEventsPublisher {
    void publishEvents(Collection<PlayerStatisticEvent> events);
}
