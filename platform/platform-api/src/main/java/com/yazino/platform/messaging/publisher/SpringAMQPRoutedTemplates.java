package com.yazino.platform.messaging.publisher;

import com.google.common.collect.ComparisonChain;
import com.yazino.configuration.ConfigurationPropertyChangeCallback;
import com.yazino.configuration.FilteringConfigurationListener;
import com.yazino.configuration.YazinoConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

public class SpringAMQPRoutedTemplates {
    private static final Logger LOG = LoggerFactory.getLogger(SpringAMQPRoutedTemplates.class);

    /**
     * The blacklist time period.
     * <p/>
     * This needs to be high enough that we won't spend forever restoring and removing a failed host.
     * It also needs to be synchronised with the value client-side, so restorations are roughly synchronous
     * (see MobileSubscriptionContainerFactory).
     */
    private static final long DEFAULT_BLACKLIST_TIME_MS = 60 * 1000;
    private static final String PROPERTY_BLACKLIST_TIME = "strata.rabbitmq.blacklist-time";
    private static final long BLACKLIST_CHECK_PERIOD = 1000;

    private final Map<String, ConnectionFactory> connectionFactories = new HashMap<>();
    private final ReadWriteLock connectionFactoriesLock = new ReentrantReadWriteLock();
    private final Random random = new Random();
    private final SortedSet<Host> hosts = new TreeSet<>();
    private final Set<BlacklistedHost> blacklist = new HashSet<>();
    private final ReadWriteLock hostsLock = new ReentrantReadWriteLock();
    private final AtomicLong lastBlacklistCheck = new AtomicLong(DateTimeUtils.currentTimeMillis());

    private final ConnectionFactoryFactory connectionFactoryFactory;
    private final CloneableRabbitTemplate sourceTemplate;
    private final YazinoConfiguration yazinoConfiguration;
    private final String hostNamesProperty;

    public SpringAMQPRoutedTemplates(final String hostNamesProperty,
                                     final ConnectionFactoryFactory connectionFactoryFactory,
                                     final CloneableRabbitTemplate sourceTemplate,
                                     final YazinoConfiguration yazinoConfiguration) {
        notBlank(hostNamesProperty, "hostNamesProperty may not be null/blank");
        notNull(connectionFactoryFactory, "connectionFactoryFactory may not be null");
        notNull(sourceTemplate, "sourceTemplate may not be null");
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");

        this.hostNamesProperty = hostNamesProperty;
        this.connectionFactoryFactory = connectionFactoryFactory;
        this.sourceTemplate = sourceTemplate;
        this.yazinoConfiguration = yazinoConfiguration;

        updateHostsFromConfiguration();
        listenToConfigurationChanges();
    }

    private void updateHostsFromConfiguration() {
        LOG.debug("Updating hosts from configuration property {}", hostNamesProperty);

        final String[] hostNamesFromConfig = yazinoConfiguration.getStringArray(hostNamesProperty);
        if (hostNamesFromConfig == null || hostNamesFromConfig.length == 0) {
            LOG.error("Cannot find any hostnames in property {} for {}::{}",
                    hostNamesProperty, sourceTemplate.getExchange(), sourceTemplate.getQueue());
        } else {
            setHosts(asList(hostNamesFromConfig));
        }
    }

    private void listenToConfigurationChanges() {
        final ConfigurationPropertyChangeCallback callback = new ConfigurationPropertyChangeCallback() {
            @Override
            public void propertyChanged(final String propertyName, final Object propertyValue) {
                updateHostsFromConfiguration();
            }

            @Override
            public void propertiesChanged() {
                updateHostsFromConfiguration();
            }
        };
        yazinoConfiguration.addConfigurationListener(new FilteringConfigurationListener(callback, hostNamesProperty));
    }

