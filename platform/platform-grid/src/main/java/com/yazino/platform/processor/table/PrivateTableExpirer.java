package com.yazino.platform.processor.table;

import com.j_spaces.core.client.SQLQuery;
import com.yazino.platform.model.session.InboxMessage;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.model.table.TableControlMessage;
import com.yazino.platform.model.table.TableControlMessageType;
import com.yazino.platform.model.table.TableRequestWrapper;
import com.yazino.platform.repository.session.InboxMessageRepository;
import com.yazino.platform.util.concurrent.ThreadPoolFactory;
import org.joda.time.DateTime;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import com.yazino.game.api.NewsEvent;
import com.yazino.game.api.NewsEventType;
import com.yazino.game.api.ParameterisedMessage;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.Validate.notNull;

public class PrivateTableExpirer {
    private static final Logger LOG = LoggerFactory.getLogger(PrivateTableExpirer.class);

    private static final long ONE_DAY_IN_MS = 1000 * 60 * 60 * 24;

    private static final String EXPIRED_TABLES_QUERY = "lastUpdated <= ? AND ownerId is NOT null";
    private static final int THIRTY_DAYS = 30;
    private static final int THREAD_POOL_SIZE = 1;
    private static final int ONE_DAY = THREAD_POOL_SIZE;
    private static final long INITIAL_POLL_DELAY = 120 * 1000;

    private final GigaSpace gigaSpace;
    private final ThreadPoolFactory threadPoolFactory;
    private final InboxMessageRepository inboxMessageRepository;

    private int daysToWaitBeforeExpiring = THIRTY_DAYS;
    private int daysToWarnBeforeExpiring = ONE_DAY;
    private long pollingDelay = ONE_DAY_IN_MS;

    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> future;

    @Autowired(required = true)
    public PrivateTableExpirer(@Qualifier("gigaSpace") final GigaSpace gigaSpace,
                               final ThreadPoolFactory threadPoolFactory,
                               final InboxMessageRepository inboxMessageRepository) {
        notNull(gigaSpace, "gigaSpace may not be null");
        notNull(threadPoolFactory, "threadPoolFactory may not be null");
        notNull(inboxMessageRepository, "inboxMessageRepository may not be null");

        this.gigaSpace = gigaSpace;
        this.threadPoolFactory = threadPoolFactory;
        this.inboxMessageRepository = inboxMessageRepository;
    }

    public void initialise() throws Exception {
        if (future != null) {
            throw new IllegalStateException("Expirer is already initialised");
        }

        LOG.info("Initialising private table expirer: tables will be expired after {} days; "
                + "a warning will be issued {} days before that; and a check will take place every {} ms",
                daysToWaitBeforeExpiring, daysToWarnBeforeExpiring, pollingDelay);

        executorService = threadPoolFactory.getScheduledThreadPool(THREAD_POOL_SIZE);

        final EventTask eventTask = new EventTask();
        future = executorService.scheduleAtFixedRate(
                eventTask, INITIAL_POLL_DELAY, pollingDelay, TimeUnit.MILLISECONDS);
    }

    public void shutdown() throws Exception {
        LOG.info("Shutting down private table expirer");

        try {
            future.cancel(true);
            future = null;
            executorService.shutdown();

        } catch (Exception e) {
            LOG.error("Could not shutdown the event checker", e);
        }
    }

    public int getDaysToWaitBeforeExpiring() {
        return daysToWaitBeforeExpiring;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setDaysToWaitBeforeExpiring(final int daysToWaitBeforeExpiring) {
        this.daysToWaitBeforeExpiring = daysToWaitBeforeExpiring;
    }

    public int getDaysToWarnBeforeExpiring() {
        return daysToWarnBeforeExpiring;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setDaysToWarnBeforeExpiring(final int daysToWarnBeforeExpiring) {
        this.daysToWarnBeforeExpiring = daysToWarnBeforeExpiring;
    }

    public long getPollingDelay() {
        return pollingDelay;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setPollingDelay(final long pollingDelay) {
        this.pollingDelay = pollingDelay;
    }

    final class EventTask implements Runnable {
        private static final String EXPIRY_MESSAGE
                = "Your private %s table has not been used recently and will be removed on %2$td/%2$tm/%2$tY.";
        private static final String EXPIRY_SHORT_MESSAGE
                = "Your private %s table will be removed on %2$td/%2$tm/%2$tY.";
        private static final int IMMEDIATE = 0;

        private EventTask() {
            // don't allow external construction
        }

        public void run() {
            try {
                final SQLQuery<Table> query = new SQLQuery<Table>(Table.class, EXPIRED_TABLES_QUERY);
                query.setParameter(1, timeForWarnings());

                LOG.debug("Running table expiry check: {}", query);

                final Table[] tables = gigaSpace.readMultiple(query, Integer.MAX_VALUE);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("For date {}, {} are eligible for warning/expiry",
                            new DateTime(query.getParameters()[IMMEDIATE]), tables.length);
                }

                for (final Table table : tables) {
                    if (table.getLastUpdated() > timeForExpiry()) {
                        warnOwnerAboutImminentExpiry(table);

                    } else {
                        closeExpiredTable(table);
                    }

                }

            } catch (Exception e) {
                LOG.error("Table expiry failed", e);
            }
        }

        private void closeExpiredTable(final Table table) {
            LOG.debug("Private table expired; closing table {} for owner {}",
                    table.getTableId(), table.getOwnerId());
            gigaSpace.write(new TableRequestWrapper(
                    new TableControlMessage(table.getTableId(), TableControlMessageType.CLOSE)));
        }

        private void warnOwnerAboutImminentExpiry(final Table table) {
            LOG.debug("Private table soon to expire; sending warning for table {} for owner {}",
                    table.getTableId(), table.getOwnerId());

            inboxMessageRepository.send(new InboxMessage(table.getOwnerId(), expiryEventFor(table), new DateTime()));
        }

        private NewsEvent expiryEventFor(final Table table) {
            final DateTime expiryDate = new DateTime().plusDays(daysToWarnBeforeExpiring);
            final String gameTypeDisplayName = table.getGameType().getName();
            final ParameterisedMessage message = new ParameterisedMessage(
                    EXPIRY_MESSAGE, gameTypeDisplayName, expiryDate.toDate());
            final ParameterisedMessage shortMessage = new ParameterisedMessage(
                    EXPIRY_SHORT_MESSAGE, gameTypeDisplayName, expiryDate.toDate());
            return new NewsEvent.Builder(table.getOwnerId(), message)
                    .setType(NewsEventType.NEWS)
                    .setShortDescription(shortMessage)
                    .build();
        }

        private long timeForWarnings() {
            final DateTime currentTime = new DateTime();
            final DateTime searchTime = currentTime.minusDays(daysToWaitBeforeExpiring)
                    .plusDays(daysToWarnBeforeExpiring);
            return searchTime.getMillis();
        }

        private long timeForExpiry() {
            final DateTime currentTime = new DateTime();
            final DateTime searchTime = currentTime.minusDays(daysToWaitBeforeExpiring);
            return searchTime.getMillis();
        }
    }
}
