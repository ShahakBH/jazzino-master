package com.yazino.platform.email;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.email.message.EmailMessage;
import com.yazino.platform.messaging.WorkerServers;
import com.yazino.platform.messaging.WorkerServiceFactory;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SuppressWarnings("unchecked")
@Configuration
public class EmailPublisherConfig {

    @Value("${strata.rabbitmq.email.exchange}")
    private String exchangeName;

    @Autowired
    private YazinoConfiguration yazinoConfiguration;

    private final WorkerServiceFactory factory = new WorkerServiceFactory();

    @Bean
    public WorkerServers emailWorkerServers(@Value("${strata.rabbitmq.email.port}")
                                            final int port,
                                            @Value("${strata.rabbitmq.email.username}")
                                            final String username,
                                            @Value("${strata.rabbitmq.email.password}")
                                            final String password,
                                            @Value("${strata.rabbitmq.email.virtualhost}")
                                            final String virtualHost) {
        return new WorkerServers("strata.rabbitmq.email.host", port, username, password, virtualHost);
    }

    @Bean
    public QueuePublishingService<EmailMessage> emailQueuePublishingService(
            @Qualifier("emailWorkerServers") final WorkerServers workerServers,
            @Value("${strata.rabbitmq.email.queue}") final String queueName,
            @Value("${strata.rabbitmq.email.routing-key}") final String routingKey) {
        return factory.createPublisher(workerServers, exchangeName, queueName, routingKey, yazinoConfiguration);
    }
}
