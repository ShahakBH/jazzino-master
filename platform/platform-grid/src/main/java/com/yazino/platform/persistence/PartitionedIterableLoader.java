package com.yazino.platform.persistence;

import com.gigaspaces.client.WriteModifiers;
import com.gigaspaces.datasource.DataIterator;
import com.yazino.platform.grid.Routing;
import com.yazino.platform.model.DataLoadComplete;
import net.jini.core.lease.Lease;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.WriteMultipleException;
import org.openspaces.core.space.mode.PostPrimary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Load data into the space for the current partition.
 * <p/>
 * The routing algorithm is describe in the GigaSpace documentation. We get to filter at this level as BigDecimal
 * and String hashes can't be calculated in MySQL.
 *
 * @param <T> the type to load.
 * @see "http://wiki.gigaspaces.com/wiki/display/XAP9/Routing+In+Partitioned+Spaces"
 */
public class PartitionedIterableLoader<T> {
    private static final Logger LOG = LoggerFactory.getLogger(PartitionedIterableLoader.class);

    private static final int MAX_RETRIES = 24;
    private static final long RETRY_DELAY_MS = 5000;
    private static final int BATCH_SIZE = 128;
    private static final int DEFAULT_THREADPOOL_SIZE = 3;

    private final List<DataIterable<T>> dataIterables = new ArrayList<>();

    private final ExecutorService executorService;
    private final GigaSpace gigaSpace;
    private final Routing routing;

    @Autowired
    public PartitionedIterableLoader(@Qualifier("gigaSpace") final GigaSpace gigaSpace,
                                     final Routing routing,
                                     final Collection<DataIterable<T>> dataIterables) {
        this(gigaSpace, routing, Executors.newFixedThreadPool(DEFAULT_THREADPOOL_SIZE), dataIterables);
    }

    public PartitionedIterableLoader(@Qualifier("gigaSpace") final GigaSpace gigaSpace,
                                     final Routing routing,
                                     final ExecutorService executorService,
                                     final Collection<DataIterable<T>> dataIterables) {
        notNull(gigaSpace, "gigaSpace may not be null");
        notNull(routing, "routing may not be null");
        notNull(executorService, "executorService may not be null");
        notNull(dataIterables, "dataIterables may not be null");

        this.dataIterables.addAll(dataIterables);
        this.gigaSpace = gigaSpace;
        this.executorService = executorService;
        this.routing = routing;
    }

    @PostPrimary
    public void load() {
        if (routing.isBackup()) {
            LOG.info("Queuing data iterators skipped for backup instance: ({}/{})", routing.partitionId(), routing.partitionCount());
            return;
        }

        LOG.info("Queuing data iterators for loading ({}/{})", routing.partitionId(), routing.partitionCount());
        for (DataIterable<T> dataIterable : dataIterables) {
            executorService.submit(new LoadDataIntoGrid(dataIterable));
        }
    }

    @PreDestroy
    public void shutdown() {
        if (!executorService.isTerminated()) {
            executorService.shutdown();
        }
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
        final PartitionedIterableLoader rhs = (PartitionedIterableLoader) obj;
        return new EqualsBuilder()
                .append(dataIterables, rhs.dataIterables)
                .append(gigaSpace, rhs.gigaSpace)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(dataIterables)
                .append(gigaSpace)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(dataIterables)
                .append(gigaSpace)
                .toString();
    }

    private class LoadDataIntoGrid implements Runnable {
        private final DataIterable<T> dataIterable;

        public LoadDataIntoGrid(final DataIterable<T> dataIterable) {
            this.dataIterable = dataIterable;
        }

