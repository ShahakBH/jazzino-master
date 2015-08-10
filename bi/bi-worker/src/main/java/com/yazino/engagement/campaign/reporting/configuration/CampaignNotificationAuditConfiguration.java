package com.yazino.engagement.campaign.reporting.configuration;

import com.yazino.bi.messaging.BIWorkerServiceFactory;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.engagement.campaign.reporting.domain.CampaignNotificationAuditMessage;
import com.yazino.platform.messaging.WorkerServers;
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
public class CampaignNotificationAuditConfiguration {


    private BIWorkerServiceFactory factory = new BIWorkerServiceFactory();

    @Value("${strata.rabbitmq.campaign-notification-audit.exchange}")
    private String exchangeName;
    @Value("${strata.rabbitmq.campaign-notification-audit.queue}")
    private String queueName;
    @Value("${strata.rabbitmq.campaign-notification-audit.routing-key}")
    private String routingKey;
    @Value("${strata.rabbitmq.campaign-notification-audit.consumer-count}")
    private int consumerCount;
    @Value("${strata.rabbitmq.campaign-notification-audit.batch-size}")
    private int batchSize;

    @Autowired
    private YazinoConfiguration yazinoConfiguration;

    @Bean
    public WorkerServers campaignNotificationAuditWorkerServers(
            @Value("${strata.rabbitmq.worker.port}") final int port,
            @Value("${strata.rabbitmq.worker.username}") final String username,
            @Value("${strata.rabbitmq.worker.password}") final String password,
            @Value("${strata.rabbitmq.worker.virtualhost}") final String virtualHost) {
        return new WorkerServers("strata.rabbitmq.worker.host", port, username, password, virtualHost);
    }

    @Bean
    public SimpleMessageListenerContainer getCampaignNotificationAuditMLContainer(
            @Qualifier("campaignNotificationAuditWorkerServers") final WorkerServers workerServers,
            @Qualifier("campaignNotificationAuditConsumer") final QueueMessageConsumer<CampaignNotificationAuditMessage> consumer) {


        return factory.startConcurrentConsumers(
                workerServers, yazinoConfiguration, exchangeName, queueName, routingKey, consumer, consumerCount, batchSize);
    }

    @Bean(name = "campaignNotificationAuditPublishingService")
    public QueuePublishingService<CampaignNotificationAuditMessage> campaignNotificationAuditPublishingService(
            @Qualifier("campaignNotificationAuditWorkerServers") final WorkerServers workerServers) {
        final SpringAMQPRoutedQueuePublishingService publisher = factory.createPublisher(
                workerServers, exchangeName, queueName, routingKey, yazinoConfiguration);
        return new SafeQueuePublishingEventService<CampaignNotificationAuditMessage>(publisher);
    }
}