    public String hostForKey(final Number routingKey) {
        hostsLock.readLock().lock();

        if (!blacklist.isEmpty()) {
            hostsLock.readLock().unlock();
            try {
                checkHostsOnBlacklist();
            } finally {
                hostsLock.readLock().lock();
            }
        }

        try {
            if (hosts.isEmpty()) {
                LOG.error("No hosts available for routing");
                throw new IllegalStateException("No hosts available for routing");
            }

            if (routingKey == null) {
                final int hostIndex = random.nextInt(hosts.size());
                final String rabbitHost = hostAtIndex(hostIndex);
                LOG.debug("No routing key supplied, routing via {} (index {})", rabbitHost, hostIndex);
                return rabbitHost;
            }

            final int hostIndex = Math.abs(routingKey.intValue() % hosts.size());
            final String rabbitHost = hostAtIndex(hostIndex);
            LOG.debug("Sending message via host {} (index {})", rabbitHost, hostIndex);
            return rabbitHost;

        } finally {
            hostsLock.readLock().unlock();
        }
    }

    private String hostAtIndex(final int hostIndex) {
        int index = 0;
        for (Host host : hosts) {
            if (index == hostIndex) {
                return host.getHostname();
            }
            ++index;
        }
        return hosts.last().getHostname();
    }

    private Host hostByName(final String hostname) {
        for (Host host : hosts) {
            if (host.getHostname().equals(hostname)) {
                return host;
            }
        }
        return null;
    }

    public void blacklist(final String hostname) {
        hostsLock.writeLock().lock();

        try {
            if (hosts.size() == 1) {
                LOG.debug("Host will not be blacklisted as it's the only host available: {}", hostname);
                return;
            }

            final Host host = hostByName(hostname);
            if (host == null) {
                LOG.debug("Host is no longer valid or is already blacklisted: {}", hostname);
                return;
            }

            blacklist.add(new BlacklistedHost(host, DateTimeUtils.currentTimeMillis() + blacklistTime()));
            hosts.remove(host);

            LOG.debug("Host has been blacklisted: {}", host);

        } finally {
            hostsLock.writeLock().unlock();
        }
    }

    private long blacklistTime() {
        return yazinoConfiguration.getLong(PROPERTY_BLACKLIST_TIME, DEFAULT_BLACKLIST_TIME_MS);
    }

    private void checkHostsOnBlacklist() {
        final long currentTimestamp = DateTimeUtils.currentTimeMillis();
        if ((lastBlacklistCheck.get() + BLACKLIST_CHECK_PERIOD) >= currentTimestamp) {
            return;
        }
        lastBlacklistCheck.set(currentTimestamp);

        hostsLock.writeLock().lock();
        try {
            LOG.debug("Checking blacklist for expired hosts for {}: {}", currentTimestamp, blacklist);

            for (Iterator<BlacklistedHost> iterator = blacklist.iterator(); iterator.hasNext();) {
                final BlacklistedHost blacklistedHost = iterator.next();
                if (blacklistedHost.isEligibleForRestoration(currentTimestamp)) {
                    final Host host = blacklistedHost.toHost();
                    if (host.isAvailable()) {
                        restoreHost(host);
                        iterator.remove();
                    } else {
                        LOG.debug("Host is not available and will remain blacklisted: {}", host.getHostname());
                        blacklistedHost.incrementBlacklistedTimeBy(blacklistTime());
                    }
                }
            }

        } finally {
            hostsLock.writeLock().unlock();
        }
    }

    private void restoreHost(final Host host) {
        if (!hosts.contains(host)) {
            LOG.debug("Host has been restored from the blacklist: {}", host.getHostname());
            hosts.add(host);
        }
    }

    public String hostFor(final Object routingObject) {
        return hostForKey(routingKeyFor(routingObject));
    }

    public AmqpTemplate templateFor(final String host) {
        return sourceTemplate.newWith(connectionFactoryFor(host));
    }

    public void setHosts(final Collection<String> newHosts) {
        LOG.info("Updating hosts to: {} from: {}", newHosts, hosts);

        hostsLock.writeLock().lock();
        try {
            hosts.clear();
            if (newHosts != null) {
                int index = 0;
                for (String newHost : newHosts) {
                    if (!StringUtils.isBlank(newHost)) {
                        hosts.add(new Host(newHost.trim(), index));
                        ++index;
                    }
                }
            }
        } finally {
            hostsLock.writeLock().unlock();
        }

        removeUnusedConnectionFactories();
    }

