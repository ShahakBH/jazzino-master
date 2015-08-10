package com.yazino.platform.worker;

import com.yazino.platform.messaging.WorkerServers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlatformWorkerServersConfiguration {

    @Value("${strata.rabbitmq.platform.port}")
    private int port;
    @Value("${strata.rabbitmq.platform.username}")
    private String username;
    @Value("${strata.rabbitmq.platform.password}")
    private String password;
    @Value("${strata.rabbitmq.platform.virtualhost}")
    private String virtualHost;
    @Value("${strata.rabbitmq.platform.exchange}")
    private String exchangeName;

    @Bean
    public WorkerServers platformWorkerServers() {
        return new WorkerServers("strata.rabbitmq.platform.host",
                port,
                username,
                password,
                virtualHost);
    }
}
