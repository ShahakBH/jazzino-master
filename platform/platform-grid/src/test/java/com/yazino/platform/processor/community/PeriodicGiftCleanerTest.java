package com.yazino.platform.processor.community;

import com.yazino.platform.repository.community.GiftRepository;
import com.yazino.platform.service.community.GiftProperties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PeriodicGiftCleanerTest {
    private static final long TEN_MINUTES = 600000;
    private static final int RETENTION = 234;

    @Mock
    private GiftRepository giftRepository;
    @Mock
    private GiftProperties giftProperties;
    @Mock
    private ScheduledExecutorService executorService;
    @Mock
    private ScheduledFuture future;

    private PeriodicGiftCleaner underTest;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        when(executorService.scheduleAtFixedRate(any(Runnable.class), eq(TEN_MINUTES), eq(TEN_MINUTES), eq(TimeUnit.MILLISECONDS)))
                .thenReturn(future);
        when(giftProperties.retentionInHours()).thenReturn(RETENTION);

        underTest = new PeriodicGiftCleaner(giftRepository, giftProperties, executorService);
    }

    @Test(expected = NullPointerException.class)
    public void cleanerCannotBeCreatedWithANullRepository() {
        new PeriodicGiftCleaner(null, giftProperties);
    }

    @Test(expected = NullPointerException.class)
    public void cleanerCannotBeCreatedWithANullProperties() {
        new PeriodicGiftCleaner(giftRepository, null);
    }

    @Test(expected = NullPointerException.class)
    public void cleanerCannotBeCreatedWithAScheduler() {
        new PeriodicGiftCleaner(giftRepository, null);
    }

    @Test
    public void initialiseSchedulesTheTaskForAPeriodOfTenMinutes() {
        underTest.initialise();

        verify(executorService).scheduleAtFixedRate(any(Runnable.class), eq(TEN_MINUTES), eq(TEN_MINUTES), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    public void theScheduledTaskInvokesCleanUpOldGifts() {
        underTest.initialise();

        final ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executorService).scheduleAtFixedRate(taskCaptor.capture(), eq(TEN_MINUTES), eq(TEN_MINUTES), eq(TimeUnit.MILLISECONDS));
        taskCaptor.getValue().run();

        verify(giftRepository).cleanUpOldGifts(RETENTION);
    }

    @Test
    public void theScheduledTaskDoesNotPropagateExceptionsFromTheRepository() {
        doThrow(new RuntimeException("aTestException")).when(giftRepository).cleanUpOldGifts(RETENTION);

        underTest.initialise();

        final ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executorService).scheduleAtFixedRate(taskCaptor.capture(), eq(TEN_MINUTES), eq(TEN_MINUTES), eq(TimeUnit.MILLISECONDS));
        taskCaptor.getValue().run();

        verify(giftRepository).cleanUpOldGifts(RETENTION);
    }

    @Test
    public void shutdownShutsTheExecutorDown() {
        underTest.initialise();

        underTest.shutdown();

        verify(executorService).shutdown();
    }

    @Test
    public void shutdownCancelsTheFuture() {
        underTest.initialise();

        underTest.shutdown();

        verify(future).cancel(true);
    }

    @Test
    public void shutdownDoesNotCancelTheFutureIfAlreadyCancelled() {
        underTest.initialise();
        when(future.isCancelled()).thenReturn(true);

        underTest.shutdown();

        verify(future, never()).cancel(true);
    }
}
