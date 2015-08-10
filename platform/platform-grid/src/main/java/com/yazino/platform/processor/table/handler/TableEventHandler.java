package com.yazino.platform.processor.table.handler;

import com.yazino.platform.gamehost.GameHost;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.table.ProcessTableRequest;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.model.table.TableRequestType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.yazino.game.api.ScheduledEvent;
import com.yazino.game.api.time.TimeSource;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

@Service
@Qualifier("tableRequestHandler")
public class TableEventHandler extends TablePersistingRequestHandler<ProcessTableRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(TableEventHandler.class);

    private final TimeSource timeSource;

    @Autowired
    public TableEventHandler(final TimeSource timeSource) {
        notNull(timeSource, "TimeSource may not be null");
        this.timeSource = timeSource;
    }

    @Override
    public boolean accepts(final TableRequestType requestType) {
        return requestType == TableRequestType.PROCESS;
    }


    protected List<HostDocument> execute(final ProcessTableRequest processTableRequest,
                                         final GameHost gameHost,
                                         final Table table) {
        LOG.debug("execute {}", processTableRequest);

        final List<ScheduledEvent> scheduledEvents = table.getPendingEvents(timeSource);
        LOG.debug("Preparing to handle {} Events.", scheduledEvents.size());

        final List<HostDocument> documentsToSend = new ArrayList<HostDocument>();
        for (ScheduledEvent scheduledEvent : scheduledEvents) {
            LOG.debug("Processing Scheduled Event: {}", scheduledEvent.getEventSimpleName());
            documentsToSend.addAll(gameHost.execute(table, scheduledEvent));
        }

        return documentsToSend;
    }
}
