package com.yazino.yaps.config;

import com.yazino.mobile.yaps.config.PlayerDeviceConfiguration;
import com.yazino.mobile.yaps.message.PushMessage;
import com.yazino.platform.messaging.WorkerServers;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlayerDeviceConsumerConfiguration extends PlayerDeviceConfiguration {

    @Bean
    public SimpleMessageListenerContainer getPlayerDeviceMLContainer(
            @Qualifier("playerDeviceWorkerServers") final WorkerServers workerServers,
            @Qualifier("playerDeviceListener") final QueueMessageConsumer<PushMessage> playerDeviceConsumer) {
        return getFactory().startConcurrentConsumers(
                workerServers, getYazinoConfiguration(), getExchangeName(), getQueueName(), getRoutingKey(), playerDeviceConsumer, getConsumerCount(), 1);
    }

}
