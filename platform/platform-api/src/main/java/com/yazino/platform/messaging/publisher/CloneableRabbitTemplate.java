package com.yazino.platform.messaging.publisher;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.MessagePropertiesConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.apache.commons.lang3.Validate.notNull;

public class CloneableRabbitTemplate extends RabbitTemplate {

    private String exchange;
    private String routingKey;
    private String queue;
    private String encoding;
    private long replyTimeout;
    private MessageConverter messageConverter;
    private MessagePropertiesConverter messagePropertiesConverter;

    public CloneableRabbitTemplate() {
        super();
    }

    public CloneableRabbitTemplate(final ConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    @Override
    public void setExchange(final String exchange) {
        this.exchange = exchange;
        super.setExchange(exchange);
    }

    @Override
    public void setRoutingKey(final String routingKey) {
        this.routingKey = routingKey;
        super.setRoutingKey(routingKey);
    }

    @Override
    public void setQueue(final String queue) {
        this.queue = queue;
        super.setQueue(queue);
    }

    @Override
    public void setEncoding(final String encoding) {
        this.encoding = encoding;
        super.setEncoding(encoding);
    }

    @Override
    public void setReplyTimeout(final long replyTimeout) {
        this.replyTimeout = replyTimeout;
        super.setReplyTimeout(replyTimeout);
    }

    @Override
    public void setMessageConverter(final MessageConverter messageConverter) {
        this.messageConverter = messageConverter;
        super.setMessageConverter(messageConverter);
    }

    @Override
    public void setMessagePropertiesConverter(final MessagePropertiesConverter messagePropertiesConverter) {
        this.messagePropertiesConverter = messagePropertiesConverter;
        super.setMessagePropertiesConverter(messagePropertiesConverter);
    }

    String getExchange() {
        return exchange;
    }

    String getRoutingKey() {
        return routingKey;
    }

    String getQueue() {
        return queue;
    }

    String getEncoding() {
        return encoding;
    }

    long getReplyTimeout() {
        return replyTimeout;
    }

    MessagePropertiesConverter getMessagePropertiesConverter() {
        return messagePropertiesConverter;
    }

    public CloneableRabbitTemplate newWith(
            final ConnectionFactory connectionFactory) {
        notNull(connectionFactory, "connectionFactory may not be null");

        final CloneableRabbitTemplate clonedTemplate = new CloneableRabbitTemplate(connectionFactory);
        clonedTemplate.setEncoding(encoding);
        clonedTemplate.setExchange(exchange);
        if (messageConverter != null) {
            clonedTemplate.setMessageConverter(messageConverter);
        }
        if (messagePropertiesConverter != null) {
            clonedTemplate.setMessagePropertiesConverter(messagePropertiesConverter);
        }
        if (queue != null) {
            clonedTemplate.setQueue(queue);
        }
        clonedTemplate.setReplyTimeout(replyTimeout);
        clonedTemplate.setRoutingKey(routingKey);
        clonedTemplate.setChannelTransacted(isChannelTransacted());

        return clonedTemplate;
    }

    @Override
    public String toString() {
        return "CloneableRabbitTemplate{"
                + "exchange='" + exchange + '\''
                + ", routingKey='" + routingKey + '\''
                + ", queue='" + queue + '\''
                + ", encoding='" + encoding + '\''
                + ", replyTimeout=" + replyTimeout
                + ", messageConverter=" + messageConverter
                + ", messagePropertiesConverter=" + messagePropertiesConverter
                + '}';
    }
}
