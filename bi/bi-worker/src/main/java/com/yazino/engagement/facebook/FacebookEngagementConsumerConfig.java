package com.yazino.engagement.facebook;

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

/**
 * Consumer config for FaceBook Request queues
 */
@Configuration
public class FacebookEngagementConsumerConfig {
    private final WorkerServiceFactory factory = new WorkerServiceFactory();

    @Value("${strata.rabbitmq.fb-app-to-user-request.exchange}")
    private String exchangeName;
    @Value("${strata.rabbitmq.fb-app-to-user-request.queue}")
    private String queueName;
    @Value("${strata.rabbitmq.fb-app-to-user-request.routing-key}")
    private String routingKey;
    @Value("${strata.rabbitmq.fb-app-to-user-request.consumer-count}")
    private int consumerCount;
    @Value("${strata.rabbitmq.fb-delete-request.queue}")
    private String deleteQueueName;
    @Value("${strata.rabbitmq.fb-delete-request.routing-key}")
    private String deleteRoutingKey;

    @Autowired
    private YazinoConfiguration yazinoConfiguration;

    @Bean
    public WorkerServers facebookAppRequestConsumerWorkerServers(
            @Value("${strata.rabbitmq.fb-app-to-user-request.port}") final int port,
            @Value("${strata.rabbitmq.fb-app-to-user-request.username}") final String username,
            @Value("${strata.rabbitmq.fb-app-to-user-request.password}") final String password,
            @Value("${strata.rabbitmq.fb-app-to-user-request.virtualhost}") final String virtualHost) {
        return new WorkerServers("strata.rabbitmq.fb-app-to-user-request.host", port, username, password, virtualHost);
    }

    @Bean
    public SimpleMessageListenerContainer fbAppToUserMessageListenerContainer(
            @Qualifier("facebookAppRequestConsumerWorkerServers") final WorkerServers workerServers,
            @Qualifier("fbRequestConsumer") final QueueMessageConsumer<GoogleCloudMessage> consumer) {
        return factory.startConcurrentConsumers(
                workerServers, yazinoConfiguration, exchangeName, queueName, routingKey, consumer, consumerCount, 1);
    }

    @Bean
    public SimpleMessageListenerContainer fbDeleteRequestMessageListenerContainer(
            @Qualifier("facebookAppRequestConsumerWorkerServers") final WorkerServers workerServers,
            @Qualifier("fbDeleteRequestConsumer") final QueueMessageConsumer<FacebookDeleteAppRequestMessage> consumer) {
        return factory.startConcurrentConsumers(
                workerServers, yazinoConfiguration, exchangeName, deleteQueueName, deleteRoutingKey, consumer, consumerCount, 1);
    }

}
