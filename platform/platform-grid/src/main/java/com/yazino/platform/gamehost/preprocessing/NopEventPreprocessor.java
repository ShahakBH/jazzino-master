package com.yazino.platform.gamehost.preprocessing;

import com.yazino.platform.model.table.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yazino.game.api.ScheduledEvent;

public class NopEventPreprocessor implements EventPreprocessor {
    private static final Logger LOG = LoggerFactory.getLogger(NopEventPreprocessor.class);

    public boolean preprocess(final ScheduledEvent event,
                              final Table table) {
        if (event.isNoProcessingEvent()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Table {}: No Processing Event", table.getTableId());
            }
            return false;
        }
        return true;
    }
}
