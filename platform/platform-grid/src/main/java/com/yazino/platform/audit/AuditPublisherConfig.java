package com.yazino.platform.audit;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.audit.message.*;
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
public class AuditPublisherConfig {

    @Autowired
    private YazinoConfiguration yazinoConfiguration;

    @Value("${strata.rabbitmq.audit.exchange}")
    private String exchangeName;

    private final WorkerServiceFactory factory = new WorkerServiceFactory();

    @Bean
    public WorkerServers auditWorkerServers(@Value("${strata.rabbitmq.audit.port}") final int port,
                                            @Value("${strata.rabbitmq.audit.username}") final String username,
                                            @Value("${strata.rabbitmq.audit.password}") final String password,
                                            @Value("${strata.rabbitmq.audit.virtualhost}") final String virtualHost) {
        return new WorkerServers("strata.rabbitmq.audit.host", port, username, password, virtualHost);
    }

    @Bean
    public QueuePublishingService<TransactionProcessedMessage> transactionProcessedMessageQueuePublishingService(
            @Qualifier("auditWorkerServers") final WorkerServers workerServers,
            @Value("${strata.rabbitmq.audit.transaction.queue}") final String queueName,
            @Value("${strata.rabbitmq.audit.transaction.routing-key}") final String routingKey) {
        return factory.createPublisher(workerServers, exchangeName, queueName, routingKey, yazinoConfiguration);
    }

    @Bean
    public QueuePublishingService<CommandAuditMessage> commandAuditMessageQueuePublishingService(
            @Qualifier("auditWorkerServers") final WorkerServers workerServers,
            @Value("${strata.rabbitmq.audit.command.queue}") final String queueName,
            @Value("${strata.rabbitmq.audit.command.routing-key}") final String routingKey) {
        return factory.createPublisher(workerServers, exchangeName, queueName, routingKey, yazinoConfiguration);
    }

    @Bean
    public QueuePublishingService<GameAuditMessage> gameAuditMessageQueuePublishingService(
            @Qualifier("auditWorkerServers") final WorkerServers workerServers,
            @Value("${strata.rabbitmq.audit.game.queue}") final String queueName,
            @Value("${strata.rabbitmq.audit.game.routing-key}") final String routingKey) {
        return factory.createPublisher(workerServers, exchangeName, queueName, routingKey, yazinoConfiguration);
    }

    @Bean
    public QueuePublishingService<ExternalTransactionMessage> externalTransactionMessageQueuePublishingService(
            @Qualifier("auditWorkerServers") final WorkerServers workerServers,
            @Value("${strata.rabbitmq.audit.externaltransaction.queue}") final String queueName,
            @Value("${strata.rabbitmq.audit.externaltransaction.routing-key}") final String routingKey) {
        return factory.createPublisher(workerServers, exchangeName, queueName, routingKey, yazinoConfiguration);
    }

    @Bean
    public QueuePublishingService<SessionKeyMessage> sessionKeyMessageQueuePublishingService(
            @Qualifier("auditWorkerServers") final WorkerServers workerServers,
            @Value("${strata.rabbitmq.audit.session.queue}") final String queueName,
            @Value("${strata.rabbitmq.audit.session.routing-key}") final String routingKey) {
        return factory.createPublisher(workerServers, exchangeName, queueName, routingKey, yazinoConfiguration);
    }
}
