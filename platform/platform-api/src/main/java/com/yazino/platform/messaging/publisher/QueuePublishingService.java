package com.yazino.platform.messaging.publisher;


import com.yazino.platform.messaging.Message;

public interface QueuePublishingService<T extends Message> {
    void send(T message);
}
