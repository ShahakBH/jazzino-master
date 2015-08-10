package com.yazino.platform.gamehost.preprocessing;

import com.yazino.platform.model.table.Table;
import com.yazino.game.api.ScheduledEvent;

public interface EventPreprocessor {
    boolean preprocess(ScheduledEvent event,
                       Table table);
}
