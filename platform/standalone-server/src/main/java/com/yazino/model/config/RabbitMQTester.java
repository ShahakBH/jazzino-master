package com.yazino.model.config;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.TreeMap;

import static java.lang.String.valueOf;

@Component
public class RabbitMQTester {

    private static final String CAN_CONNECT = "(%s) Can connect?";
    private static final String CAN_ACCESS_EXCHANGE = "(%s) Can access exchange?";
    private static final String CAN_USE_QUEUES = "(%s) Can use queue?";
    private final String host;
    private final String virtualHost;

    @Autowired
    public RabbitMQTester(@Value("${standalone-server.rabbitmq.host}")
                          final String host,
                          @Value("${standalone-server.rabbitmq.virtualHost}")
                          final String virtualHost) {
        this.host = host;
        this.virtualHost = virtualHost;
    }

    public Map<String, String> runDiagnostics() {
        final Map<String, String> diagnostics = new TreeMap<String, String>();
        final CachingConnectionFactory serverConnectionFactory = new CachingConnectionFactory(host);
        serverConnectionFactory.setVirtualHost(virtualHost);
        serverConnectionFactory.setUsername("pat");
        serverConnectionFactory.setPassword("p0stm4n");

        testConfig(diagnostics, "Server", serverConnectionFactory);

        final CachingConnectionFactory clientConnectionFactory = new CachingConnectionFactory(host);
        clientConnectionFactory.setVirtualHost(virtualHost);
        clientConnectionFactory.setUsername("flash");
        clientConnectionFactory.setPassword("readonly");
        testConfig(diagnostics, "Client", clientConnectionFactory);

        return diagnostics;
    }

    private void testConfig(final Map<String, String> diagnostics,
                            final String configName,
                            final CachingConnectionFactory factory) {
        final String connectKey = String.format(CAN_CONNECT, configName);
        final RabbitAdmin admin;
        try {
            factory.createConnection();
            admin = new RabbitAdmin(factory);
            diagnostics.put(connectKey, valueOf(true));
        } catch (Exception e) {
            diagnostics.put(connectKey, valueOf(false));
            return;
        }

        final boolean canAccessExchange = tryDeclaringExchange(admin);
        diagnostics.put(String.format(CAN_ACCESS_EXCHANGE, configName), String.valueOf(canAccessExchange));

        final boolean canDeclareQueue = tryUsingQueue(admin);
        diagnostics.put(String.format(CAN_USE_QUEUES, configName), String.valueOf(canDeclareQueue));
    }

    private boolean tryUsingQueue(final RabbitAdmin admin) {
        try {
            final Queue queue = admin.declareQueue();
            final DirectExchange exchange = new DirectExchange("player-direct", true, false);
            admin.declareBinding(BindingBuilder.bind(queue).to(exchange).with("TEST"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean tryDeclaringExchange(final RabbitAdmin admin) {
        boolean result;
        try {
            admin.declareExchange(new DirectExchange("player-direct", false, false));
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
            result = false;
        }
        return result;
    }

}
