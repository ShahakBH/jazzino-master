package com.yazino.platform.lightstreamer.adapter;

import com.lightstreamer.interfaces.data.*;
import com.yazino.configuration.YazinoConfiguration;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.annotation.PreDestroy;
import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * This class adapts messages from a queue into ones that can be sent to a mobile client.
 */
public class MobileSubscriptionAdapter implements SmartDataProvider, MobileSubscriptionContainerFactoryListener {
    private static final Logger LOG = LoggerFactory.getLogger(MobileSubscriptionAdapter.class);

    private static final ClassPathXmlApplicationContext CONTEXT = new ClassPathXmlApplicationContext("classpath:ls-spring.xml");

    private static final String PROPERTY_RETRY_DELAY_MS = "strata.rabbitmq.retry-delay";
    private static final String PROPERTY_RETRY_COUNT = "strata.rabbitmq.retry-count";
    private static final String PROPERTY_CONNECTION_EXPIRY_MS = "strata.rabbitmq.connection-expiry";
    private static final int DEFAULT_RETRY_DELAY_MS = 3 * 1000;
    private static final int DEFAULT_RETRY_COUNT = 10;
    private static final int DEFAULT_CONNECTION_EXPIRY_MS = 1000 * 60 * 60 * 4;
    private static final int RETRY_CHECK_DELAY = 500;
    private static final int HEALTH_CHECK_DELAY = 5 * 1000;
    private static final int REAPER_DELAY = 5 * 1000 * 60;

    private final Map<String, LifecycleListeningMessageListenerContainer> subscriptions = new HashMap<>();
    private final ReadWriteLock subscriptionsLock = new ReentrantReadWriteLock();

    private final Set<String> pendingUnsubscription = new HashSet<>();
    private final ReadWriteLock pendingUnsubscriptionLock = new ReentrantReadWriteLock();

    private final List<DelayedSubscription> delayedSubscriptions = new LinkedList<>();
    private final ReadWriteLock delayedUnsubscriptionLock = new ReentrantReadWriteLock();

    private final MobileSubscriptionContainerFactory containerFactory;
    private final YazinoConfiguration yazinoConfiguration;
    private final ExecutorService subscriptionExecutorService;
    private final ScheduledExecutorService scheduledExecutorService;

    @SuppressWarnings("UnusedDeclaration")
    public MobileSubscriptionAdapter() {
        this(new MobileSubscriptionContainerFactory(CONTEXT.getBean(Exchange.class),
                CONTEXT.getBean(ConnectionFactoryFactory.class),
                CONTEXT.getBean(YazinoConfiguration.class)),
                CONTEXT.getBean(YazinoConfiguration.class),
                CONTEXT.getBean("subscriptionExecutorService", ExecutorService.class),
                CONTEXT.getBean("scheduledExecutorService", ScheduledExecutorService.class));
    }

    MobileSubscriptionAdapter(final MobileSubscriptionContainerFactory containerFactory,
                              final YazinoConfiguration yazinoConfiguration,
                              final ExecutorService subscriptionExecutorService,
                              final ScheduledExecutorService scheduledExecutorService) {
        notNull(containerFactory, "containerFactory may not be null");
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");
        notNull(subscriptionExecutorService, "subscriptionExecutorService may not be null");
        notNull(scheduledExecutorService, "scheduledExecutorService may not be null");

        this.containerFactory = containerFactory;
        this.yazinoConfiguration = yazinoConfiguration;
        this.subscriptionExecutorService = subscriptionExecutorService;
        this.scheduledExecutorService = scheduledExecutorService;

        containerFactory.addListener(this);

        startScheduledTasks();
    }

