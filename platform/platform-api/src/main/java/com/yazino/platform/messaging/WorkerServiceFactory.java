package com.yazino.platform.messaging;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import com.yazino.platform.messaging.consumer.SpringAMQPMessageListener;
import com.yazino.platform.messaging.publisher.CloneableRabbitTemplate;
import com.yazino.platform.messaging.publisher.SpringAMQPRoutedQueuePublishingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.rabbit.support.DefaultMessagePropertiesConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.util.ErrorHandler;

import java.util.ArrayList;
import java.util.List;

public class WorkerServiceFactory {
    private static final Logger LOG = LoggerFactory.getLogger(WorkerServiceFactory.class);

    public static final boolean DURABLE = true;
    public static final boolean EXCLUSIVE = false;
    public static final boolean AUTO_DELETE = false;

    private final DefaultMessagePropertiesConverter messagePropertiesConverter
            = new DefaultMessagePropertiesConverter();

    private MessageConverter messageConverter = new Jackson2JodaJsonMessageConverter();

    private final ErrorHandler errorHandler = new SLF4JLoggingErrorHandler();

    public SpringAMQPRoutedQueuePublishingService createPublisher(final WorkerServers servers,
                                                                  final String exchangeName,
                                                                  final String queueName,
                                                                  final String routingKey,
                                                                  final YazinoConfiguration yazinoConfiguration) {

        final List<CachingConnectionFactory> factories = createConnectionFactories(servers, yazinoConfiguration, 1);
        if (factories.isEmpty()) {
            throw new IllegalStateException("No factories available for servers " + servers);
        }

        declareEverything(exchangeName, queueName, routingKey, factories);

        final CloneableRabbitTemplate template = new CloneableRabbitTemplate();
        template.setConnectionFactory(factories.get(0));
        template.setEncoding("UTF-8");
        template.setMessageConverter(messageConverter);
        template.setMessagePropertiesConverter(messagePropertiesConverter);
        template.setExchange(exchangeName);
        template.setQueue(queueName);
        template.setRoutingKey(routingKey);

        return new SpringAMQPRoutedQueuePublishingService(servers.getHostsPropertyName(),
                servers.getPort(),
                servers.getVirtualHost(),
                servers.getUsername(),
                servers.getPassword(),
                template,
                yazinoConfiguration);
    }

    public SimpleMessageListenerContainer startConsumer(final WorkerServers servers,
                                                        final YazinoConfiguration yazinoConfiguration,
                                                        final String exchangeName,
                                                        final String queueName,
                                                        final String routingKey,
                                                        final QueueMessageConsumer<?> consumerQueue,
                                                        final int batchSize) {
        return startConcurrentConsumers(servers, yazinoConfiguration, exchangeName, queueName, routingKey, consumerQueue, 1, batchSize);
    }

    public SimpleMessageListenerContainer startConcurrentConsumers(final WorkerServers servers,
                                                                   final YazinoConfiguration yazinoConfiguration,
                                                                   final String exchangeName,
                                                                   final String queueName,
                                                                   final String routingKey,
                                                                   final QueueMessageConsumer<?> consumerQueue,
                                                                   final int consumerCount,
                                                                   final int batchSize) {
        final List<CachingConnectionFactory> factories = createConnectionFactories(servers, yazinoConfiguration, consumerCount);

        declareEverything(exchangeName, queueName, routingKey, factories);

        final SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(factories.get(0));
        container.setAcknowledgeMode(AcknowledgeMode.AUTO);
        container.setConnectionFactory(factories.get(0));
        container.setQueueNames(queueName);
        container.setConcurrentConsumers(consumerCount);
        final SpringAMQPMessageListener delegate = new SpringAMQPMessageListener(consumerQueue);
        container.setMessageListener(new MessageListenerAdapter(delegate, messageConverter));
        container.setChannelTransacted(false);
        container.setAutoStartup(true);
        container.setPrefetchCount(batchSize);
        container.setTxSize(batchSize);
        container.setErrorHandler(errorHandler);
        return container;
    }

    private void declareEverything(final String exchangeName,
                                   final String queueName,
                                   final String routingKey,
                                   final List<CachingConnectionFactory> connectionFactories) {
        for (CachingConnectionFactory connectionFactory : connectionFactories) {
            final Queue queue = new Queue(queueName, DURABLE, EXCLUSIVE, AUTO_DELETE);
            final Exchange exchange = new DirectExchange(exchangeName, DURABLE, AUTO_DELETE);
            final Binding binding = BindingBuilder.bind(queue)
                    .to(exchange)
                    .with(routingKey)
                    .noargs();
            final RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
            rabbitAdmin.declareExchange(exchange);
            rabbitAdmin.declareQueue(queue);
            rabbitAdmin.declareBinding(binding);
        }
    }

    private List<CachingConnectionFactory> createConnectionFactories(final WorkerServers servers,
                                                                     final YazinoConfiguration yazinoConfiguration,
                                                                     final int consumerCount) {
        final List<CachingConnectionFactory> result = new ArrayList<>();
        final String[] hosts = yazinoConfiguration.getStringArray(servers.getHostsPropertyName());
        if (hosts != null) {
            for (String host : hosts) {
                final CachingConnectionFactory connectionFactory = new CachingConnectionFactory(host.trim(),
                        servers.getPort());
                connectionFactory.setUsername(servers.getUsername());
                connectionFactory.setPassword(servers.getPassword());
                connectionFactory.setVirtualHost(servers.getVirtualHost());
                if (connectionFactory.getChannelCacheSize() < consumerCount) {
                    connectionFactory.setChannelCacheSize(consumerCount);
                }
                result.add(connectionFactory);
            }
        } else {
            LOG.error("Could not find any hosts for property {}", servers.getHostsPropertyName());
        }
        return result;
    }

}
