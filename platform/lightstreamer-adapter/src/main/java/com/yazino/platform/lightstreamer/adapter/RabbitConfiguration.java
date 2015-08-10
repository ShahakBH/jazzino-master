package com.yazino.platform.lightstreamer.adapter;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfiguration {

    @Bean
    public Exchange playerExchange(@Value("${strata.rabbitmq.topic}") String exchange) {
        return new DirectExchange(exchange);
    }

}
