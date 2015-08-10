package com.yazino.web.service;

import com.yazino.configuration.YazinoConfiguration;
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
import strata.server.lobby.api.promotion.message.PromotionMessage;

@Configuration
public class PromotionRequestPublisherConfig {

    private final WorkerServiceFactory factory = new WorkerServiceFactory();
    @Value("${strata.rabbitmq.promotion.exchange}")
    private String exchangeName;
    @Value("${strata.rabbitmq.promotion.queue}")
    private String queueName;
    @Value("${strata.rabbitmq.promotion.routing-key}")
    private String routingKey;

    @Autowired
    private YazinoConfiguration yazinoConfiguration;

    @Bean
    public WorkerServers promotionRequestWorkerServers(
            @Value("${strata.rabbitmq.promotion.port}") final int port,
            @Value("${strata.rabbitmq.promotion.username}") final String username,
            @Value("${strata.rabbitmq.promotion.password}") final String password,
            @Value("${strata.rabbitmq.promotion.virtualhost}") final String virtualHost) {
        return new WorkerServers("strata.rabbitmq.promotion.host", port, username, password, virtualHost);
    }

    @Bean
    public QueuePublishingService<PromotionMessage> promotionRequestQueuePublishingService(
            @Qualifier("promotionRequestWorkerServers") final WorkerServers workerServers) {
        final SpringAMQPRoutedQueuePublishingService publisher =
                factory.createPublisher(workerServers, exchangeName, queueName, routingKey, yazinoConfiguration);
        return new SafeQueuePublishingEventService <PromotionMessage>(publisher);
    }
}
