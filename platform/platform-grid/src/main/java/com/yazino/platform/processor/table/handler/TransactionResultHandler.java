package com.yazino.platform.processor.table.handler;

import com.yazino.platform.gamehost.GameHost;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.model.table.TableRequestType;
import com.yazino.platform.model.table.TransactionResultWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Processor class to execute {@link com.yazino.platform.model.table.TransactionResultWrapper}s.
 */
@Service
@Qualifier("tableRequestHandler")
public class TransactionResultHandler extends TablePersistingRequestHandler<TransactionResultWrapper> {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionResultHandler.class);
    private static final Logger LOG_TIMING = LoggerFactory.getLogger("senet.timings");

    @Override
    public boolean accepts(final TableRequestType requestType) {
        return requestType == TableRequestType.TRANSACTION_RESULT;
    }

    @Override
    protected List<HostDocument> execute(final TransactionResultWrapper transactionResultWrapper,
                                         final GameHost gameHost,
                                         final Table table) {
        if (gameHost == null) {
            throw new IllegalStateException("Class was created via CGLib constructor and is invalid for direct use");
        }

        notNull(transactionResultWrapper, "Transaction Result Wrapper may not be null");

        notNull(transactionResultWrapper.getTableId(),
                "Transaction Result Wrapper: table ID may not be null");
        notNull(transactionResultWrapper.getGameId(),
                "Transaction Result Wrapper: game ID may not be null");
        notNull(transactionResultWrapper.getTransactionResult(),
                "Transaction Result Wrapper: transaction result may not be null");

        long gameStartTime = 0;
        final long finishTime = 0;
        if (LOG_TIMING.isDebugEnabled()) {
            gameStartTime = System.currentTimeMillis();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Processing Transaction Result: Table ID: %s; Game ID: %s; Result: %s",
                    transactionResultWrapper.getTableId(), transactionResultWrapper.getGameId(),
                    transactionResultWrapper));
        }

        if (!table.getOpenOrClosing()) {
            return Collections.emptyList();
        }

        final List<HostDocument> documentsToSend = gameHost.processTransactionResult(
                table, transactionResultWrapper.getGameId(), transactionResultWrapper.getTransactionResult());

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Processed Transaction Result: Table ID: %s; Game ID: %s; Result: %s",
                    transactionResultWrapper.getTableId(), transactionResultWrapper.getGameId(),
                    transactionResultWrapper));
        }

        if (LOG_TIMING.isDebugEnabled()) {
            final long totalGameTime = finishTime - gameStartTime;
            LOG_TIMING.debug(String.format("\tTransaction_Game\t%s\t%s",
                    totalGameTime, transactionResultWrapper.getCommandReference()));
            if (transactionResultWrapper.getTimestamp() != null) {
                final long transactionStartTime = transactionResultWrapper.getTimestamp().getTime();
                final long totalTransactionTime = finishTime - transactionStartTime;
                LOG_TIMING.debug(String.format("\tTransaction\t%s\t%s",
                        totalTransactionTime, transactionResultWrapper.getCommandReference()));
            }
        }

        return documentsToSend;
    }
}
