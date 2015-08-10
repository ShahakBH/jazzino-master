package com.yazino.bi.aggregator;

import com.yazino.configuration.YazinoConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.dao.CannotAcquireLockException;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class LockRetryerTest {
    private static final int RETRY_DELAY = 10;
    private static final int MAX_RETRIES = 5;

    @Mock
    private YazinoConfiguration yazinoConfiguration;
    @Mock
    private Locker locker;

    private LockRetryer underTest;

    @Before
    public void setUp() {
        when(yazinoConfiguration.getInt("lock.retry-delay-ms", 1000)).thenReturn(RETRY_DELAY);
        when(yazinoConfiguration.getInt("lock.retries", 30)).thenReturn(MAX_RETRIES);

        underTest = new LockRetryer(locker, yazinoConfiguration);
    }

    @Test(expected = NullPointerException.class)
    public void theRetryerCannotBeCreatedWithANullLocker() {
        new LockRetryer(null, yazinoConfiguration);
    }

    @Test(expected = NullPointerException.class)
    public void theRetryerCannotBeCreatedWithANullConfiguration() {
        new LockRetryer(locker, null);
    }

    @Test
    public void theRetryerWillNotRetryWhenTheFirstCallSucceeds() {
        when(locker.lock()).thenReturn(true);

        assertThat(underTest.acquireLock(), is(true));

        verify(locker, times(1)).lock();
    }

    @Test
    public void theRetryerWillReturnTheResultOfTheLocker() {
        when(locker.lock())
                .thenReturn(false)
                .thenReturn(true);

        assertThat(underTest.acquireLock(), is(false));
        assertThat(underTest.acquireLock(), is(true));
    }

    @Test
    public void theRetryerWillKeepRetryingUntilTheCallSucceeds() {
        when(locker.lock())
                .thenThrow(new CannotAcquireLockException("aTestException"))
                .thenThrow(new CannotAcquireLockException("aTestException"))
                .thenThrow(new CannotAcquireLockException("aTestException"))
                .thenThrow(new CannotAcquireLockException("aTestException"))
                .thenReturn(true);

        assertThat(underTest.acquireLock(), is(true));

        verify(locker, times(5)).lock();
    }

    @Test
    public void theRetryerWillSleepBetweenRetries() {
        final long startTime = System.currentTimeMillis();
        when(locker.lock())
                .thenThrow(new CannotAcquireLockException("aTestException"))
                .thenThrow(new CannotAcquireLockException("aTestException"))
                .thenThrow(new CannotAcquireLockException("aTestException"))
                .thenThrow(new CannotAcquireLockException("aTestException"))
                .thenReturn(true);

        assertThat(underTest.acquireLock(), is(true));
        final long endTime = System.currentTimeMillis();

        assertThat(endTime - startTime, is(greaterThanOrEqualTo(4L * RETRY_DELAY)));
    }

    @Test(expected = CannotAcquireLockException.class)
    public void theRetryerWillStopRetryingAfterMaxRetries() {
        when(locker.lock()).thenThrow(new CannotAcquireLockException("aTestException"));

        try {
            underTest.acquireLock();
            fail("Expected exception not thrown");
        } catch (CannotAcquireLockException e) {
            verify(locker, times(MAX_RETRIES)).lock();
            throw e;
        }
    }

}
