package strata.server.worker.audit;

import com.yazino.bi.messaging.BIWorkerServiceFactory;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.audit.message.ExternalTransactionMessage;
import com.yazino.platform.messaging.WorkerServers;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import strata.server.worker.audit.consumer.CommandAuditMessageConsumer;
import strata.server.worker.audit.consumer.GameAuditMessageConsumer;
import strata.server.worker.audit.consumer.SessionKeyMessageConsumer;
import strata.server.worker.audit.consumer.TransactionProcessedMessageConsumer;

@SuppressWarnings("unchecked")
public class AuditConsumerConfig {
    @Value("${strata.rabbitmq.audit.exchange}")
    private String exchangeName;

    @Autowired
    private YazinoConfiguration yazinoConfiguration;

    private final BIWorkerServiceFactory factory = new BIWorkerServiceFactory();

    @Bean
    public WorkerServers auditConsumerWorkerServers(@Value("${strata.rabbitmq.audit.port}") final int port,
                                                    @Value("${strata.rabbitmq.audit.username}") final String username,
                                                    @Value("${strata.rabbitmq.audit.password}") final String password,
                                                    @Value("${strata.rabbitmq.audit.virtualhost}") final String virtualHost) {
        return new WorkerServers("strata.rabbitmq.audit.host", port, username, password, virtualHost);
    }

    @Bean
    public SimpleMessageListenerContainer startTransactionProcessedMessageConsumer(
            @Qualifier("auditConsumerWorkerServers") final WorkerServers workerServers,
            @Value("${strata.rabbitmq.audit.transaction.queue}") final String queueName,
            @Value("${strata.rabbitmq.audit.transaction.routing-key}") final String routingKey,
            @Value("${strata.rabbitmq.audit.transaction.consumer-count}") final int consumerCount,
            @Value("${strata.rabbitmq.audit.transaction.batch-size}") final int batchSize,
            final TransactionProcessedMessageConsumer consumer) {
        return factory.startConcurrentConsumers(workerServers, yazinoConfiguration, exchangeName, queueName, routingKey,
                consumer, consumerCount, batchSize);
    }

    @Bean
    public SimpleMessageListenerContainer startCommandAuditMessageConsumer(
            @Qualifier("auditConsumerWorkerServers") final WorkerServers workerServers,
            @Value("${strata.rabbitmq.audit.command.queue}") final String queueName,
            @Value("${strata.rabbitmq.audit.command.routing-key}") final String routingKey,
            @Value("${strata.rabbitmq.audit.command.consumer-count}") final int consumerCount,
            @Value("${strata.rabbitmq.audit.command.batch-size}") final int batchSize,
            final CommandAuditMessageConsumer consumer) {
        return factory.startConcurrentConsumers(workerServers, yazinoConfiguration, exchangeName, queueName, routingKey,
                consumer, consumerCount, batchSize);
    }

    @Bean
    public SimpleMessageListenerContainer startGameAuditMessageConsumer(
            @Qualifier("auditConsumerWorkerServers") final WorkerServers workerServers,
            @Value("${strata.rabbitmq.audit.game.queue}") final String queueName,
            @Value("${strata.rabbitmq.audit.game.routing-key}") final String routingKey,
            @Value("${strata.rabbitmq.audit.game.consumer-count}") final int consumerCount,
            @Value("${strata.rabbitmq.audit.game.batch-size}") final int batchSize,
            final GameAuditMessageConsumer consumer) {
        return factory.startConcurrentConsumers(workerServers, yazinoConfiguration, exchangeName, queueName, routingKey,
                consumer, consumerCount, batchSize);
    }

    @Bean
    public SimpleMessageListenerContainer startExternalTransactionMessageConsumer(
            @Qualifier("auditConsumerWorkerServers") final WorkerServers workerServers,
            @Value("${strata.rabbitmq.audit.externaltransaction.queue}") final String queueName,
            @Value("${strata.rabbitmq.audit.externaltransaction.routing-key}") final String routingKey,
            @Value("${strata.rabbitmq.audit.externaltransaction.consumer-count}") final int consumerCount,
            @Value("${strata.rabbitmq.audit.externaltransaction.batch-size}") final int batchSize,
            @Qualifier("externalTransactionMessageConsumer")
            final QueueMessageConsumer<ExternalTransactionMessage> consumer) {
        // batching leaves a chance of multiple processing where one item in the batch fails,
        // therefore to guarantee a single entry we cannot batch
        return factory.startConcurrentConsumers(workerServers, yazinoConfiguration, exchangeName, queueName, routingKey,
                consumer, consumerCount, batchSize);
    }

    @Bean
    public SimpleMessageListenerContainer startSessionKeyMessageConsumer(
            @Qualifier("auditConsumerWorkerServers") final WorkerServers workerServers,
            @Value("${strata.rabbitmq.audit.session.queue}") final String queueName,
            @Value("${strata.rabbitmq.audit.session.routing-key}") final String routingKey,
            @Value("${strata.rabbitmq.audit.session.consumer-count}") final int consumerCount,
            @Value("${strata.rabbitmq.audit.session.batch-size}") final int batchSize,
            final SessionKeyMessageConsumer consumer) {
        return factory.startConcurrentConsumers(workerServers, yazinoConfiguration, exchangeName, queueName, routingKey,
                consumer, consumerCount, batchSize);
    }
}
