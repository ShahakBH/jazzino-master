package com.yazino.bi.opengraph;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.messaging.WorkerServers;
import com.yazino.platform.messaging.WorkerServiceFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenGraphConsumerConfiguration {

    private WorkerServiceFactory factory = new WorkerServiceFactory();

    @Value("${strata.rabbitmq.opengraph.exchange}")
    private String exchangeName;

    @Value("${strata.rabbitmq.opengraph.consumer-count}")
    private int consumerCount;

    @Value("${opengraph.accesstoken.capacity}")
    private int maxCapacity;

    @Autowired
    private YazinoConfiguration yazinoConfiguration;

    @Bean
    public WorkerServers openGraphWorkerServers(
            @Value("${strata.rabbitmq.opengraph.port}") final int port,
            @Value("${strata.rabbitmq.opengraph.username}") final String username,
            @Value("${strata.rabbitmq.opengraph.password}") final String password,
            @Value("${strata.rabbitmq.opengraph.virtualhost}") final String virtualHost) {
        return new WorkerServers("strata.rabbitmq.opengraph.host", port, username, password, virtualHost);
    }

    @Bean
    public SimpleMessageListenerContainer openGraphCredentialsEventConsumerContainer(
            @Value("${strata.rabbitmq.opengraph.credentials.queue}") final String queueName,
            @Value("${strata.rabbitmq.opengraph.credentials.routing-key}") final String routingKey,
            @Qualifier("openGraphWorkerServers") final WorkerServers workerServers,
            @Qualifier("openGraphCredentialsEventConsumer") final OpenGraphCredentialsEventConsumer consumer) {

        return factory.startConsumer(workerServers, yazinoConfiguration, exchangeName, queueName, routingKey, consumer, 1);
    }

    @Bean
    public SimpleMessageListenerContainer openGraphActionMessageConsumerContainer(
            @Value("${strata.rabbitmq.opengraph.action.queue}") final String queueName,
            @Value("${strata.rabbitmq.opengraph.action.routing-key}") final String routingKey,
            @Qualifier("openGraphWorkerServers") final WorkerServers workerServers,
            @Qualifier("openGraphActionMessageConsumer") final OpenGraphActionMessageConsumer consumer) {

        return factory.startConcurrentConsumers(
                workerServers, yazinoConfiguration, exchangeName, queueName, routingKey, consumer, consumerCount, 1);
    }

    @Bean
    public AccessTokenStore accessTokenStore() {
        return new AccessTokenStore(maxCapacity);
    }

}
