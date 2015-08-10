package com.yazino.platform.lightstreamer.adapter;

import com.rabbitmq.client.ShutdownSignalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.ActiveObjectCounter;
import org.springframework.amqp.rabbit.listener.BlockingQueueConsumer;
import org.springframework.amqp.rabbit.support.MessagePropertiesConverter;

import java.io.IOException;
import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

public class LifecycleListeningBlockingQueueConsumer extends BlockingQueueConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(LifecycleListeningBlockingQueueConsumer.class);

    private final LifecycleListeningMessageListenerContainer parent;
    private final AtomicInteger messagesSinceCommit = new AtomicInteger();

    public LifecycleListeningBlockingQueueConsumer(final ConnectionFactory connectionFactory,
                                                   final MessagePropertiesConverter messagePropertiesConverter,
                                                   final ActiveObjectCounter<BlockingQueueConsumer> activeObjectCounter,
                                                   final AcknowledgeMode acknowledgeMode,
                                                   final boolean transactional,
                                                   final int prefetchCount,
                                                   final boolean defaultRequeueRejected,
                                                   final LifecycleListeningMessageListenerContainer parent,
                                                   final String... queues) {
        super(connectionFactory, messagePropertiesConverter, activeObjectCounter, acknowledgeMode,
                transactional, prefetchCount, defaultRequeueRejected, queues);
        this.parent = parent;
    }

    @Override
    public Message nextMessage() throws InterruptedException, ShutdownSignalException {
        final Message message = super.nextMessage();
        if (message != null) {
            messagesSinceCommit.incrementAndGet();
            parent.notifyMessageReceived();
        }
        return message;
    }

    @Override
    public Message nextMessage(final long timeout) throws InterruptedException, ShutdownSignalException {
        final Message message = super.nextMessage(timeout);
        if (message != null) {
            messagesSinceCommit.incrementAndGet();
            parent.notifyMessageReceived();
        }
        return message;
    }

    @Override
    public boolean commitIfNecessary(final boolean locallyTransacted) throws IOException {
        try {
            final int messagesReceived = messagesSinceCommit.getAndSet(0);
            LOG.debug("Processing commit notification, {} messages received", messagesReceived);
            parent.fireConsumerCommitting(messagesReceived);

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
