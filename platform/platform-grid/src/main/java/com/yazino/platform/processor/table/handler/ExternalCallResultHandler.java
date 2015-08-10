package com.yazino.platform.processor.table.handler;

import com.yazino.platform.gamehost.GameHost;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.table.ExternalCallResultWrapper;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.model.table.TableRequestType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Qualifier("tableRequestHandler")
public class ExternalCallResultHandler extends TablePersistingRequestHandler<ExternalCallResultWrapper> {
    private static final Logger LOG = LoggerFactory.getLogger(ExternalCallResultHandler.class);

    @Override
    protected List<HostDocument> execute(ExternalCallResultWrapper wrapper,
                                         GameHost gameHost,
                                         Table table) {
        LOG.debug("Handling {}", wrapper);
        return gameHost.execute(table, wrapper.getExternalCallResult());
    }

    @Override
    public boolean accepts(TableRequestType requestType) {
        return requestType == TableRequestType.EXTERNAL_CALL_RESULT;
    }
}
