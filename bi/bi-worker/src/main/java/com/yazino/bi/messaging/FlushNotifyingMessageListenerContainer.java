package com.yazino.bi.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.ActiveObjectCounter;
import org.springframework.amqp.rabbit.listener.BlockingQueueConsumer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.support.MessagePropertiesConverter;

import java.lang.reflect.Field;

public class FlushNotifyingMessageListenerContainer extends SimpleMessageListenerContainer {
    private static final Logger LOG = LoggerFactory.getLogger(FlushNotifyingMessageListenerContainer.class);

    private int prefetchCount = DEFAULT_PREFETCH_COUNT;
    private int txSize = 1;
    private boolean defaultRequeueRejected = true;

    public FlushNotifyingMessageListenerContainer() {
        super();
    }

    public FlushNotifyingMessageListenerContainer(final ConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    @Override
    protected BlockingQueueConsumer createBlockingQueueConsumer() {
        if (getMessageListener() instanceof CommitAware) {
            return new FlushNotifyingBlockingQueueConsumer(getConnectionFactory(), messagePropertiesConverter(), cancellationLock(),
                    getAcknowledgeMode(), isChannelTransacted(), actualPrefetchCount(), defaultRequeueRejected,
                    (CommitAware) getMessageListener(), getRequiredQueueNames());
        }
        return super.createBlockingQueueConsumer();
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
}
