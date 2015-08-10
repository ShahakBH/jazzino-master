package com.yazino.engagement.campaign.configuration;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.engagement.campaign.domain.CampaignRunMessage;
import com.yazino.platform.messaging.WorkerServers;
import com.yazino.platform.messaging.WorkerServiceFactory;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.messaging.publisher.SafeQueuePublishingEventService;
import com.yazino.platform.messaging.publisher.SpringAMQPRoutedQueuePublishingService;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CampaignRunQueueConfiguration {
    private WorkerServiceFactory factory = new WorkerServiceFactory();

    @Value("${strata.rabbitmq.run-campaign.exchange}")
    private String exchangeName;
    @Value("${strata.rabbitmq.run-campaign.queue}")
    private String queueName;
    @Value("${strata.rabbitmq.run-campaign.routing-key}")
    private String routingKey;
    @Value("${strata.rabbitmq.run-campaign.consumer-count}")
    private int consumerCount;

    @Autowired
    private YazinoConfiguration yazinoConfiguration;

    @Bean
    public WorkerServers runCampaignWorkerServers(
            @Value("${strata.rabbitmq.worker.port}") final int port,
            @Value("${strata.rabbitmq.worker.username}") final String username,
            @Value("${strata.rabbitmq.worker.password}") final String password,
            @Value("${strata.rabbitmq.worker.virtualhost}") final String virtualHost) {
        return new WorkerServers("strata.rabbitmq.worker.host", port, username, password, virtualHost);
    }

    @Bean
    public SimpleMessageListenerContainer getNotificationCampaignMLContainer(
            @Qualifier("runCampaignWorkerServers") final WorkerServers workerServers,
            @Qualifier("campaignRunConsumer") final QueueMessageConsumer<CampaignRunMessage> consumer) {
        return factory.startConcurrentConsumers(
                workerServers, yazinoConfiguration, exchangeName, queueName, routingKey, consumer, consumerCount, 1);
    }

    @Bean(name = "runCampaignMessageQueuePublishingService")
    public QueuePublishingService<CampaignRunMessage> runCampaignMessageQueuePublishingService(
            @Qualifier("runCampaignWorkerServers") final WorkerServers workerServers) {
        final SpringAMQPRoutedQueuePublishingService publisher = factory.createPublisher(
                workerServers, exchangeName, queueName, routingKey, yazinoConfiguration);
        return new SafeQueuePublishingEventService<CampaignRunMessage>(publisher);
    }
}
