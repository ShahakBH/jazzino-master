package com.yazino.platform.persistence;

import com.gigaspaces.client.WriteModifiers;
import com.gigaspaces.datasource.DataIterator;
import com.yazino.platform.grid.Routing;
import com.yazino.platform.model.DataLoadComplete;
import net.jini.core.lease.Lease;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openspaces.core.GigaSpace;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PartitionedIterableLoaderTest {
    private final static int PARTITION_NUMBER = 2;
    private final static int PARTITION_TOTAL = 8;
    private final static int BATCH_SIZE = 128;

    @Mock
    private DataIterable<Integer> dataIterable;
    @Mock
    private GigaSpace gigaSpace;
    @Mock
    private Routing routing;

    private PartitionedIterableLoader<Integer> underTest;

    @Before
    public void setUp() {
        when(routing.partitionCount()).thenReturn(PARTITION_TOTAL);
        when(routing.partitionId()).thenReturn(PARTITION_NUMBER);
        when(routing.isRoutedToCurrentPartition(1)).thenReturn(true);

        underTest = new PartitionedIterableLoader<>(gigaSpace, routing, new ImmediateExecutorService(), asList(dataIterable));
    }

    @Test(expected = NullPointerException.class)
    public void theLoaderCannotBeCreatedWithANullIterable() {
        new PartitionedIterableLoader<Integer>(gigaSpace, routing, new ImmediateExecutorService(), null);
    }

    @Test(expected = NullPointerException.class)
    public void theLoaderCannotBeCreatedWithANullDestinationPartition() {
        new PartitionedIterableLoader<>(null, routing, new ImmediateExecutorService(), asList(dataIterable));
    }

    @Test(expected = NullPointerException.class)
    public void theLoaderCannotBeCreatedWithANullRouting() {
        new PartitionedIterableLoader<>(gigaSpace, null, new ImmediateExecutorService(), asList(dataIterable));
    }

    @Test(expected = NullPointerException.class)
    public void theLoaderCannotBeCreatedWithANullExecutor() {
        new PartitionedIterableLoader<>(gigaSpace, routing, null, asList(dataIterable));
    }

    @Test
    public void theCurrentPartitionNumberIsPassedToTheIterable() {
        underTest.load();

        verify(dataIterable).iterateAll();
    }

    @Test
    public void theCurrentPartitionTotalIsPassedToTheIterable() {
        underTest.load();

        verify(dataIterable).iterateAll();
    }

    @Test
    public void nothingIsLoadedWhenADataLoadCompleteObjectIsPresentForTheIterable() {
        when(dataIterable.iterateAll()).thenReturn(anIteratorWith(16));
        when(gigaSpace.count(new DataLoadComplete(dataIterable.getClass().getSimpleName()))).thenReturn(1);

        underTest.load();

        verify(gigaSpace).count(new DataLoadComplete(dataIterable.getClass().getSimpleName()));
        verifyNoMoreInteractions(gigaSpace);
    }

    @Test
    public void nothingIsLoadedWhenTheIterableReturnsNull() {
        when(dataIterable.iterateAll()).thenReturn(null);

        underTest.load();

        verify(gigaSpace).count(new DataLoadComplete(dataIterable.getClass().getSimpleName()));
        verifyNoMoreInteractions(gigaSpace);
    }

    @Test
    public void nothingIsLoadedWhenTheIterableReturnsAnEmptyIterator() {
        when(dataIterable.iterateAll()).thenReturn(anIteratorWith(0));

        underTest.load();

        verify(gigaSpace).count(new DataLoadComplete(dataIterable.getClass().getSimpleName()));
        verify(gigaSpace).write(new DataLoadComplete(1, dataIterable.getClass().getSimpleName()));
        verifyNoMoreInteractions(gigaSpace);
    }

    @Test
    public void onlyItemsThatAreRoutedToTheCurrentPartitionAreWrittenToTheSpace() {
        when(routing.isRoutedToCurrentPartition(1)).thenReturn(true);
        when(routing.isRoutedToCurrentPartition(2)).thenReturn(true);
        when(routing.isRoutedToCurrentPartition(3)).thenReturn(true);
        when(dataIterable.iterateAll()).thenReturn(anIteratorWith(16));

        underTest.load();

        verify(gigaSpace).writeMultiple(anyArrayFrom(1, 4), Lease.FOREVER, WriteModifiers.UPDATE_OR_WRITE);
    }

    @Test
    public void aDataLoadCompleteIsWrittenToThePartitionAfterASuccessfulLoad() {
        when(dataIterable.iterateAll()).thenReturn(anIteratorWith(16));

        underTest.load();

        verify(gigaSpace).write(new DataLoadComplete(1, dataIterable.getClass().getSimpleName()));
    }

    @Test
    public void aNumberOfItemsBelowTheBatchLimitIsWrittenToTheSpace() {
        when(routing.isRoutedToCurrentPartition(anyInt())).thenReturn(true);
        when(dataIterable.iterateAll()).thenReturn(anIteratorWith(120));

        underTest.load();

        verify(gigaSpace).writeMultiple(anyArrayFrom(0, 120), Lease.FOREVER, WriteModifiers.UPDATE_OR_WRITE);
    }

    @Test
    public void aNumberOfItemsEqualToTheBatchLimitIsWrittenToTheSpace() {
        when(routing.isRoutedToCurrentPartition(anyInt())).thenReturn(true);
        when(dataIterable.iterateAll()).thenReturn(anIteratorWith(BATCH_SIZE));

        underTest.load();

        verify(gigaSpace).writeMultiple(anyArrayFrom(0, BATCH_SIZE), Lease.FOREVER, WriteModifiers.UPDATE_OR_WRITE);
    }

    @Test
    public void aNumberOfItemsAboveTheBatchLimitIsWrittenToTheSpaceInMultipleBatches() {
        when(routing.isRoutedToCurrentPartition(anyInt())).thenReturn(true);
        when(dataIterable.iterateAll()).thenReturn(anIteratorWith(257));

        underTest.load();

        verify(gigaSpace).writeMultiple(anyArrayFrom(0, BATCH_SIZE), Lease.FOREVER, WriteModifiers.UPDATE_OR_WRITE);
        verify(gigaSpace).writeMultiple(anyArrayFrom(BATCH_SIZE, 2 * BATCH_SIZE), Lease.FOREVER, WriteModifiers.UPDATE_OR_WRITE);
        verify(gigaSpace).writeMultiple(anyArrayFrom(2 * BATCH_SIZE, 257), Lease.FOREVER, WriteModifiers.UPDATE_OR_WRITE);
    }

    @Test
    public void aBackupSpaceDoesNotLoadData() {
        when(routing.isBackup()).thenReturn(true);

        underTest.load();

        verifyZeroInteractions(gigaSpace, dataIterable);
    }

    private Object[] anyArrayFrom(final int start, final int end) {
        final Object[] items = new Object[end - start];
        for (int i = 0; i < items.length; i++) {
            items[i] = start + i;
        }
        return items;
    }

    private DataIterator<Integer> anIteratorWith(final int itemCount) {
        return new DataIterator<Integer>() {
            private int cursor;

            @Override
            public void close() {
            }

            @Override
            public boolean hasNext() {
                return cursor < itemCount;
            }

            @Override
            public Integer next() {
                return cursor++;
            }

            @Override
            public void remove() {
            }
        };
    }

    private static class ImmediateExecutorService implements ExecutorService {
        @Override
        public void shutdown() {
        }

        @Override
        public List<Runnable> shutdownNow() {
            return null;
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
            return false;
        }

        @Override
        public <T> Future<T> submit(final Callable<T> task) {
            return null;
        }

        @Override
        public <T> Future<T> submit(final Runnable task, final T result) {
            task.run();
            return null;
        }

        @Override
        public Future<?> submit(final Runnable task) {
            task.run();
            return null;
        }

        @Override
        public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return null;
        }

        @Override
        public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException {
            return null;
        }

        @Override
        public <T> T invokeAny(final Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            return null;
        }

        @Override
        public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return null;
        }

        @Override
        public void execute(final Runnable command) {
            command.run();
        }
    }
}
