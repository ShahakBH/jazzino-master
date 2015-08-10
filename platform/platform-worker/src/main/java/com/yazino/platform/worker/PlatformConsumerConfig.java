package com.yazino.platform.worker;

import com.yazino.platform.messaging.WorkerServers;
import com.yazino.platform.messaging.WorkerServiceFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

@SuppressWarnings("unchecked")
public class PlatformConsumerConfig {

    private final WorkerServiceFactory factory = new WorkerServiceFactory();

    @Value("${strata.rabbitmq.platform.exchange}")
    private String exchangeName;

    @Bean
    public WorkerServers platformConsumerWorkerServers(
            @Value("${strata.rabbitmq.platform.port}") final int port,
            @Value("${strata.rabbitmq.platform.username}") final String username,
            @Value("${strata.rabbitmq.platform.password}") final String password,
            @Value("${strata.rabbitmq.platform.virtualhost}") final String virtualHost) {
        return new WorkerServers("strata.rabbitmq.platform.host", port, username, password, virtualHost);
    }
}
