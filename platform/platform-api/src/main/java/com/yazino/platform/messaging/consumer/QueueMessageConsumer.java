package com.yazino.platform.messaging.consumer;

import com.yazino.platform.messaging.Message;

public interface QueueMessageConsumer<T extends Message> {

    void handle(T message);
}
