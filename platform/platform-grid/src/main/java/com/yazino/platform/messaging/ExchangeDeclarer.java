package com.yazino.platform.messaging;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.Validate.notNull;

public class ExchangeDeclarer {
    private static final Logger LOG = LoggerFactory.getLogger(ExchangeDeclarer.class);

    private static final int RABBITMQ_PORT = 5672;
    private static final boolean DURABLE = true;
    private static final boolean DO_NOT_AUTO_DELETE = false;

    private final List<String> hosts = new ArrayList<String>();
    private final int port;
    private final String virtualHost;
    private final String exchangeName;
    private final String username;
    private final String password;

    public ExchangeDeclarer(final String hosts,
                            final int port,
                            final String virtualHost,
                            final String exchangeName,
                            final String username,
                            final String password) {
        notNull(hosts, "hosts may not be null");
        notNull(virtualHost, "virtualHost may not be null");
        notNull(exchangeName, "exchangeName may not be null");

        this.hosts.addAll(asList(hosts.split(",")));
        this.port = port;
        this.virtualHost = virtualHost;
        this.exchangeName = exchangeName;
        this.username = username;
        this.password = password;
    }

    public static void main(final String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: <hosts> <virtual-host>");
            System.exit(-1);
        }

        final String hosts = StringUtils.join(Arrays.asList(args).subList(0, args.length - 1), ',');
        final String virtualHost = args[args.length - 1];

        new ExchangeDeclarer(hosts, RABBITMQ_PORT, virtualHost, "player-direct", "pat", "p0stm4n").declare();

        System.exit(0);
    }

    public void declare() {
        for (String host : hosts) {
            final CachingConnectionFactory factory = new CachingConnectionFactory(host, port);
            factory.setUsername(username);
            factory.setPassword(password);
            factory.setVirtualHost(virtualHost);

            try {
//                declareExchangeFor(factory);
                final Connection connection = factory.createConnection();
                connection.close();

                LOG.info("Exchange {} created on host {} / virtual host {}", exchangeName, host, virtualHost);

            } catch (Exception e) {
                LOG.error("Exchange {} creation failed on host {} / virtual host {}", exchangeName, host, virtualHost, e);
            }
        }
    }

    private void declareExchangeFor(final CachingConnectionFactory factory) throws Exception {
        final RabbitAdmin admin = new RabbitAdmin(factory);
        try {
            admin.declareExchange(new DirectExchange(exchangeName, DURABLE, DO_NOT_AUTO_DELETE));

        } catch (Exception e) {
            // Rabbit reply 406 = PRECONDITION_FAILED, probably differing durable value
            if (rootMessageOf(e).contains("reply-code=406")) {
                LOG.debug("Exchange {} already exists, attempting delete and recreation", exchangeName);

                if (!admin.deleteExchange(exchangeName)) {
                    LOG.error("Failed to delete exchange {}, settings may not match", exchangeName);
                } else {
                    admin.declareExchange(new DirectExchange(exchangeName, DURABLE, DO_NOT_AUTO_DELETE));
                }

            } else {
                throw e;
            }
        }
    }

    private String rootMessageOf(final Exception e) {
        Throwable root = e;
        if (root != null) {
            while (root.getCause() != null) {
                root = root.getCause();
            }
        }

        if (root != null && root.getMessage() != null) {
            return root.getMessage();
        }
        return "";
    }
}