    private void startScheduledTasks() {
        scheduledExecutorService.scheduleAtFixedRate(new DelayedSubscriptionChecker(), RETRY_CHECK_DELAY, RETRY_CHECK_DELAY, TimeUnit.MILLISECONDS);
        scheduledExecutorService.scheduleAtFixedRate(new HealthLogger(), HEALTH_CHECK_DELAY, HEALTH_CHECK_DELAY, TimeUnit.MILLISECONDS);
        scheduledExecutorService.scheduleAtFixedRate(new AbandonedConnectionReaper(), REAPER_DELAY, REAPER_DELAY, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void shutdown() {
        LOG.info("Shutting down subscription adapter");

        if (!scheduledExecutorService.isShutdown()) {
            scheduledExecutorService.shutdown();
        }
        if (!subscriptionExecutorService.isShutdown()) {
            subscriptionExecutorService.shutdown();
        }
    }

    @Override
    public void subscribe(final String subject, final Object itemToken, final boolean iteratorRequired)
            throws SubscriptionException, FailureException {
        subscribeTo(subject, true);
    }

    @Override
    public void subscribe(final String subject, final boolean iteratorRequired) throws SubscriptionException, FailureException {
        LOG.warn("Unexpected call of deprecated subscribe method");
        subscribeTo(subject, true);
    }

    @Override
    public void unsubscribe(final String subject) {
        unsubscribeFrom(subject, true, null);
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    private void unsubscribeFrom(final String subject,
                                 final boolean asynchronous,
                                 final Runnable callback) {
        final UnsubscribeTask task = new UnsubscribeTask(subject, callback);
        if (asynchronous) {
            subscriptionExecutorService.submit(task);
            LOG.debug("Subject {} queued for unsubscription", subject);

        } else {
            try {
                task.run();
                if (task.getLastError() != null) {
                    throw task.getLastError();
                }
                callback.run();
                LOG.debug("Subject {} unsubscription processed", subject);

            } catch (Exception e) {
                LOG.error("Unsubscription for subject {} failed", subject, e);
                throw new RuntimeException("Subscription failed for subject " + subject, e);
            }
        }
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    private void subscribeTo(final String subject, final boolean asynchronous) {
        final SubscribeTask task = new SubscribeTask(subject, asynchronous);
        if (asynchronous) {
            subscriptionExecutorService.submit(task);
            LOG.debug("Subject {} queued for subscription", subject);

        } else {
            try {
                task.run();
                if (task.getLastError() != null) {
                    throw task.getLastError();
                }

            } catch (Exception e) {
                LOG.error("Subscription for subject {} failed", subject, e);
                throw new RuntimeException("Subscription failed for subject " + subject, e);
            }
        }
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    private void subscribeToDelayed(final String subject,
                                    final int retry,
                                    final int maxRetries,
                                    final boolean asynchronous,
                                    final DateTime subscribeAt) {
        final SubscribeTask task = new SubscribeTask(subject, retry, maxRetries, asynchronous);
        if (asynchronous) {
            delayedUnsubscriptionLock.writeLock().lock();
            try {
                delayedSubscriptions.add(new DelayedSubscription(task, subscribeAt));
            } finally {
                delayedUnsubscriptionLock.writeLock().unlock();
            }
            LOG.debug("Subject {} will have subscription attempted at {}", subject, subscribeAt);

        } else {
            try {
                final long timeUntilSubscription = subscribeAt.getMillis() - DateTimeUtils.currentTimeMillis();
                if (timeUntilSubscription > 0) {
                    sleepFor(timeUntilSubscription);
                }
                task.run();
                if (task.getLastError() != null) {
                    throw task.getLastError();
                }

            } catch (Exception e) {
                LOG.error("Delayed subscription for subject {} failed", subject, e);
                throw new RuntimeException("Delayed subscription failed for subject " + subject, e);
            }
        }
    }

    private void resubscribeTo(final String subject, final boolean asynchronous) {
        unsubscribeFrom(subject, asynchronous, new Runnable() {
            @Override
            public void run() {
                subscribeTo(subject, asynchronous);
            }
        });
    }

    @Override
    public void invalidateAllContainers() {
        final List<String> subjects = new ArrayList<>();
        subscriptionsLock.readLock().lock();
        try {
            subjects.addAll(subscriptions.keySet());

        } finally {
            subscriptionsLock.readLock().unlock();
        }

        LOG.debug("Containers invalidated; resubscribing to all subjects: {}", subjects);
        for (String subject : subjects) {
            resubscribeTo(subject, true);
        }
    }

    @Override
    public void setListener(ItemEventListener eventListener) {
        containerFactory.setEventListener(eventListener);
    }

    @Override
    public void init(final Map map, final File file) throws DataProviderException {
    }

    @Override
    public boolean isSnapshotAvailable(final String subject) throws SubscriptionException {
        return false;
    }

    private void sleepFor(final long sleepTime) {
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException ignored) {
            // ignored
        }
    }

    final Map<String, LifecycleListeningMessageListenerContainer> getSubscriptions() {
        subscriptionsLock.readLock().lock();
        try {
            return new HashMap<>(subscriptions);

        } finally {
            subscriptionsLock.readLock().unlock();
        }
    }

    private boolean currentlySubscribedTo(final String subject) {
        subscriptionsLock.readLock().lock();
        try {
            return subscriptions.containsKey(subject);
        } finally {
            subscriptionsLock.readLock().unlock();
        }
    }

    private class ResubscribingLifecycleListener implements LifecycleListener {
        private final String subject;

        public ResubscribingLifecycleListener(final String subject) {
            this.subject = subject;
        }

        @Override
        public void starting() {
        }

        @Override
        public void stopping() {
            // You cannot try reconnecting here, as we don't know if the connection is intended or not,
            // and the LightStreamer unsubscription tends to come after the RabbitMQ disconnection.
        }

        @Override
        public void startupConnectionFailure(final AmqpConnectException t) {
            LOG.debug("Startup connection received for subject {}, resubscribing", subject, t);
            unsubscribeFrom(subject, true, new Runnable() {
                @Override
                public void run() {
                    subscribeToDelayed(subject, 1, maxRetries(), true, new DateTime().plusMillis(retryDelay()));
                }
            });

        }

        @Override
        public void consumerCommitting(final int messagesReceived) {
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
            ResubscribingLifecycleListener rhs = (ResubscribingLifecycleListener) obj;
            return new EqualsBuilder()
                    .append(this.subject, rhs.subject)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(subject)
                    .toHashCode();
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("subject", subject)
                    .toString();
        }
    }

    private class SubscribeTask implements Runnable {
        private final String subject;
        private final int maxRetries;
        private final int retry;
        private final boolean retryAsynchronously;

        private Exception lastError;

        public SubscribeTask(final String subject,
                             final boolean retryAsynchronously) {
            this.subject = subject;
            this.retryAsynchronously = retryAsynchronously;

            this.maxRetries = maxRetries();
            this.retry = 1;
        }

        private SubscribeTask(final String subject,
                              final int retry,
                              final int maxRetries,
                              final boolean retryAsynchronously) {
            this.subject = subject;
            this.retry = retry;
            this.maxRetries = maxRetries;
            this.retryAsynchronously = retryAsynchronously;
        }

        private Exception getLastError() {
            return lastError;
        }

        @Override
        public void run() {
            try {
                LOG.debug("Attempting subscription to {}, attempt {}/{}", subject, retry, maxRetries);
                subscribe();

            } catch (Exception e) {
                LOG.error("Subscription failed for subject {} on attempt {}/{}", subject, retry, maxRetries, e);
                lastError = e;

                if (retry < maxRetries) {
                    LOG.debug("Scheduling retry {}/{} of subscription to {}", retry + 1, maxRetries, subject);
                    subscribeToDelayed(subject, retry + 1, maxRetries, retryAsynchronously, new DateTime().plusMillis(retryDelay()));
                } else {
                    LOG.warn("Retries exhausted for subscription to {}", subject);
                }
            }
        }

        private void subscribe() {
            if (currentlySubscribedTo(subject)) {
                LOG.debug("Subscription to {} exists, no subscription required", subject);
                return;
            }

            subscriptionsLock.writeLock().lock();
            try {
                if (subscriptions.containsKey(subject)) { // To be sure, to be sure
                    LOG.debug("Subscription to {} exists, no subscription required", subject);
                    return;
                }

                final LifecycleListeningMessageListenerContainer container = containerFactory.containerForSubject(subject);
                container.addLifecycleListener(new ResubscribingLifecycleListener(subject));
                LOG.debug("Subscribing to {}", subject);

                container.start();

                subscriptions.put(subject, container);

            } finally {
                subscriptionsLock.writeLock().unlock();
            }
        }

        public String getSubject() {
            return subject;
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
            SubscribeTask rhs = (SubscribeTask) obj;
            return new EqualsBuilder()
                    .append(this.subject, rhs.subject)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(subject)
                    .toHashCode();
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("subject", subject)
                    .toString();
        }
    }

    private int retryDelay() {
        return yazinoConfiguration.getInt(PROPERTY_RETRY_DELAY_MS, DEFAULT_RETRY_DELAY_MS);
    }

    private int maxRetries() {
        return yazinoConfiguration.getInt(PROPERTY_RETRY_COUNT, DEFAULT_RETRY_COUNT);
    }

    private class UnsubscribeTask implements Runnable {
        private final String subject;
        private final Runnable successCallback;

        private Exception lastError;

        public UnsubscribeTask(final String subject, final Runnable successCallback) {
            this.subject = subject;
            this.successCallback = successCallback;
        }

        private Exception getLastError() {
            return lastError;
        }

        @Override
        public void run() {
            if (!currentlySubscribedTo(subject)) {
                LOG.debug("No subscription to {} exists, no unsubscription required", subject);
                return;
            }

            try {
                unsubscribe();
                invokeSuccessCallback();

            } catch (Exception e) {
                lastError = e;
                LOG.error("Unsubscription from {} failed", subject, e);
            }
        }

        private void invokeSuccessCallback() {
            try {
                if (successCallback != null) {
                    successCallback.run();
                }
            } catch (Exception e) {
                LOG.error("Unsubscription successCallback failed for subject {}", subject, e);
            }
        }

        private void unsubscribe() {
            SimpleMessageListenerContainer container = null;

            markUnsubscriptionAsPending();

            subscriptionsLock.writeLock().lock();
            try {
                container = subscriptions.remove(subject);

            } finally {
                subscriptionsLock.writeLock().unlock();
                markUnsubscriptionAsComplete();
            }

            if (container != null) {
                LOG.debug("Unsubscribed from {}", subject);
                container.stop();
            }
        }

        private void markUnsubscriptionAsPending() {
            pendingUnsubscriptionLock.writeLock().lock();
            try {
                pendingUnsubscription.add(subject);
            } finally {
                pendingUnsubscriptionLock.writeLock().unlock();
            }
        }

        private void markUnsubscriptionAsComplete() {
            pendingUnsubscriptionLock.writeLock().lock();
            try {
                pendingUnsubscription.remove(subject);
            } finally {
                pendingUnsubscriptionLock.writeLock().unlock();
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
            final UnsubscribeTask rhs = (UnsubscribeTask) obj;
            return new EqualsBuilder()
                    .append(this.subject, rhs.subject)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(subject)
                    .toHashCode();
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("subject", subject)
                    .toString();
        }
    }

    private class HealthLogger implements Runnable {
        @Override
        public void run() {
            try {
                if (subscriptionExecutorService instanceof ThreadPoolExecutor) {
                    final ThreadPoolExecutor pool = (ThreadPoolExecutor) subscriptionExecutorService;
                    LOG.info("Thread pool status: active {}; max {}; largest {}; pending {}; completed {}",
                            pool.getActiveCount(), pool.getMaximumPoolSize(), pool.getLargestPoolSize(),
                            pool.getQueue().size(), pool.getCompletedTaskCount());
                }

                subscriptionsLock.readLock().lock();
                pendingUnsubscriptionLock.readLock().lock();
                delayedUnsubscriptionLock.readLock().lock();
                try {
                    LOG.info("Health status: subscriptions {}; pending unsubscriptions {}; delayed subscriptions {}",
                            subscriptions.size(), pendingUnsubscription.size(), delayedSubscriptions.size());
                } finally {
                    subscriptionsLock.readLock().unlock();
                    pendingUnsubscriptionLock.readLock().unlock();
                    delayedUnsubscriptionLock.readLock().unlock();
                }
            } catch (Exception e) {
                LOG.error("Unable to retrieve health information", e);
            }
        }
    }

    private class DelayedSubscriptionChecker implements Runnable {
        @Override
        public void run() {
            LOG.debug("Checking for pending delayed subscriptions");

            final DateTime now = new DateTime();
            delayedUnsubscriptionLock.writeLock().lock();

            try {
                for (Iterator<DelayedSubscription> i = delayedSubscriptions.iterator(); i.hasNext(); ) {
                    final DelayedSubscription delayedSubscription = i.next();
                    if (delayedSubscription.getDelayUntil().isAfter(now)) {
                        continue;
                    }

                    i.remove();
                    final SubscribeTask subscribeTask = delayedSubscription.getSubscribeTask();
                    subscriptionExecutorService.submit(subscribeTask);
                    LOG.debug("Delayed subscription to subject {} queued for subscription", subscribeTask.getSubject());
                }

            } finally {
                delayedUnsubscriptionLock.writeLock().unlock();
            }
        }
    }

    private class AbandonedConnectionReaper implements Runnable {
        @Override
        public void run() {
            final int connectionExpiryDelay = yazinoConfiguration.getInt(PROPERTY_CONNECTION_EXPIRY_MS, DEFAULT_CONNECTION_EXPIRY_MS);

            LOG.info("Checking for abandoned connections (expiry delay {}ms", connectionExpiryDelay);

            final long expiryTime = new DateTime().minusMillis(connectionExpiryDelay).getMillis();

            int connectionsReaped = 0;
            subscriptionsLock.readLock().lock();
            try {
                for (Map.Entry<String, LifecycleListeningMessageListenerContainer> subscription : subscriptions.entrySet()) {
                    final long lastReceived = subscription.getValue().getMessageLastReceived();
                    if (lastReceived < expiryTime) {
                        unsubscribe(subscription.getKey());
                        ++connectionsReaped;
                    }
                }

            } finally {
                subscriptionsLock.readLock().unlock();
            }

            LOG.info("Reaped {} connections quiescent for longer than {}ms", connectionsReaped, connectionExpiryDelay);
        }
    }

    private class DelayedSubscription {
        private final SubscribeTask subscribeTask;
        private final DateTime delayUntil;

        private DelayedSubscription(final SubscribeTask subscribeTask,
                                    final DateTime delayUntil) {
            this.subscribeTask = subscribeTask;
            this.delayUntil = delayUntil;
        }

        public SubscribeTask getSubscribeTask() {
            return subscribeTask;
        }

        public DateTime getDelayUntil() {
            return delayUntil;
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
            DelayedSubscription rhs = (DelayedSubscription) obj;
            return new EqualsBuilder()
                    .append(this.subscribeTask, rhs.subscribeTask)
                    .append(this.delayUntil, rhs.delayUntil)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(subscribeTask)
                    .append(delayUntil)
                    .toHashCode();
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("subscribeTask", subscribeTask)
                    .append("delayUntil", delayUntil)
                    .toString();
        }
    }
}
