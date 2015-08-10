package com.yazino.platform.processor.table.handler;

import com.yazino.platform.gamehost.GameHost;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.table.AttemptToCloseTableRequest;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.model.table.TableRequestType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Qualifier("tableRequestHandler")
public class TableClosingHandler extends TablePersistingRequestHandler<AttemptToCloseTableRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(TableClosingHandler.class);

    @Override
    public boolean accepts(final TableRequestType requestType) {
        return requestType == TableRequestType.ATTEMPT_TO_CLOSE;
    }

    @Override
    protected List<HostDocument> execute(final AttemptToCloseTableRequest gameAction,
                                         final GameHost gameHost,
                                         final Table table) {
        LOG.debug("Table {} - Trying to close .", gameAction.getTableId());

        return gameHost.attemptClose(table);
    }
}
