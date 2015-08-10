package com.yazino.platform.lightstreamer.adapter;

import org.springframework.amqp.AmqpConnectException;

public interface LifecycleListener {

    void starting();

    void stopping();

    void startupConnectionFailure(AmqpConnectException t);

    void consumerCommitting(int messagesReceived);

}
