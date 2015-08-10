package com.yazino.engagement.android;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.engagement.GoogleCloudMessage;
import com.yazino.platform.messaging.WorkerServers;
import com.yazino.platform.messaging.WorkerServiceFactory;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GoogleCloudMessagingForAndroidRequestConsumerConfig {
    private final WorkerServiceFactory factory = new WorkerServiceFactory();

    @Value("${strata.rabbitmq.google-cloud-messaging-for-android.exchange}")
    private String exchangeName;
    @Value("${strata.rabbitmq.google-cloud-messaging-for-android.queue}")
    private String queueName;
    @Value("${strata.rabbitmq.google-cloud-messaging-for-android.routing-key}")
    private String routingKey;
    @Value("${strata.rabbitmq.google-cloud-messaging-for-android.consumer-count}")
    private int consumerCount;

    @Autowired
    private YazinoConfiguration yazinoConfiguration;

    @Bean
    public WorkerServers googleCloudMessagingForAndroidWorkerServers(
            @Value("${strata.rabbitmq.google-cloud-messaging-for-android.port}") final int port,
            @Value("${strata.rabbitmq.google-cloud-messaging-for-android.username}") final String username,
            @Value("${strata.rabbitmq.google-cloud-messaging-for-android.password}") final String password,
            @Value("${strata.rabbitmq.google-cloud-messaging-for-android.virtualhost}") final String virtualHost) {
        return new WorkerServers("strata.rabbitmq.google-cloud-messaging-for-android.host", port, username, password, virtualHost);
    }

    @Bean
    public SimpleMessageListenerContainer googleCloudMessagingForAndroidWorkerMessageListenerContainer(
            @Qualifier("googleCloudMessagingForAndroidWorkerServers") final WorkerServers workerServers,
            @Qualifier("googleCloudMessagingRequestConsumer") final QueueMessageConsumer<GoogleCloudMessage> consumer) {
        return factory.startConcurrentConsumers(
                workerServers, yazinoConfiguration, exchangeName, queueName, routingKey, consumer, consumerCount, 1);
    }

}