        @Override
        public void run() {
            if (spaceHasBeenPopulatedForIterable()) {
                LOG.info("Space has previously completed data load: {} ({}/{})",
                        dataIterable.getClass().getSimpleName(), routing.partitionId(), routing.partitionCount());
                return;
            }

            LOG.info("Loading data: {} ({}/{})",
                    dataIterable.getClass().getSimpleName(), routing.partitionId(), routing.partitionCount());

            final DataIterator<T> dataIterator = dataIterable.iterateAll();
            if (dataIterator == null) {
                LOG.error("Null iterator returned: {} ({}/{})",
                        dataIterable.getClass().getSimpleName(), routing.partitionId(), routing.partitionCount());
                return;
            }

            if (writeDataFrom(dataIterator)) {
                LOG.info("Data load completed successfully: {} ({}/{})",
                        dataIterable.getClass().getSimpleName(), routing.partitionId(), routing.partitionCount());
                markLoadCompleteForSpace();

            } else {
                LOG.error("Unable to load into space (retries exceeded): {} ({}/{})",
                        dataIterable.getClass().getSimpleName(), routing.partitionId(), routing.partitionCount());
            }
        }

        private boolean writeDataFrom(final DataIterator<T> dataIterator) {
            long attempt = 0;
            boolean complete = false;
            do {
                try {
                    writeElementsToSpace(dataIterator);
                    complete = true;

                } catch (Exception e) {
                    if (e instanceof WriteMultipleException
                            && e.getMessage().contains("com.gigaspaces.cluster.activeelection.InactiveSpaceException")) {
                        if (routing.isBackup()) { // if the space was inactive, this may have changed
                            LOG.info("This is now a backup instance, no data will be loaded: {} ({}/{})",
                                    dataIterable.getClass().getSimpleName(), routing.partitionId(), routing.partitionCount());
                            return true;
                        }

                        LOG.warn("Space is inactive, will retry in {}ms: {} ({}/{})",
                                RETRY_DELAY_MS, dataIterable.getClass().getSimpleName(), attempt, MAX_RETRIES);

                    } else {
                        LOG.error("Failed to write items to space: {} ({}/{})",
                                dataIterable.getClass().getSimpleName(),
                                routing.partitionId(),
                                routing.partitionCount(), e);
                        throw new RuntimeException("Failed to write items to space", e);
                    }

                    ++attempt;
                    sleepFor(RETRY_DELAY_MS);
                }

            } while (!complete && attempt < MAX_RETRIES);

            return complete;
        }

        private boolean spaceHasBeenPopulatedForIterable() {
            return gigaSpace.count(new DataLoadComplete(dataIterable.getClass().getSimpleName())) > 0;
        }

        private void sleepFor(final long delayInMs) {
            try {
                Thread.sleep(delayInMs);
            } catch (InterruptedException ignored) {
                // ignored
            }
        }


        private void writeElementsToSpace(final DataIterator<T> dataIterator) {
            final List<T> batch = new ArrayList<>(BATCH_SIZE);
            while (dataIterator.hasNext()) {
                final T item = dataIterator.next();
                if (item != null && routing.isRoutedToCurrentPartition(item)) {
                    batch.add(item);
                }

                if (batch.size() >= BATCH_SIZE) {
                    flush(batch);
                }
            }

            if (!batch.isEmpty()) {
                flush(batch);
            }
        }

        private void flush(final List<T> batch) {
            LOG.debug("Writing {} items from {} to partition {}/{}",
                    batch.size(), dataIterable.getClass().getSimpleName(), routing.partitionId(), routing.partitionCount());
            writeTo(gigaSpace, batch);
            batch.clear();
        }

        private void writeTo(final GigaSpace destination,
                             final List<T> batch) {
            destination.writeMultiple(batch.toArray(), Lease.FOREVER, WriteModifiers.UPDATE_OR_WRITE);
        }

        private void markLoadCompleteForSpace() {
            for (int i = 0; i < routing.partitionCount(); ++i) {
                if (routing.isRoutedToCurrentPartition(i)) {
                    gigaSpace.write(new DataLoadComplete(i, dataIterable.getClass().getSimpleName()));
                    return;
                }
            }
            throw new IllegalStateException(String.format("Couldn't find an ID valid for partition %s/%s",
                    routing.partitionId(), routing.partitionCount()));
        }
    }
}
