package com.yazino.engagement.email.infrastructure;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.engagement.CampaignDeliverMessage;
import com.yazino.engagement.EmailCampaignDeliverMessage;
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
public class EmailCampaignDeliveryQueueConfiguration {
    private WorkerServiceFactory factory = new WorkerServiceFactory();

    @Value("${strata.rabbitmq.deliver-email-campaign.exchange}")
    private String exchangeName;
    @Value("${strata.rabbitmq.deliver-email-campaign.queue}")
    private String queueName;
    @Value("${strata.rabbitmq.deliver-email-campaign.routing-key}")
    private String routingKey;
    @Value("${strata.rabbitmq.deliver-email-campaign.consumer-count}")
    private int consumerCount;
    @Value("${strata.rabbitmq.deliver-email-campaign.redelivery-pause-seconds}")
    private Long howLongToPauseBeforeRedeliveringMessage;

    @Autowired
    private YazinoConfiguration yazinoConfiguration;

    @Bean
    public Long getHowLongToPauseBeforeRedeliveringMessage() {
        return howLongToPauseBeforeRedeliveringMessage * 1000;
    }

    @Bean
    public WorkerServers emailCampaignDeliveryWorkerServers(
            @Value("${strata.rabbitmq.worker.port}") final int port,
            @Value("${strata.rabbitmq.worker.username}") final String username,
            @Value("${strata.rabbitmq.worker.password}") final String password,
            @Value("${strata.rabbitmq.worker.virtualhost}") final String virtualHost) {
        return new WorkerServers("strata.rabbitmq.worker.host", port, username, password, virtualHost);
    }

    @Bean
    public SimpleMessageListenerContainer getEmailDeliveryCampaignMLContainer(
            @Qualifier("emailCampaignDeliveryWorkerServers") final WorkerServers workerServers,
            @Qualifier("emailCampaignDeliveryConsumer") final QueueMessageConsumer<CampaignDeliverMessage> consumer) {
        return factory.startConcurrentConsumers(
                workerServers, yazinoConfiguration, exchangeName, queueName, routingKey, consumer, consumerCount, 1);
    }

    // note need to make sure queue is empty it should be.
    @Bean(name = "emailCampaignDeliverMessageQueuePublishingService")
    public QueuePublishingService<EmailCampaignDeliverMessage> emailCampaignDeliverMessageQueuePublishingService(
            @Qualifier("emailCampaignDeliveryWorkerServers") final WorkerServers workerServers) {
        final SpringAMQPRoutedQueuePublishingService publisher = factory.createPublisher(
                workerServers, exchangeName, queueName, routingKey, yazinoConfiguration);
        return new SafeQueuePublishingEventService<EmailCampaignDeliverMessage>(publisher);
    }
}
