package com.yazino.platform.messaging.dispatcher;

import com.yazino.platform.messaging.publisher.SpringAMQPRoutedTemplates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@Component("playerDocumentDispatcher")
public class PlayerDocumentDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(PlayerDocumentDispatcher.class);

    private final SpringAMQPRoutedTemplates templates;

    @Autowired
    public PlayerDocumentDispatcher(
            @Qualifier("playerRabbitMQRoutedPublishers") final SpringAMQPRoutedTemplates templates) {
        notNull(templates, "publishers is null");
        this.templates = templates;
    }

    public void dispatch(final BigDecimal playerId, final String documentType, final String documentBody) {
        LOG.debug("Dispatching {} for {}: {}", documentType, playerId, documentBody);
        final byte[] messageBodyBytes;
        try {
            messageBodyBytes = documentBody.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            final String message = String.format("Could not send message due to encoding errors. Message= %s",
                    documentBody);
            LOG.error(message, e);
            throw new RuntimeException(message, e);
        }
        final String routingKey = "PLAYER." + playerId;
        final MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType(documentType);

        send(playerId, routingKey, new Message(messageBodyBytes, messageProperties));
    }

    private void send(final BigDecimal playerId, final String routingKey, final Message message) {
        final String messagingHost = templates.hostFor(playerId);
        try {
            templates.templateFor(messagingHost).send(routingKey, message);

        } catch (AmqpConnectException e) {
            LOG.warn("AMQP connection exception received, blacklisting {} and retrying", messagingHost, e);
            templates.blacklist(messagingHost);
            send(playerId, routingKey, message);
        }
    }

}
