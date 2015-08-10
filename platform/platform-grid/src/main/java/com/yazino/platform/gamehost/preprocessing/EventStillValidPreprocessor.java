package com.yazino.platform.gamehost.preprocessing;

import com.yazino.platform.model.table.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yazino.game.api.ScheduledEvent;

public class EventStillValidPreprocessor implements EventPreprocessor {
    private static final Logger LOG = LoggerFactory.getLogger(EventStillValidPreprocessor.class);

    public boolean preprocess(final ScheduledEvent event,
                              final Table table) {
        if (event.getGameId() != table.getGameId()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Table {}: event is not valid for this table table gameid is {} event game id is {}",
                        table.getTableId(), table.getGameId(), event.getGameId());
            }
            return false;
        }
        return true;
    }
}
