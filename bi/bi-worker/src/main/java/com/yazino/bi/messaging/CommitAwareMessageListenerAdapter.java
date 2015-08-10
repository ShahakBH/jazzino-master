package com.yazino.bi.messaging;

import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.MessageConverter;

public class CommitAwareMessageListenerAdapter extends MessageListenerAdapter implements CommitAware {
    private final CommitAware commitAwareDelegate;

    public CommitAwareMessageListenerAdapter(final CommitAware delegate, final MessageConverter messageConverter) {
        super(delegate, messageConverter);

        this.commitAwareDelegate = delegate;
    }

    @Override
    public void consumerCommitting() {
        commitAwareDelegate.consumerCommitting();
    }
}
