package com.yazino.platform.messaging.publisher;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.messaging.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;

/**
 * A publisher that routes messages to a number of independent messaging hosts.
 */
public class SpringAMQPRoutedQueuePublishingService
        implements QueuePublishingService {
    private static final Logger LOG = LoggerFactory.getLogger(SpringAMQPRoutedQueuePublishingService.class);

    private final SpringAMQPRoutedTemplates templates;

    SpringAMQPRoutedQueuePublishingService(final SpringAMQPRoutedTemplates templates) {
        this.templates = templates;
    }

    public SpringAMQPRoutedQueuePublishingService(final String hostsPropertyName,
                                                  final int port,
                                                  final String virtualHost,
                                                  final String username,
                                                  final String password,
                                                  final CloneableRabbitTemplate sourceTemplate,
                                                  final YazinoConfiguration yazinoConfiguration) {
        templates = new SpringAMQPRoutedTemplates(hostsPropertyName,
                new ConnectionFactoryFactory(port, virtualHost, username, password),
                sourceTemplate,
                yazinoConfiguration);
    }

    @Override
    public void send(final Message message) {
        if (message == null) {
            LOG.warn("null message received, ignoring");
            return;
        }

        sendTo(message, templates.hostFor(message));
    }

    private void sendTo(final Message message, final String messagingHost) {
        try {
            templates.templateFor(messagingHost).convertAndSend(message, new MessagePostProcessor() {
                @Override
                public org.springframework.amqp.core.Message postProcessMessage(
                        final org.springframework.amqp.core.Message processedMessage) throws AmqpException {
                    processedMessage.getMessageProperties().setType(message.getMessageType().toString());
                    processedMessage.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return processedMessage;
                }
            });

        } catch (AmqpConnectException e) {
            LOG.warn("AMQP connection exception received, blacklisting {} and retrying", messagingHost, e);
            templates.blacklist(messagingHost);
            send(message);
        }
    }


}
