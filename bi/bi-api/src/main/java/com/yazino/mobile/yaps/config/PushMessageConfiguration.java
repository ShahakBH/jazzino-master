package com.yazino.mobile.yaps.config;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.mobile.yaps.message.PushMessage;
import com.yazino.platform.messaging.WorkerServers;
import com.yazino.platform.messaging.WorkerServiceFactory;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.messaging.publisher.SafeQueuePublishingEventService;
import com.yazino.platform.messaging.publisher.SpringAMQPRoutedQueuePublishingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PushMessageConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(PushMessageConfiguration.class);

    private WorkerServiceFactory factory = new WorkerServiceFactory();

    @Value("${strata.rabbitmq.yaps.pushmessage-queue.name}")
    private String name;
    @Value("${strata.rabbitmq.yaps.pushmessage-queue.durable}")
    private boolean durable;
    @Value("${strata.rabbitmq.yaps.pushmessage-queue.auto-delete}")
    private boolean autoDelete;
    @Value("${strata.rabbitmq.yaps.pushmessage-queue.exclusive}")
    private boolean exclusive;
    @Value("${strata.rabbitmq.yaps.pushmessage-queue.routing-key}")
    private String routingKey;
    @Value("${strata.rabbitmq.yaps.pushmessage.consumer-count}")
    private String consumerCountAsString;

    @Value("${strata.rabbitmq.yaps.pushmessage.exchange.name}")
    private String exchangeName;

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

    @Bean(name = "pushMessageQueue")
    public Queue queue() {
        LOG.info("Configured pushMessageQueue with name {}, durable {}, exclusive {}, autodelete {}", name, durable, autoDelete, exclusive);
        return new Queue(name, durable, exclusive, autoDelete);
    }

    @Bean(name = "pushWorkerServers")
    public WorkerServers notificationCampaignWorkerServers() {
        return createNewWorkerServer();
    }

    @Bean(name = "pushQueuePublishingService")
    public QueuePublishingService<PushMessage> pushQueuePublishingService(
            @Qualifier("pushWorkerServers") final WorkerServers workerServers) {
        final SpringAMQPRoutedQueuePublishingService publisher = factory.createPublisher(
                workerServers, exchangeName, name, routingKey, yazinoConfiguration);
        return new SafeQueuePublishingEventService<PushMessage>(publisher);
    }

    private WorkerServers createNewWorkerServer() {
        return new WorkerServers("strata.rabbitmq.worker.host", port, username, password, virtualHost);
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public YazinoConfiguration getYazinoConfiguration() {
        return yazinoConfiguration;
    }

    public String getQueueName() {
        return name;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public int getConsumerCount() {
        return Integer.parseInt(consumerCountAsString);
    }

    public WorkerServiceFactory getFactory() {
        return factory;
    }

}
