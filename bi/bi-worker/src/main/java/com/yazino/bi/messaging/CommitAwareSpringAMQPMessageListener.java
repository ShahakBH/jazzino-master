package com.yazino.bi.messaging;

import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import com.yazino.platform.messaging.consumer.SpringAMQPMessageListener;

public class CommitAwareSpringAMQPMessageListener extends SpringAMQPMessageListener implements CommitAware {
    private final CommitAware commitAwareConsumerQueue;

    public CommitAwareSpringAMQPMessageListener(final QueueMessageConsumer<?> consumerQueue) {
        super(consumerQueue);

        // we could do this via bounded generics, but then casting to call the constructor is a right pain
        this.commitAwareConsumerQueue = (CommitAware) consumerQueue;
    }

    @Override
    public void consumerCommitting() {
        commitAwareConsumerQueue.consumerCommitting();
    }
}
