package com.yazino.platform.android;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.messaging.WorkerServers;
import com.yazino.platform.messaging.WorkerServiceFactory;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.messaging.publisher.SafeQueuePublishingEventService;
import com.yazino.platform.messaging.publisher.SpringAMQPRoutedQueuePublishingService;
import com.yazino.platform.worker.PlatformWorkerServersConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(PlatformWorkerServersConfiguration.class)
public class MessagingDeviceRegistrationServiceConfiguration {

    private WorkerServiceFactory factory = new WorkerServiceFactory();

    @Autowired(required = true)
    @Qualifier("platformWorkerServers")
    private WorkerServers workerServers;

    @Value("${strata.rabbitmq.platform.exchange}")
    private String exchangeName;

    @Autowired
    private YazinoConfiguration yazinoConfiguration;

    @Bean
    public QueuePublishingService<MessagingDeviceRegistrationEvent> messagingDeviceRegistrationService(
            @Value("${strata.rabbitmq.platform.messaging-device-registration.queue}") final String queueName,
            @Value("${strata.rabbitmq.platform.messaging-device-registration.routing-key}") final String routingKey) {
        final SpringAMQPRoutedQueuePublishingService publisher = factory.createPublisher(
                workerServers, exchangeName, queueName, routingKey, yazinoConfiguration);
        return new SafeQueuePublishingEventService<MessagingDeviceRegistrationEvent>(publisher);
    }
}
