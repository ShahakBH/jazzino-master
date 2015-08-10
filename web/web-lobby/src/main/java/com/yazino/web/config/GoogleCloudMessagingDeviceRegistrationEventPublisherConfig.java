package com.yazino.web.config;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.android.MessagingDeviceRegistrationEvent;
import com.yazino.platform.messaging.WorkerServers;
import com.yazino.platform.messaging.WorkerServiceFactory;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.messaging.publisher.SafeQueuePublishingEventService;
import com.yazino.platform.messaging.publisher.SpringAMQPRoutedQueuePublishingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GoogleCloudMessagingDeviceRegistrationEventPublisherConfig {

    private final WorkerServiceFactory factory = new WorkerServiceFactory();

    @Value("${strata.rabbitmq.messaging-device-registration.exchange}")
    private String exchangeName;

    @Value("${strata.rabbitmq.messaging-device-registration.queue}")
    private String queueName;

    @Value("${strata.rabbitmq.messaging-device-registration.routing-key}")
    private String routingKey;

    @Autowired
    private YazinoConfiguration yazinoConfiguration;

    @Bean
    public WorkerServers messagingDeviceRegistrationWorkerServers(
            @Value("${strata.rabbitmq.messaging-device-registration.port}") final int port,
            @Value("${strata.rabbitmq.messaging-device-registration.username}") final String username,
            @Value("${strata.rabbitmq.messaging-device-registration.password}") final String password,
            @Value("${strata.rabbitmq.messaging-device-registration.virtualhost}") final String virtualHost) {
        return new WorkerServers("strata.rabbitmq.messaging-device-registration.host", port, username, password, virtualHost);
    }

    @Bean
    public QueuePublishingService<MessagingDeviceRegistrationEvent> messagingDeviceRegistrationService(
            @Qualifier("messagingDeviceRegistrationWorkerServers") final WorkerServers workerServers) {

        SpringAMQPRoutedQueuePublishingService publisher = factory.createPublisher(workerServers, exchangeName, queueName, routingKey, yazinoConfiguration);

        return new SafeQueuePublishingEventService<MessagingDeviceRegistrationEvent>(publisher);
    }
}
