package com.yazino.yaps;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.retry.MessageKeyGenerator;

/**
 * Creates a random message ID should the incoming message not have one.
 */
public class MessageIDGenerator implements MessageKeyGenerator {

    @Override
    public Object getKey(Message message) {
        String messageId = message.getMessageProperties().getMessageId();
        if (messageId == null) {
            messageId = RandomStringUtils.random(10);
            message.getMessageProperties().setMessageId(messageId);
        }
        return messageId;
    }
}
