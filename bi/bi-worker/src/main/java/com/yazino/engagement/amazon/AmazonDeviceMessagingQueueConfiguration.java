package com.yazino.engagement.amazon;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.messaging.WorkerServers;
import com.yazino.platform.messaging.WorkerServiceFactory;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.messaging.publisher.SafeQueuePublishingEventService;
import com.yazino.platform.messaging.publisher.SpringAMQPRoutedQueuePublishingService;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AmazonDeviceMessagingQueueConfiguration {
    private WorkerServiceFactory factory = new WorkerServiceFactory();

    @Value("${strata.rabbitmq.amazon-device-messaging.exchange}")
    private String exchangeName;
    @Value("${strata.rabbitmq.amazon-device-messaging.queue}")
    private String queueName;
    @Value("${strata.rabbitmq.amazon-device-messaging.routing-key}")
    private String routingKey;
    @Value("${strata.rabbitmq.amazon-device-messaging.consumer-count}")
    private int consumerCount;

    @Autowired
    private YazinoConfiguration yazinoConfiguration;

    @Bean
    public WorkerServers amazonDeviceMessagingWorkerServers(
            @Value("${strata.rabbitmq.worker.port}") final int port,
            @Value("${strata.rabbitmq.worker.username}") final String username,
            @Value("${strata.rabbitmq.worker.password}") final String password,
            @Value("${strata.rabbitmq.worker.virtualhost}") final String virtualHost) {
        return new WorkerServers("strata.rabbitmq.worker.host", port, username, password, virtualHost);
    }

    @Bean
    public SimpleMessageListenerContainer getAmazonDeviceMessagingMLContainer(
            @Qualifier("amazonDeviceMessagingWorkerServers") final WorkerServers workerServers,
            @Qualifier("amazonDeviceMessagingConsumer") final QueueMessageConsumer<AmazonDeviceMessage> consumer) {
        return factory.startConcurrentConsumers(
                workerServers, yazinoConfiguration, exchangeName, queueName, routingKey, consumer, consumerCount, 1);
    }

    @Bean(name = "amazonDeviceMessageQueuePublishingService")
    public QueuePublishingService<AmazonDeviceMessage> amazonDeviceMessageQueuePublishingService(
            @Qualifier("amazonDeviceMessagingWorkerServers") final WorkerServers workerServers) {
        final SpringAMQPRoutedQueuePublishingService publisher = factory.createPublisher(
                workerServers, exchangeName, queueName, routingKey, yazinoConfiguration);
        return new SafeQueuePublishingEventService<AmazonDeviceMessage>(publisher);
    }
}
