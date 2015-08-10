package strata.server.lobby.promotion.consumer;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.event.message.PlayerPlayedEvent;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import com.yazino.platform.messaging.WorkerServers;
import com.yazino.platform.messaging.WorkerServiceFactory;
import com.yazino.platform.event.EventWorkerServersConfiguration;

@Configuration
@Import(EventWorkerServersConfiguration.class)
public class ConsumersConfiguration {

    private WorkerServiceFactory factory = new WorkerServiceFactory();

    @Autowired(required = true)
    @Qualifier("eventWorkerServers")
    private WorkerServers workerServers;

    @Value("${strata.rabbitmq.event.exchange}")
    private String exchangeName;

    @Autowired
    private YazinoConfiguration yazinoConfiguration;

    @Bean
    public SimpleMessageListenerContainer playerPlayedEventConsumerContainer(
            @Value("${strata.rabbitmq.event.played.queue}") final String queueName,
            @Value("${strata.rabbitmq.event.played.routing-key}") final String routingKey,
            @Qualifier("playerPlayedEventConsumer") final QueueMessageConsumer<PlayerPlayedEvent> consumer) {
        return factory.startConsumer(workerServers, yazinoConfiguration, exchangeName, queueName, routingKey, consumer, 1);
    }
}
