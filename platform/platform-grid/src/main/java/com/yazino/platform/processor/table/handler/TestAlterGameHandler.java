package com.yazino.platform.processor.table.handler;

import com.yazino.platform.gamehost.GameHost;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.model.table.TableRequestType;
import com.yazino.platform.model.table.TestAlterGameRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@Qualifier("tableRequestHandler")
public class TestAlterGameHandler extends TablePersistingRequestHandler<TestAlterGameRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(TestAlterGameHandler.class);

    @Override
    protected List<HostDocument> execute(final TestAlterGameRequest request,
                                         final GameHost gameHost,
                                         final Table table) {
        LOG.error("Overwriting game for table {} - this should never be visible outside of test systems", table.getTableId());
        LOG.error("Overwriting game for table {}: new value is {}", table.getTableId(), request.getGameStatus());

        table.setCurrentGame(request.getGameStatus());
        table.nextIncrement();

        return Collections.emptyList();
    }

    @Override
    public boolean accepts(final TableRequestType requestType) {
        return requestType == TableRequestType.TEST_ALTER_GAME;
    }
}
