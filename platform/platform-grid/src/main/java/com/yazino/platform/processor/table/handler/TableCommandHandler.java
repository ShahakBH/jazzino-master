package com.yazino.platform.processor.table.handler;

import com.yazino.platform.gamehost.GameHost;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.table.CommandWrapper;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.model.table.TableRequestType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Qualifier("tableRequestHandler")
public class TableCommandHandler extends TablePersistingRequestHandler<CommandWrapper> {
    private static final Logger TIMING_LOG = LoggerFactory.getLogger("senet.timings");

    @Override
    public boolean accepts(final TableRequestType requestType) {
        return requestType == TableRequestType.COMMAND;
    }

    protected List<HostDocument> execute(final CommandWrapper commandWrapper,
                                         final GameHost gameHost,
                                         final Table table) {
        long processingStart = 0;
        if (TIMING_LOG.isDebugEnabled()) {
            processingStart = System.currentTimeMillis();
        }

        final List<HostDocument> documentsToSend = gameHost.execute(table, commandWrapper);

        if (TIMING_LOG.isDebugEnabled()) {
            final long finishTime = System.currentTimeMillis();
            final long processingTime = finishTime - processingStart;
            TIMING_LOG.debug(String.format("\t%s_Game\t%s\t%s", commandWrapper.getType(),
                    processingTime, commandWrapper.getRequestId()));
            if (commandWrapper.getTimestamp() != null) {
                final long totalTime = finishTime - commandWrapper.getTimestamp().getTime();
                TIMING_LOG.debug(String.format("\t%s\t%s\t%s", commandWrapper.getType(),
                        totalTime, commandWrapper.getRequestId()));
            }
        }

        return documentsToSend;
    }
}
