package com.yazino.platform.test;

import com.yazino.game.api.ScheduledEvent;

import java.util.HashMap;
import java.util.Map;

public class PrintLnEvent {
    private Long delayInMillis;

    private Long gameId;

    private String message;

    public PrintLnEvent(final Long delayInMillis,
                        final Long gameId,
                        final String message) {
        this.message = message;
        this.delayInMillis = delayInMillis;
        this.gameId = gameId;
    }

    public ScheduledEvent toScheduledEvent() {
        final Map<String, String> map = new HashMap<String, String>();
        map.put("message", message);
        return new ScheduledEvent(delayInMillis, gameId, this.getClass().getName(),
                this.getClass().getSimpleName(), map, true);
    }

    public Long getDelayInMillis() {
        return delayInMillis;
    }

    public Long getGameId() {
        return gameId;
    }
}
