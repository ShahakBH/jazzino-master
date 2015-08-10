package com.yazino.platform.lightstreamer.adapter;

import com.lightstreamer.interfaces.data.ItemEventListener;
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
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

public class MobileSubscriptionContainerFactory {
    private static final Logger LOG = LoggerFactory.getLogger(MobileSubscriptionContainerFactory.class);

    /**
     * The blacklist time period.
     * <p/>
     * This needs to be high enough that we won't spend forever restoring and removing a failed host.
     * It also needs to be synchronised with the value server-side, so restorations are roughly synchronous
     * (see SpringAMQPRoutedTemplates).
     */
    private static final long DEFAULT_BLACKLIST_TIME_MS = 60 * 1000;
    private static final String PROPERTY_HOSTS = "strata.rabbitmq.host";
    private static final String PROPERTY_BLACKLIST_TIME = "strata.rabbitmq.blacklist-time";
    private static final long BLACKLIST_CHECK_PERIOD = 1000;

    private final List<MobileSubscriptionContainerFactoryListener> listeners = new CopyOnWriteArrayList<>();
    private final PlayerIdResolver idResolver = new PlayerIdResolver();
    private Map<String, ConnectionFactory> connectionFactories = new HashMap<>();
    private final SortedSet<Host> hosts = new TreeSet<>();
    private final Set<BlacklistedHost> blacklist = new HashSet<>();
    private final ReadWriteLock hostsLock = new ReentrantReadWriteLock();
    private final ReadWriteLock connectionFactoriesLock = new ReentrantReadWriteLock();
    private final AtomicLong lastBlacklistCheck = new AtomicLong(DateTimeUtils.currentTimeMillis());

    private final Exchange exchange;
    private final YazinoConfiguration yazinoConfiguration;
    private final ConnectionFactoryFactory connectionFactoryFactory;
    private ItemEventListener eventListener;

    @Autowired
    public MobileSubscriptionContainerFactory(final Exchange exchange,
                                              final ConnectionFactoryFactory connectionFactoryFactory,
                                              final YazinoConfiguration yazinoConfiguration) {
        notNull(exchange, "exchange may not be null");
        notNull(connectionFactoryFactory, "connectionFactoryFactory may not be null");
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");

        this.exchange = exchange;
        this.yazinoConfiguration = yazinoConfiguration;
        this.connectionFactoryFactory = connectionFactoryFactory;

        updateHostsFromConfiguration();
        listenToConfigurationChanges();
    }

