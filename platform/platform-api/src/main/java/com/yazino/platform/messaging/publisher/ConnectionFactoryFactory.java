package com.yazino.platform.messaging.publisher;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.Connection;

import static org.apache.commons.lang3.Validate.*;

public class ConnectionFactoryFactory {
    private static final int MAX_PORT = 65535;
    private static final int MIN_PORT = 0;

    private final int port;
    private final String virtualHost;
    private final String username;
    private final String password;

    public ConnectionFactoryFactory(final int port,
                                    final String virtualHost,
                                    final String username,
                                    final String password) {
        notBlank(virtualHost, "virtualHost may not be null/blank");
        inclusiveBetween(MIN_PORT, MAX_PORT, port, String.format("port must be %d <= port <= %d", MIN_PORT, MAX_PORT));

        this.port = port;
        this.virtualHost = virtualHost;
        this.username = username;
        this.password = password;
    }

    public CachingConnectionFactory forHost(final String hostname) {
        notNull(hostname, "hostname may not be null");

        final CachingConnectionFactory connectionFactoryForHost = new CachingConnectionFactory(hostname, port);
        connectionFactoryForHost.setVirtualHost(virtualHost);
        connectionFactoryForHost.setUsername(username);
        connectionFactoryForHost.setPassword(password);

        return connectionFactoryForHost;
    }

    public boolean isAvailable(final String hostname) {
        notNull(hostname, "hostname may not be null");
        try {
            closeQuietly(connectionFactory(hostname).createConnection());
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    private CachingConnectionFactory connectionFactory(final String hostname) {
        final CachingConnectionFactory factory = new CachingConnectionFactory(hostname, port);
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setVirtualHost(virtualHost);
        return factory;
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

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        ConnectionFactoryFactory rhs = (ConnectionFactoryFactory) obj;
        return new EqualsBuilder()
                .append(this.port, rhs.port)
                .append(this.virtualHost, rhs.virtualHost)
                .append(this.username, rhs.username)
                .append(this.password, rhs.password)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(port)
                .append(virtualHost)
                .append(username)
                .append(password)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("port", port)
                .append("virtualHost", virtualHost)
                .append("username", username)
                .append("password", password)
                .toString();
    }
}
