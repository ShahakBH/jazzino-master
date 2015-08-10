package com.yazino.platform.event;

import com.yazino.platform.messaging.WorkerServers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventWorkerServersConfiguration {

    @Value("${strata.rabbitmq.event.port}")
    private int port;
    @Value("${strata.rabbitmq.event.username}")
    private String username;
    @Value("${strata.rabbitmq.event.password}")
    private String password;
    @Value("${strata.rabbitmq.event.virtualhost}")
    private String virtualHost;
    @Value("${strata.rabbitmq.event.exchange}")
    private String exchangeName;

    @Bean
    public WorkerServers eventWorkerServers() {
        return new WorkerServers("strata.rabbitmq.event.host",
                port,
                username,
                password,
                virtualHost);
    }
}
