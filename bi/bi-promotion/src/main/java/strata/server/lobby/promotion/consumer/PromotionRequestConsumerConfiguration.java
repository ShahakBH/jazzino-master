package strata.server.lobby.promotion.consumer;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.messaging.WorkerServers;
import com.yazino.platform.messaging.WorkerServiceFactory;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import strata.server.lobby.api.promotion.message.PromotionMessage;

@Configuration
public class PromotionRequestConsumerConfiguration {
    private final WorkerServiceFactory factory = new WorkerServiceFactory();

    @Value("${strata.rabbitmq.promotion.exchange}")
    private String exchangeName;
    @Value("${strata.rabbitmq.promotion.queue}")
    private String queueName;
    @Value("${strata.rabbitmq.promotion.routing-key}")
    private String routingKey;
    @Value("${strata.rabbitmq.promotion.consumer-count}")
    private int consumerCount;

    @Autowired
    private YazinoConfiguration yazinoConfiguration;

    @Bean
    public WorkerServers promotionRequestConsumerWorkerServers(
            @Value("${strata.rabbitmq.promotion.port}") final int port,
            @Value("${strata.rabbitmq.promotion.username}") final String username,
            @Value("${strata.rabbitmq.promotion.password}") final String password,
            @Value("${strata.rabbitmq.promotion.virtualhost}") final String virtualHost) {
        return new WorkerServers("strata.rabbitmq.promotion.host", port, username, password, virtualHost);
    }

    @Bean
    public SimpleMessageListenerContainer promotionRequestMessageListenerContainer(
            @Qualifier("promotionRequestConsumerWorkerServers") final WorkerServers workerServers,
            @Qualifier("promotionRequestConsumer") final QueueMessageConsumer<PromotionMessage> consumer) {
        return factory.startConcurrentConsumers(
                workerServers, yazinoConfiguration, exchangeName, queueName, routingKey, consumer, consumerCount, 1);
    }
}
