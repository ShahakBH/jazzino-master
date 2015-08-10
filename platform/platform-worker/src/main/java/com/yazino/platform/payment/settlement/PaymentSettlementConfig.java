package com.yazino.platform.payment.settlement;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.messaging.WorkerServers;
import com.yazino.platform.messaging.WorkerServiceFactory;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentSettlementConfig {

    @Autowired
    private YazinoConfiguration yazinoConfiguration;

    @Value("${strata.rabbitmq.payment-settlement.exchange}")
    private String exchangeName;

    private final WorkerServiceFactory factory = new WorkerServiceFactory();

    @Bean
    public WorkerServers paymentSettlementWorkerServers(@Value("${strata.rabbitmq.payment-settlement.port}") final int port,
                                                        @Value("${strata.rabbitmq.payment-settlement.username}") final String username,
                                                        @Value("${strata.rabbitmq.payment-settlement.password}") final String password,
                                                        @Value("${strata.rabbitmq.payment-settlement.virtualhost}") final String virtualHost) {
        return new WorkerServers("strata.rabbitmq.payment-settlement.host", port, username, password, virtualHost);
    }

    @SuppressWarnings("unchecked")
    @Bean
    public QueuePublishingService<PaymentSettlementRequest> paymentSettlementQueuePublishingService(
            @Qualifier("paymentSettlementWorkerServers") final WorkerServers workerServers,
            @Value("${strata.rabbitmq.payment-settlement.queue}") final String queueName,
            @Value("${strata.rabbitmq.payment-settlement.routing-key}") final String routingKey) {
        return factory.createPublisher(workerServers, exchangeName, queueName, routingKey, yazinoConfiguration);
    }

    @Bean
    public SimpleMessageListenerContainer startPaymentSettlementRequestConsumer(
            @Qualifier("paymentSettlementWorkerServers") final WorkerServers workerServers,
            @Value("${strata.rabbitmq.payment-settlement.queue}") final String queueName,
            @Value("${strata.rabbitmq.payment-settlement.routing-key}") final String routingKey,
            @Value("${strata.rabbitmq.payment-settlement.consumer-count}") final int consumerCount,
            @Value("${strata.rabbitmq.payment-settlement.batch-size}") final int batchSize,
            final PaymentSettlementRequestConsumer consumer) {
        return factory.startConcurrentConsumers(workerServers, yazinoConfiguration, exchangeName, queueName, routingKey,
                consumer, consumerCount, batchSize);
    }

}
