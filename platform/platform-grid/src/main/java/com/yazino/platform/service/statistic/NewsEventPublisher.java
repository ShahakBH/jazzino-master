package com.yazino.platform.service.statistic;

import com.yazino.game.api.NewsEvent;

public interface NewsEventPublisher {
    void send(NewsEvent... events);
}
