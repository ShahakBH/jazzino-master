package com.yazino.bi.aggregator;

import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class AggregatorLockCleanupTest {
    private AggregatorLockCleanup underTest;
    private AggregatorLockDao lockDAO;

    @Test
    public void clearLocksShouldClearLocksUsingDaowithHostName() throws UnknownHostException {
        lockDAO = mock(AggregatorLockDao.class);
        underTest = new AggregatorLockCleanup(lockDAO);
        underTest.cleanupLocks();
        verify(lockDAO).clearLocks(InetAddress.getLocalHost().getHostName());
    }
}
