package com.yazino.platform.lightstreamer.adapter;

import org.joda.time.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.ActiveObjectCounter;
import org.springframework.amqp.rabbit.listener.BlockingQueueConsumer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.support.MessagePropertiesConverter;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class is a gigantic hack to get around SpringAMQP refusing to let us near the message
 * listening loop. The nearest we can get is to override the consumer and listen on the commit
 * messages. It's a mess and dependent on the SpringAMQP implementation, and I welcome better
 * solution.
 */
public class LifecycleListeningMessageListenerContainer extends SimpleMessageListenerContainer {
    private static final Logger LOG = LoggerFactory.getLogger(LifecycleListeningMessageListenerContainer.class);

    private int prefetchCount = DEFAULT_PREFETCH_COUNT;
    private int txSize = 1;
    private boolean defaultRequeueRejected = true;

    private final AtomicLong messageLastReceived = new AtomicLong(DateTimeUtils.currentTimeMillis());
    private final List<LifecycleListener> lifecycleListeners = new CopyOnWriteArrayList<>();

    public LifecycleListeningMessageListenerContainer() {
        super();
    }

    public LifecycleListeningMessageListenerContainer(final ConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    public void addLifecycleListener(final LifecycleListener listener) {
        if (!lifecycleListeners.contains(listener)) {
            lifecycleListeners.add(listener);
        }
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        fireStarting();
    }

    @Override
    protected void handleStartupFailure(final Throwable t) throws Exception {
        LOG.debug("Startup error received", t);

        if (t instanceof AmqpConnectException) {
            fireStartupConnectionFailure((AmqpConnectException) t);
        }

        super.handleStartupFailure(t);
    }

    @Override
    protected void doStop() {
        super.doStop();
        fireStopping();
    }

    @Override
    protected BlockingQueueConsumer createBlockingQueueConsumer() {
        return new LifecycleListeningBlockingQueueConsumer(getConnectionFactory(), messagePropertiesConverter(), cancellationLock(),
                getAcknowledgeMode(), isChannelTransacted(), actualPrefetchCount(), defaultRequeueRejected,
                this, getRequiredQueueNames());
    }

    private int actualPrefetchCount() {
        if (prefetchCount > txSize) {
            return prefetchCount;
        }
        return txSize;
    }

    @SuppressWarnings("unchecked")
    private ActiveObjectCounter<BlockingQueueConsumer> cancellationLock() {
        try {
            final Field cancellationLockField = SimpleMessageListenerContainer.class.getDeclaredField("cancellationLock");
            cancellationLockField.setAccessible(true);
            return (ActiveObjectCounter<BlockingQueueConsumer>) cancellationLockField.get(this);

        } catch (Exception e) {
            throw new RuntimeException("Cannot brute force our way to cancellationLockField - have you updated SpringAMQP?", e);
        }
    }

    @SuppressWarnings("unchecked")
    private MessagePropertiesConverter messagePropertiesConverter() {
        // messagePropertiesConverter is set by default, so capturing it via the setter is holey
        try {
            final Field messagePropertiesConverterField = SimpleMessageListenerContainer.class.getDeclaredField("messagePropertiesConverter");
            messagePropertiesConverterField.setAccessible(true);
            return (MessagePropertiesConverter) messagePropertiesConverterField.get(this);

        } catch (Exception e) {
            throw new RuntimeException("Cannot brute force our way to messagePropertiesConverter - have you updated SpringAMQP?", e);
        }
    }

    @Override
    public void setPrefetchCount(final int prefetchCount) {
        this.prefetchCount = prefetchCount;
        super.setPrefetchCount(prefetchCount);
    }

    @Override
    public void setTxSize(final int txSize) {
        this.txSize = txSize;
        super.setTxSize(txSize);
    }

    @Override
    public void setDefaultRequeueRejected(final boolean defaultRequeueRejected) {
        this.defaultRequeueRejected = defaultRequeueRejected;
        super.setDefaultRequeueRejected(defaultRequeueRejected);
    }


    public long getMessageLastReceived() {
        return messageLastReceived.get();
    }

    void notifyMessageReceived() {
        messageLastReceived.set(DateTimeUtils.currentTimeMillis());
    }

    void fireConsumerCommitting(final int messagesReceived) {
        for (LifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.consumerCommitting(messagesReceived);
        }
    }

    private void fireStopping() {
        for (LifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.stopping();
        }
    }

    private void fireStarting() {
        for (LifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.starting();
        }
    }

    private void fireStartupConnectionFailure(final AmqpConnectException t) {
        for (LifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.startupConnectionFailure(t);
        }
    }
}
