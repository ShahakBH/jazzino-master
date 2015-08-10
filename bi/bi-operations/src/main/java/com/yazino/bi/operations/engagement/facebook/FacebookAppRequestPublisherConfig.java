package com.yazino.bi.operations.engagement.facebook;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.messaging.WorkerServers;
import com.yazino.platform.messaging.WorkerServiceFactory;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FacebookAppRequestPublisherConfig {

    private final WorkerServiceFactory factory = new WorkerServiceFactory();
    @Value("${strata.rabbitmq.fb-app-to-user-request.exchange}")
    private String exchangeName;
    @Value("${strata.rabbitmq.fb-app-to-user-request.queue}")
    private String queueName;
    @Value("${strata.rabbitmq.fb-app-to-user-request.routing-key}")
    private String routingKey;

    @Autowired
    private YazinoConfiguration yazinoConfiguration;

    @Bean
    public WorkerServers facebookAppRequestWorkerServers(
            @Value("${strata.rabbitmq.fb-app-to-user-request.port}") final int port,
            @Value("${strata.rabbitmq.fb-app-to-user-request.username}") final String username,
            @Value("${strata.rabbitmq.fb-app-to-user-request.password}") final String password,
            @Value("${strata.rabbitmq.fb-app-to-user-request.virtualhost}") final String virtualHost) {
        return new WorkerServers("strata.rabbitmq.fb-app-to-user-request.host", port, username, password, virtualHost);
    }

    @Bean
    public QueuePublishingService fbAppToUserRequestQueuePublishingService(
            @Qualifier("facebookAppRequestWorkerServers") final WorkerServers workerServers) {
        return factory.createPublisher(workerServers, exchangeName, queueName, routingKey, yazinoConfiguration);
    }
}