    private Number routingKeyFor(final Object routingObject) {
        if (routingObject != null) {
            return routingObject.hashCode();
        }
        return 0;
    }

    private void removeUnusedConnectionFactories() {
        final Set<String> hostsToRemove = new HashSet<>();

        hostsLock.readLock().lock();
        connectionFactoriesLock.readLock().lock();

        try {
            for (String connectionFactoryHost : connectionFactories.keySet()) {
                if (hostByName(connectionFactoryHost) == null) {
                    hostsToRemove.add(connectionFactoryHost);
                }
            }

        } finally {
            connectionFactoriesLock.readLock().unlock();
            hostsLock.readLock().unlock();
        }

        removeHostsFromConnectionFactories(hostsToRemove);
    }

    private void removeHostsFromConnectionFactories(final Set<String> hostsToRemove) {
        if (!hostsToRemove.isEmpty()) {
            LOG.debug("Removing hosts from connection factories: {}", hostsToRemove);

            connectionFactoriesLock.writeLock().lock();
            try {
                for (String hostToRemove : hostsToRemove) {
                    connectionFactories.remove(hostToRemove);
                }
            } finally {
                connectionFactoriesLock.writeLock().unlock();
            }
        }
    }

    private ConnectionFactory connectionFactoryFor(final String host) {
        connectionFactoriesLock.readLock().lock();
        try {
            if (connectionFactories.containsKey(host)) {
                return connectionFactories.get(host);
            }

            final ConnectionFactory connectionFactory;
            try {
                connectionFactoriesLock.readLock().unlock();
                connectionFactory = cacheNewConnectionFactoryFor(host);
            } finally {
                connectionFactoriesLock.readLock().lock();
            }
            return connectionFactory;

        } finally {
            connectionFactoriesLock.readLock().unlock();
        }
    }

    private ConnectionFactory cacheNewConnectionFactoryFor(final String host) {
        connectionFactoriesLock.writeLock().lock();

        try {
            final CachingConnectionFactory connectionFactory = connectionFactoryFactory.forHost(host);
            connectionFactories.put(host, connectionFactory);
            return connectionFactory;

        } finally {
            connectionFactoriesLock.writeLock().unlock();
        }
    }

    /**
     * This object exists to ensure the order remains predictable as hosts are blacklisted and restored. This
     * should only matter if we're not using clustered RabbitMQ, mind.
     */
    private class Host implements Comparable<Host> {
        private final String hostname;
        private final int order;

        private Host(final String hostname,
                     final int order) {
            this.hostname = hostname;
            this.order = order;
        }

        public boolean isAvailable() {
            return connectionFactoryFactory.isAvailable(hostname);
        }

        String getHostname() {
            return hostname;
        }

        int getOrder() {
            return order;
        }

        @Override
        public int compareTo(final Host that) {
            return ComparisonChain.start()
                    .compare(this.order, that.order)
                    .result();
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
            Host rhs = (Host) obj;
            return new EqualsBuilder()
                    .append(this.hostname, rhs.hostname)
                    .append(this.order, rhs.order)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(hostname)
                    .append(order)
                    .toHashCode();
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("hostname", hostname)
                    .append("order", order)
                    .toString();
        }
    }

    private class BlacklistedHost extends Host {
        private long blacklistedUntil;

        private BlacklistedHost(final Host host, final long blacklistedUntil) {
            super(host.getHostname(), host.getOrder());
            this.blacklistedUntil = blacklistedUntil;
        }

        private Host toHost() {
            return new Host(getHostname(), getOrder());
        }

        private boolean isEligibleForRestoration(final long currentTimestamp) {
            return blacklistedUntil <= currentTimestamp;
        }

        private void incrementBlacklistedTimeBy(final long additionalBlacklistTime) {
            blacklistedUntil += additionalBlacklistTime;
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
            return new EqualsBuilder()
                    .appendSuper(super.equals(obj))
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .appendSuper(super.hashCode())
                    .toHashCode();
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .appendSuper(super.toString())
                    .append("blacklistedUntil", blacklistedUntil)
                    .toString();
        }
    }
}
