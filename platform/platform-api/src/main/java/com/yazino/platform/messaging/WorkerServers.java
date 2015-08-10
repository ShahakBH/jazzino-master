package com.yazino.platform.messaging;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import static org.apache.commons.lang3.Validate.notBlank;

public class WorkerServers {
    private final String hostsPropertyName;
    private final int port;
    private final String username;
    private final String password;
    private final String virtualHost;

    public WorkerServers(final String hostsPropertyName,
                         final int port,
                         final String username,
                         final String password,
                         final String virtualHost) {
        notBlank(hostsPropertyName, "hostsPropertyName may not be blank");

        this.hostsPropertyName = hostsPropertyName;
        this.port = port;
        this.username = username;
        this.password = password;
        this.virtualHost = virtualHost;
    }

    public String getHostsPropertyName() {
        return hostsPropertyName;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getVirtualHost() {
        return virtualHost;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final WorkerServers rhs = (WorkerServers) obj;
        return new EqualsBuilder()
                .append(hostsPropertyName, rhs.hostsPropertyName)
                .append(port, rhs.port)
                .append(username, rhs.username)
                .append(password, rhs.password)
                .append(virtualHost, rhs.virtualHost)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(hostsPropertyName)
                .append(port)
                .append(username)
                .append(password)
                .append(virtualHost)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(hostsPropertyName)
                .append(port)
                .append(username)
                .append(password)
                .append(virtualHost)
                .toString();
    }

}
