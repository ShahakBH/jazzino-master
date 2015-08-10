package com.yazino.platform.event.emailmonitoring;

import com.yazino.platform.event.EventWorkerServersConfiguration;
import com.yazino.platform.messaging.WorkerServers;
import com.yazino.platform.messaging.WorkerServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;


@Configuration
@Import(EventWorkerServersConfiguration.class)
public class EmailMonitoringEventServiceConfiguration {

    private WorkerServiceFactory factory = new WorkerServiceFactory();

    @Autowired(required = true)
    @Qualifier("eventWorkerServers")
    private WorkerServers workerServers;

    @Value("${strata.rabbitmq.event.exchange}")
    private String exchangeName;

}
