package com.yazino.engagement.campaign.configuration;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.engagement.PushNotificationMessage;
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
public class NotificationCampaignServiceConfiguration {

    private WorkerServiceFactory factory = new WorkerServiceFactory();

    @Value("${strata.rabbitmq.notification-campaign-ios.exchange}")
    private String iosExchangeName;

    @Value("${strata.rabbitmq.notification-campaign-android.exchange}")
    private String androidExchangeName;

    @Value("${strata.rabbitmq.notification-campaign-facebook.exchange}")
    private String facebookExchangeName;

    @Value("${strata.rabbitmq.notification-campaign.ios.queue}")
    private String iosQueueName;
    @Value("${strata.rabbitmq.notification-campaign.android.queue}")
    private String androidQueueName;
    @Value("${strata.rabbitmq.notification-campaign.facebook.queue}")
    private String facebookQueueName;

    @Value("${strata.rabbitmq.notification-campaign.routing-key}")
    private String routingKey;
    @Value("${strata.rabbitmq.notification-campaign.consumer-count}")
    private int consumerCount;

    @Value("${strata.rabbitmq.worker.port}")
    private int port;
    @Value("${strata.rabbitmq.worker.username}")
    private String username;
    @Value("${strata.rabbitmq.worker.password}")
    private String password;
    @Value("${strata.rabbitmq.worker.virtualhost}")
    private String virtualHost;

    @Autowired
    private YazinoConfiguration yazinoConfiguration;


    @Bean(name = "iosNotificationProducerWorkerServers")
    public WorkerServers iosNotificationProducerWorkerServers() {
        return createNewMultiHostWorkerServer();
    }

    @Bean(name = "iosNotificationConsumerWorkerServers")
    public WorkerServers iosNotificationConsumerWorkerServers() {
        return createNewSingleHostWorkerServer();
    }

    @Bean
    public SimpleMessageListenerContainer getIosNotificationCampaignMLContainer(
            @Qualifier("iosNotificationConsumerWorkerServers") final WorkerServers workerServers,
            @Qualifier("iosNotificationCampaignConsumer") final QueueMessageConsumer<PushNotificationMessage> iosConsumer) {
        return factory.startConcurrentConsumers(
                workerServers, yazinoConfiguration, iosExchangeName, iosQueueName, routingKey, iosConsumer, consumerCount, 1);
    }

    @Bean(name = "iosQueuePublishingService")
    public QueuePublishingService<PushNotificationMessage> notificationCampaignMessageQueuePublishingService(
            @Qualifier("iosNotificationProducerWorkerServers") final WorkerServers workerServers) {
        final SpringAMQPRoutedQueuePublishingService publisher = factory.createPublisher(
                workerServers, iosExchangeName, iosQueueName, routingKey, yazinoConfiguration);
        return new SafeQueuePublishingEventService<PushNotificationMessage>(publisher);
    }


    @Bean(name = "androidNotificationProducerWorkerServers")
    public WorkerServers androidNotificationProducerWorkerServers() {
        return createNewMultiHostWorkerServer();
    }

    @Bean(name = "androidNotificationConsumerWorkerServers")
    public WorkerServers androidNotificationConsumerWorkerServers() {
        return createNewSingleHostWorkerServer();
    }

    @Bean
    public SimpleMessageListenerContainer getAndroidNotificationCampaignMLContainer(
            @Qualifier("androidNotificationConsumerWorkerServers") final WorkerServers workerServers,
            @Qualifier("androidNotificationCampaignConsumer") final QueueMessageConsumer<PushNotificationMessage> androidConsumer) {
        return factory.startConcurrentConsumers(
                workerServers, yazinoConfiguration, androidExchangeName, androidQueueName, routingKey, androidConsumer, consumerCount, 1);
    }

    @Bean(name = "androidQueuePublishingService")
    public QueuePublishingService<PushNotificationMessage> androidNotificationCampaignMessageQueuePublishingService(
            @Qualifier("androidNotificationProducerWorkerServers") final WorkerServers workerServers) {
        final SpringAMQPRoutedQueuePublishingService publisher = factory.createPublisher(
                workerServers, androidExchangeName, androidQueueName, routingKey, yazinoConfiguration);
        return new SafeQueuePublishingEventService<PushNotificationMessage>(publisher);
    }


    @Bean(name = "facebookNotificationProducerWorkerServers")
    public WorkerServers facebookNotificationProducerWorkerServers() {
        return createNewMultiHostWorkerServer();
    }

    @Bean(name = "facebookNotificationConsumerWorkerServers")
    public WorkerServers facebookNotificationConsumerWorkerServers() {
        return createNewSingleHostWorkerServer();
    }

    @Bean
    public SimpleMessageListenerContainer getFacebookNotificationCampaignMLContainer(
            @Qualifier("facebookNotificationConsumerWorkerServers") final WorkerServers workerServers,
            @Qualifier("facebookNotificationCampaignConsumer") final QueueMessageConsumer<PushNotificationMessage> facebookConsumer) {
        return factory.startConcurrentConsumers(
                workerServers, yazinoConfiguration, facebookExchangeName, facebookQueueName, routingKey, facebookConsumer, consumerCount, 1);
    }

    @Bean(name = "facebookQueuePublishingService")
    public QueuePublishingService<PushNotificationMessage> facebookNotificationCampaignMessageQueuePublishingService(
            @Qualifier("facebookNotificationProducerWorkerServers") final WorkerServers workerServers) {
        final SpringAMQPRoutedQueuePublishingService publisher = factory.createPublisher(
                workerServers, facebookExchangeName, facebookQueueName, routingKey, yazinoConfiguration);
        return new SafeQueuePublishingEventService<PushNotificationMessage>(publisher);
    }


    private WorkerServers createNewMultiHostWorkerServer() {
        return new WorkerServers("strata.rabbitmq.campaign-notification.worker.hosts", port, username, password, virtualHost);
    }

    private WorkerServers createNewSingleHostWorkerServer() {
        return new WorkerServers("strata.rabbitmq.worker.host", port, username, password, virtualHost);
    }
}
