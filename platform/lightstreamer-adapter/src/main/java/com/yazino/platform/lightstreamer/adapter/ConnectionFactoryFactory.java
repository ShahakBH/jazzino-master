package com.yazino.platform.lightstreamer.adapter;

import com.yazino.configuration.YazinoConfiguration;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.apache.commons.lang3.Validate.notNull;

@Service
public class ConnectionFactoryFactory {
    private static final String PROPERTY_PORT = "strata.rabbitmq.port";
    private static final String PROPERTY_USERNAME = "strata.rabbitmq.username";
    private static final String PROPERTY_PASSWORD = "strata.rabbitmq.password";
    private static final String PROPERTY_VIRTUALHOST = "strata.rabbitmq.virtualhost";
    private static final String PROPERTY_CACHE_SIZE = "strata.rabbitmq.channelCacheSize";

    private static final int DEFAULT_PORT = 5672;
    private static final int DEFAULT_CACHE_SIZE = 1;

    private final YazinoConfiguration yazinoConfiguration;

    @Autowired
    public ConnectionFactoryFactory(final YazinoConfiguration yazinoConfiguration) {
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");

        this.yazinoConfiguration = yazinoConfiguration;
    }

    public ConnectionFactory forHost(final String hostname) {
        final CachingConnectionFactory factory = new CachingConnectionFactory(hostname, yazinoConfiguration.getInt(PROPERTY_PORT, DEFAULT_PORT));
        factory.setChannelCacheSize(yazinoConfiguration.getInt(PROPERTY_CACHE_SIZE, DEFAULT_CACHE_SIZE));
        factory.setVirtualHost(yazinoConfiguration.getString(PROPERTY_VIRTUALHOST));
        factory.setUsername(yazinoConfiguration.getString(PROPERTY_USERNAME));
        factory.setPassword(yazinoConfiguration.getString(PROPERTY_PASSWORD));
        return factory;
    }

    public boolean isAvailable(final String hostname) {
        try {
            closeQuietly(forHost(hostname).createConnection());
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    private void closeQuietly(final Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception ignored) {
                // ignored
            }
        }
    }

}
