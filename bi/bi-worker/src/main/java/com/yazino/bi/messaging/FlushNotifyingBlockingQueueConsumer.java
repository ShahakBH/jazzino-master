package com.yazino.bi.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.ActiveObjectCounter;
import org.springframework.amqp.rabbit.listener.BlockingQueueConsumer;
import org.springframework.amqp.rabbit.support.MessagePropertiesConverter;

import java.io.IOException;
import java.sql.BatchUpdateException;
import java.sql.SQLException;

public class FlushNotifyingBlockingQueueConsumer extends BlockingQueueConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(FlushNotifyingBlockingQueueConsumer.class);

    private final CommitAware commitListener;

    public FlushNotifyingBlockingQueueConsumer(final ConnectionFactory connectionFactory,
                                               final MessagePropertiesConverter messagePropertiesConverter,
                                               final ActiveObjectCounter<BlockingQueueConsumer> activeObjectCounter,
                                               final AcknowledgeMode acknowledgeMode,
                                               final boolean transactional,
                                               final int prefetchCount,
                                               final boolean defaultRequeueRejected,
                                               final CommitAware commitListener,
                                               final String... queues) {
        super(connectionFactory, messagePropertiesConverter, activeObjectCounter, acknowledgeMode,
                transactional, prefetchCount, defaultRequeueRejected, queues);
        this.commitListener = commitListener;
    }

    @Override
    public boolean commitIfNecessary(final boolean locallyTransacted) throws IOException {
        try {
            LOG.debug("Processing commit notification");
            commitListener.consumerCommitting();

        } catch (Exception e) {
            LOG.error("Commit listener failed, attempting rollback", e);
            logBatchExceptionsFrom(e);

            attemptRollback(e);

            throw new RuntimeException("Commit listener invocation failed", e);
        }

        return super.commitIfNecessary(locallyTransacted);
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    private void logBatchExceptionsFrom(final Exception e) {
        if (e.getMessage().contains("Call getNextException")) {
            final BatchUpdateException batchUpdateException = batchUpdateExceptionFrom(e);
            if (batchUpdateException != null) {
                SQLException nextException = batchUpdateException;
                while ((nextException = nextException.getNextException()) != null) {
                    LOG.error("Batch update: next exception is {}", nextException);
                }
            }
        }
    }

    private BatchUpdateException batchUpdateExceptionFrom(final Throwable e) {
        if (e == null || e instanceof BatchUpdateException) {
            return (BatchUpdateException) e;
        }

        return batchUpdateExceptionFrom(e.getCause());
    }

    private void attemptRollback(final Exception rollbackException) {
        try {
            rollbackOnExceptionIfNecessary(rollbackException);

        } catch (Exception e) {
            LOG.error("Rollback failed", e);
        }
    }
}