    public void addListener(final MobileSubscriptionContainerFactoryListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    private void updateHostsFromConfiguration() {
        LOG.debug("Updating hosts from configuration property {}", PROPERTY_HOSTS);

        final String[] hostNamesFromConfig = yazinoConfiguration.getStringArray(PROPERTY_HOSTS);
        if (hostNamesFromConfig == null || hostNamesFromConfig.length == 0) {
            LOG.error("Cannot find any hostnames in property {}", PROPERTY_HOSTS);
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
        yazinoConfiguration.addConfigurationListener(new FilteringConfigurationListener(callback, PROPERTY_HOSTS));
    }

    public LifecycleListeningMessageListenerContainer containerForSubject(final String subject) {
        notEmpty(subject);
        if (eventListener == null) {
            throw new IllegalStateException("ItemEventListener has not been set");
        }

        final String hostname = hostForSubject(subject);
        final ConnectionFactory factory = connectionFactoryFor(hostname);

        LOG.debug("Host {} is mapped to connection factory {}", hostname, factory);

        final Queue queue = createQueue(subject, factory);
        if (queue == null && blacklist(hostname)) {
            return containerForSubject(subject);
        }

        if (queue == null) {
            throw new IllegalStateException("Cannot create connection to RabbitMQ for subject " + subject);
        }

        final LifecycleListeningMessageListenerContainer container = new LifecycleListeningMessageListenerContainer(factory);
        container.setQueues(queue);
        container.setMessageListener(new MobileMessageListenerAdapter(subject, eventListener));
        container.addLifecycleListener(new CleanUpLifecycleListener(hostname));
        return container;
    }

    private Queue createQueue(final String subject, final ConnectionFactory factory) {
        Queue queue;
        try {
            queue = buildQueue(factory, subject);
            LOG.debug("Subject {} is mapped to queue {}", subject, queue.getName());

        } catch (AmqpConnectException e) {
            LOG.error("Failed to connect to host", e);
            queue = null;
        }
        return queue;
    }

    private String hostForSubject(final String subject) {
        notEmpty(subject);
        if (eventListener == null) {
            throw new IllegalStateException("ItemEventListener has not been set");
        }

        restoreHostsFromBlacklistIfNecessary();

        hostsLock.readLock().lock();
        try {
            if (hosts.isEmpty()) {
                LOG.error("No hosts available for routing");
                throw new IllegalStateException("No hosts available for routing");
            }

            final BigDecimal playerId = idResolver.resolve(subject);
            final int hostIndex = Math.abs(playerId.intValue() % hosts.size());
            final String hostname = hostForIndex(hostIndex);
            LOG.debug("Player {} yielded host index {} resulting in connection host {}", playerId, hostIndex, hostname);

            return hostname;

        } finally {
            hostsLock.readLock().unlock();
        }
    }

    private ConnectionFactory connectionFactoryFor(final String hostname) {
        connectionFactoriesLock.readLock().lock();

        try {
            ConnectionFactory factory = connectionFactories.get(hostname);
            if (factory == null) {
                connectionFactoriesLock.readLock().unlock();
                try {
                    factory = createConnectionFactoryFor(hostname);
                } finally {
                    connectionFactoriesLock.readLock().lock();
                }
            }
            return factory;

        } finally {
            connectionFactoriesLock.readLock().unlock();
        }
    }

    private ConnectionFactory createConnectionFactoryFor(final String hostname) {
        connectionFactoriesLock.writeLock().lock();
        try {
            ConnectionFactory connectionFactory = connectionFactories.get(hostname);
            if (connectionFactory == null) {
                connectionFactory = connectionFactoryFactory.forHost(hostname);
                if (connectionFactory == null) {
                    throw new IllegalStateException("Unable to create connection factory for host " + hostname);
                }

                connectionFactories.put(hostname, connectionFactory);
            }
            return connectionFactory;

        } finally {
            connectionFactoriesLock.writeLock().unlock();
        }
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


    public void setEventListener(ItemEventListener eventListener) {
        this.eventListener = eventListener;
    }

    final Queue buildQueue(final ConnectionFactory connectionFactory, final String subject) {
        final RabbitAdmin administrator = new RabbitAdmin(connectionFactory);
        final Queue queue = administrator.declareQueue();
        administrator.declareBinding(BindingBuilder.bind(queue).to(exchange).with(subject).noargs());
        return queue;
    }

    public boolean blacklist(final String hostname) {
        hostsLock.writeLock().lock();

        try {
            if (hosts.size() == 1) {
                LOG.debug("Host will not be blacklisted as it's the only host available: {}", hostname);
                return false;
            }

            final Host host = hostByName(hostname);
            if (host == null) {
                LOG.debug("Host is no longer valid or is already blacklisted: {}", hostname);
                return true;
            }

            blacklist.add(new BlacklistedHost(host, DateTimeUtils.currentTimeMillis() + blacklistTime()));
            hosts.remove(host);

            LOG.info("Host has been blacklisted: {}", host);
            return true;

        } finally {
            hostsLock.writeLock().unlock();
        }
    }

    private long blacklistTime() {
        return yazinoConfiguration.getLong(PROPERTY_BLACKLIST_TIME, DEFAULT_BLACKLIST_TIME_MS);
    }

    public boolean restoreHostsFromBlacklistIfNecessary() {
        final long currentTimestamp = DateTimeUtils.currentTimeMillis();
        if ((lastBlacklistCheck.get() + BLACKLIST_CHECK_PERIOD) >= currentTimestamp) {
            return false;
        }
        lastBlacklistCheck.set(currentTimestamp);

        hostsLock.readLock().lock();
        try {
            if (blacklist.isEmpty()) {
                return false;
            }
        } finally {
            hostsLock.readLock().unlock();
        }

        hostsLock.writeLock().lock();
        boolean hostsChanged = false;
        try {
            LOG.debug("Checking blacklist for expired hosts for {}: {}", currentTimestamp, blacklist);

            for (Iterator<BlacklistedHost> iterator = blacklist.iterator(); iterator.hasNext();) {
                final BlacklistedHost blacklistedHost = iterator.next();
                if (blacklistedHost.isEligibleForRestoration(currentTimestamp)) {
                    final Host host = blacklistedHost.toHost();
                    if (host.isAvailable()) {
                        hostsChanged = restoreHost(host);
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

        return hostsChanged;
    }

    private boolean restoreHost(final Host host) {
        if (!hosts.contains(host)) {
            LOG.info("Host has been restored from the blacklist: {}", host);
            hosts.add(host);
            return true;
        }
        return false;
    }

    private String hostForIndex(final int desiredIndex) {
        int index = 0;
        for (Host messagingHost : hosts) {
            if (index == desiredIndex) {
                return messagingHost.getHostname();
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

    private void fireInvalidateAllContainers() {
        for (MobileSubscriptionContainerFactoryListener listener : listeners) {
            listener.invalidateAllContainers();
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
            if (this.order < that.order) {
                return -1;
            } else if (this.order > that.order) {
                return 1;
            }
            return 0;
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

    private final class BlacklistedHost extends Host {
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

    private class CleanUpLifecycleListener implements LifecycleListener {
        private final String hostname;

        public CleanUpLifecycleListener(final String hostname) {
            this.hostname = hostname;
        }

        @Override
        public void starting() {
        }

        @Override
        public void stopping() {
        }

        @Override
        public void startupConnectionFailure(final AmqpConnectException t) {
            LOG.debug("Startup connection received, blacklisting connection factory for {}", hostname, t);
            blacklist(hostname);
        }

        @Override
        public void consumerCommitting(final int messagesReceived) {
            if (restoreHostsFromBlacklistIfNecessary()) {
                try {
                    LOG.info("Hosts have changed, stopping all containers");
                    fireInvalidateAllContainers();

                } catch (Exception e) {
                    LOG.error("Container stop failed", e);
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
            CleanUpLifecycleListener rhs = (CleanUpLifecycleListener) obj;
            return new EqualsBuilder()
                    .append(this.hostname, rhs.hostname)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(hostname)
                    .toHashCode();
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("hostname", hostname)
                    .toString();
        }
    }
}
