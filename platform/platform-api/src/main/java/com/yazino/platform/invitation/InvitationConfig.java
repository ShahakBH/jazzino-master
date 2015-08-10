package com.yazino.platform.invitation;

import com.yazino.platform.messaging.Jackson2JodaJsonMessageConverter;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 * We need to use a config bean here, as the rabbit namespace doesn't handle two
 * exchanges with the same name (but different IDs). So either we lose our
 * configuration option to split them, or we do this!
 */
@Configuration
public class InvitationConfig {

    @Value("${strata.rabbitmq.invitations.queue}")
    private String queueName;
    @Value("${strata.rabbitmq.invitations.queue.durable}")
    private boolean queueDurable;
    @Value("${strata.rabbitmq.invitations.queue.auto-delete}")
    private boolean queueAutoDelete;
    @Value("${strata.rabbitmq.invitations.queue.exclusive}")
    private boolean queueExclusive;

    @Value("${strata.rabbitmq.invitations.exchange}")
    private String exchangeName;
    @Value("${strata.rabbitmq.invitations.exchange.durable}")
    private boolean exchangeDurable;
    @Value("${strata.rabbitmq.invitations.exchange.auto-delete}")
    private boolean exchangeAutoDelete;

    @Value("${strata.rabbitmq.invitations.routing-key}")
    private String routingKey;

    @Value("${strata.rabbitmq.invitations.host}")
    private String host;
    @Value("${strata.rabbitmq.invitations.port}")
    private int port;
    @Value("${strata.rabbitmq.invitations.username}")
    private String username;
    @Value("${strata.rabbitmq.invitations.password}")
    private String password;
    @Value("${strata.rabbitmq.invitations.virtualhost}")
    private String virtualHost;

    @Bean
    public AmqpAdmin invitationsAdmin() {
        return new RabbitAdmin(invitationsConnectionFactory());
    }

    @Bean
    public Exchange invitationsExchange() {
        return new DirectExchange(exchangeName, exchangeDurable, exchangeAutoDelete);
    }

    @Bean
    public Queue invitationsQueue() {
        return new Queue(queueName, queueDurable, queueAutoDelete, queueExclusive);
    }

    @Bean
    public Binding invitationsBinding() {
        return BindingBuilder.bind(invitationsQueue())
                .to(invitationsExchange())
                .with(routingKey).noargs();
    }

    @Bean
    public MessageConverter invitationsConverter() {
        return new Jackson2JodaJsonMessageConverter();
    }

    @Bean
    public ConnectionFactory invitationsConnectionFactory() {
        final CachingConnectionFactory connectionFactory = new CachingConnectionFactory(host, port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setVirtualHost(virtualHost);
        return connectionFactory;
    }
}
