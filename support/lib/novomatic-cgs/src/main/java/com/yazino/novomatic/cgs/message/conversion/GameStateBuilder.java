package com.yazino.novomatic.cgs.message.conversion;

import com.yazino.novomatic.cgs.NovomaticGameState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class GameStateBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(GameStateBuilder.class);
    private final GameEventsConverter eventsConverter = new GameEventsConverter();

    public NovomaticGameState buildFromMap(Map map) {
        LOG.debug("Building GameState from {}", map);
        return new NovomaticGameState((byte[]) map.get("gmstate"), eventsConverter.convert((List<Map<String, Object>>) map.get("events")));
    }
}
