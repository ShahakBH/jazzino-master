package com.yazino.web.config;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.messaging.WorkerServers;
import com.yazino.platform.messaging.WorkerServiceFactory;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.messaging.publisher.SafeQueuePublishingEventService;
import com.yazino.platform.messaging.publisher.SpringAMQPRoutedQueuePublishingService;
import com.yazino.platform.tracking.TrackingEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TrackingEventPublisherConfig {

    private final WorkerServiceFactory factory = new WorkerServiceFactory();

    @Value("${strata.rabbitmq.tracking.exchange}")
    private String exchangeName;

    @Value("${strata.rabbitmq.tracking.queue}")
    private String queueName;

    @Value("${strata.rabbitmq.tracking.routing-key}")
    private String routingKey;

    @Autowired
    private YazinoConfiguration yazinoConfiguration;

    @Bean
    public WorkerServers trackingWorkerServers(
            @Value("${strata.rabbitmq.tracking.port}") final int port,
            @Value("${strata.rabbitmq.tracking.username}") final String username,
            @Value("${strata.rabbitmq.tracking.password}") final String password,
            @Value("${strata.rabbitmq.tracking.virtualhost}") final String virtualHost) {
        return new WorkerServers("strata.rabbitmq.tracking.host", port, username, password, virtualHost);
    }

    @Bean
    public QueuePublishingService<TrackingEvent> trackingEventPublishingService(
            @Qualifier("trackingWorkerServers") final WorkerServers workerServers) {

        SpringAMQPRoutedQueuePublishingService publisher = factory.createPublisher(workerServers, exchangeName, queueName, routingKey, yazinoConfiguration);

        return new SafeQueuePublishingEventService<TrackingEvent>(publisher);
    }
}
