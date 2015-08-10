package com.yazino.host;

import com.yazino.model.log.IncrementalLog;
import com.yazino.model.log.LogStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import com.yazino.game.api.TransactionResult;
import com.yazino.platform.model.table.TableRequestWrapper;
import com.yazino.platform.model.table.CommandWrapper;
import com.yazino.platform.model.table.TableRequest;
import com.yazino.platform.model.table.TransactionResultWrapper;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
@Qualifier("gameHostLog")
public class TableRequestWrapperQueue implements IncrementalLog {
    private static final Logger LOG = LoggerFactory.getLogger(TableRequestWrapperQueue.class);

    private final BlockingQueue<TableRequestWrapper> queue = new LinkedBlockingQueue<TableRequestWrapper>();

    private final LogStorage logStorage = new LogStorage();

    public void addRequest(final TableRequestWrapper wrapper) {
        queue.add(wrapper);
        logRequest(wrapper.getTableRequest());
    }

    private void logRequest(final TableRequest tableRequest) {
        final String message = prettyPrint(tableRequest);
        if (message != null) {
            logStorage.log(message);
        }
    }

    private String prettyPrint(final TableRequest tableRequest) {
        if (tableRequest == null) {
            return String.valueOf(null);
        }
        switch (tableRequest.getRequestType()) {
            case COMMAND:
                final CommandWrapper command = (CommandWrapper) tableRequest;
                return String.format("Command   \tgame %s\tplayer %s\t%s %s",
                        command.getGameId(),
                        command.getPlayerId(),
                        command.getType(),
                        Arrays.asList(command.getArgs()));
            case TRANSACTION_RESULT:
                final TransactionResultWrapper tx = (TransactionResultWrapper) tableRequest;
                final TransactionResult tr = tx.getTransactionResult();
                final String successful;
                if (tr.isSuccessful()) {
                    successful = "successful";
                } else {
                    successful = tr.getErrorReason();
                }
                return String.format("Tx result\tgame %s\tplayer %s\t%s (balance=%s)",
                        tx.getGameId(),
                        tr.getPlayerId(),
                        successful,
                        tr.getBalance());
            case PROCESS:
                return null;
            default:
                return String.valueOf(tableRequest);
        }
    }

    public TableRequestWrapper getNextRequest() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            LOG.error("Error retrieving nextTableRequestWrapper", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String nextIncrement() {
        return logStorage.popJSON();
    }
}
