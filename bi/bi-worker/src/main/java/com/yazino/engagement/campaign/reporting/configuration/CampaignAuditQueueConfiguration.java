package com.yazino.engagement.campaign.reporting.configuration;

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
public class CampaignAuditQueueConfiguration {
    private WorkerServiceFactory factory = new WorkerServiceFactory();

    @Value("${strata.rabbitmq.audit-campaign.exchange}")
    private String exchangeName;
    @Value("${strata.rabbitmq.audit-campaign.queue}")
    private String queueName;
    @Value("${strata.rabbitmq.audit-campaign.routing-key}")
    private String routingKey;
    @Value("${strata.rabbitmq.audit-campaign.consumer-count}")
    private int consumerCount;

    @Autowired
    private YazinoConfiguration yazinoConfiguration;

    @Bean
    public WorkerServers auditCampaignWorkerServers(
            @Value("${strata.rabbitmq.worker.port}") final int port,
            @Value("${strata.rabbitmq.worker.username}") final String username,
            @Value("${strata.rabbitmq.worker.password}") final String password,
            @Value("${strata.rabbitmq.worker.virtualhost}") final String virtualHost) {
        return new WorkerServers("strata.rabbitmq.worker.host", port, username, password, virtualHost);
    }

    @Bean
    public SimpleMessageListenerContainer getCampaignAuditMLContainer(
            @Qualifier("auditCampaignWorkerServers") final WorkerServers workerServers,
            @Qualifier("campaignAuditConsumer") final QueueMessageConsumer<CampaignRunMessage> consumer) {
        return factory.startConcurrentConsumers(
                workerServers, yazinoConfiguration, exchangeName, queueName, routingKey, consumer, consumerCount, 1);
    }

    @Bean(name = "auditCampaignMessageQueuePublishingService")
    public QueuePublishingService<CampaignRunMessage> auditCampaignMessageQueuePublishingService(
            @Qualifier("auditCampaignWorkerServers") final WorkerServers workerServers) {
        final SpringAMQPRoutedQueuePublishingService publisher = factory.createPublisher(
                workerServers, exchangeName, queueName, routingKey, yazinoConfiguration);
        return new SafeQueuePublishingEventService<CampaignRunMessage>(publisher);
    }

}
