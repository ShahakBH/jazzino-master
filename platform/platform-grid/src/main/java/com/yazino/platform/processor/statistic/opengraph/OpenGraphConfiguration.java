package com.yazino.platform.processor.statistic.opengraph;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.messaging.WorkerServers;
import com.yazino.platform.messaging.WorkerServiceFactory;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.messaging.publisher.SafeQueuePublishingEventService;
import com.yazino.platform.messaging.publisher.SpringAMQPRoutedQueuePublishingService;
import com.yazino.platform.opengraph.OpenGraphActionMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenGraphConfiguration {

    private WorkerServiceFactory factory = new WorkerServiceFactory();

    @Autowired
    private YazinoConfiguration yazinoConfiguration;

    @Bean
    public WorkerServers openGraphWorkerServers(@Value("${strata.rabbitmq.opengraph.port}") final int port,
                                                @Value("${strata.rabbitmq.opengraph.username}") final String username,
                                                @Value("${strata.rabbitmq.opengraph.password}") final String password,
                                                @Value("${strata.rabbitmq.opengraph.virtualhost}") final String virtualHost) {
        return new WorkerServers("strata.rabbitmq.opengraph.host", port, username, password, virtualHost);
    }

    @Bean
    public DefaultStatisticToActionTransformer statisticEventOpenGraphActionTransformer() {
        return new DefaultStatisticToActionTransformer();
    }

    @Bean
    public QueuePublishingService<OpenGraphActionMessage> openGraphActionQueuePublishingService(
            @Value("${strata.rabbitmq.opengraph.exchange}") final String exchangeName,
            @Value("${strata.rabbitmq.opengraph.action.queue}") final String queueName,
            @Value("${strata.rabbitmq.opengraph.action.routing-key}") final String routingKey,
            @Qualifier("openGraphWorkerServers") final WorkerServers workerServers) {

        final SpringAMQPRoutedQueuePublishingService publisher = factory.createPublisher(
                workerServers, exchangeName, queueName, routingKey, yazinoConfiguration);
        return new SafeQueuePublishingEventService<OpenGraphActionMessage>(publisher);
    }

}
