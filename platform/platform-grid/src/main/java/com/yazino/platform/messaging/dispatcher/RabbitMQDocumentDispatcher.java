package com.yazino.platform.messaging.dispatcher;

import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.messaging.RabbitMQDirectRoutingKeyWorker;
import com.yazino.platform.messaging.RabbitMQRoutingKeyWorker;
import com.yazino.platform.messaging.publisher.SpringAMQPRoutedTemplates;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

public class RabbitMQDocumentDispatcher implements DocumentDispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(RabbitMQDocumentDispatcher.class);

    private static final RabbitMQDirectRoutingKeyWorker ROUTING_KEY_WORKER = new RabbitMQDirectRoutingKeyWorker();

    private final SpringAMQPRoutedTemplates templates;

    @Autowired(required = true)
    public RabbitMQDocumentDispatcher(
            @Qualifier("routedTemplates") final SpringAMQPRoutedTemplates templates) {
        notNull(templates, "routedTemplates is null");
        this.templates = templates;
    }

    public void dispatch(final Document document) {
        dispatch(document, (BigDecimal) null);
    }

    public void dispatch(final Document document, final BigDecimal playerId) {
        final byte[] messageBodyBytes = getBodyBytes(document);
        final MessageProperties messageProperties = buildMessageProperties(document);
        final String routingKey = resolveRoutingKey(document, playerId, ROUTING_KEY_WORKER);
        if (LOG.isDebugEnabled()) {
            LOG.debug("dispatching document to {} with routingKey {} | {}", playerId, routingKey, document.getBody());
        }
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

    private MessageProperties buildMessageProperties(final Document document) {
        final MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType(document.getType());
        if (document.getEncoding() != null) {
            messageProperties.setContentEncoding(document.getEncoding());
        }
        return messageProperties;
    }

    private byte[] getBodyBytes(final Document document) {
        try {
            return document.getBody().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unsupported encoding for document: " + document.getBody(), e);
        }
    }

    public void dispatch(final Document document, final Set<BigDecimal> playerIds) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("dispatching document " + ReflectionToStringBuilder.reflectionToString(document));
        }
        for (BigDecimal playerId : playerIds) {
            dispatch(document, playerId);
        }
    }

    private String resolveRoutingKey(final Document document,
                                     final BigDecimal playerId,
                                     final RabbitMQRoutingKeyWorker routingKeyWorker) {
        final String filteredPlayer;
        if (playerId == null) {
            filteredPlayer = "";
        } else {
            filteredPlayer = playerId.toString().replace(".", "_");
        }
        String tableId = document.getHeaders().get("table");
        if (tableId == null) {
            tableId = document.getHeaders().get("locationId");
        }
        final String filteredTable;
        if (tableId == null) {
            filteredTable = "";
        } else {
            filteredTable = tableId.replace(".", "_");
        }
        return routingKeyWorker.getRoutingKey(document.getType(), filteredPlayer, filteredTable);
    }
}
