package com.yazino.bi.aggregator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
@DirtiesContext
public class AggregatorLockDAOIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AggregatorLockDao underTest;

    @Before
    @After
    public void cleanUp() {
        jdbc.update("DELETE FROM aggregator_lock");
    }

    @Test
    public void lockReturnsTrueWhenALockCanBeAcquired() {
        assertThat(underTest.lock("aLock", "aClient"), is(true));
    }

    @Test
    public void lockReturnsFalseWhenALockAlreadyExistsForTheClient() {
        underTest.lock("aLock", "aClient");

        assertThat(underTest.lock("aLock", "aClient"), is(false));
    }

    @Test
    public void lockReturnsFalseWhenALockAlreadyExistsForAnotherClient() {
        underTest.lock("aLock", "anotherClient");

        assertThat(underTest.lock("aLock", "aClient"), is(false));
    }

    @Test
    public void unlockRemovesTheLockWhenALockExistsForTheGivenClient() {
        underTest.lock("aLock", "aClient");

        underTest.unlock("aLock", "aClient");

        assertThat(underTest.lock("aLock", "anotherClient"), is(true));
    }

    @Test
    public void clearLocksShouldKillAllLocksForThisHost() {
        assertTrue(underTest.lock("aLock", "aClient"));
        assertTrue(underTest.lock("bLock", "bClient"));
        assertTrue(underTest.lock("cLock", "bClient"));
        assertTrue(underTest.lock("dLock", "cClient"));

        underTest.clearLocks("bClient");
        assertThat(underTest.lock("aLock", "aClient"), is(false));
        assertThat(underTest.lock("bLock", "aClient"), is(true));
        assertThat(underTest.lock("cLock", "aClient"), is(true));
        assertThat(underTest.lock("dLock", "aClient"), is(false));
    }

    @Test
    public void updateShouldRunOnlyOnceAtATime() throws InterruptedException {
        final ExecutorService executorService = Executors.newFixedThreadPool(3);
        final List<Boolean> locks = new CopyOnWriteArrayList<>();
        for (int i = 0; i < 3; i++) {
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    try {
                        locks.add(underTest.lock("aLock", "aClient"));
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException ignored) {
                            // ignored
                        }
                        underTest.unlock("aLock", "aClient");
                    } catch (CannotAcquireLockException e) {
                        locks.add(false);
                    }
                }
            };

            executorService.execute(task);
        }
        try {
            executorService.shutdown();
            executorService.awaitTermination(100, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
            e.printStackTrace();
            fail();
        }

        assertThat(locks.size(), is(3));
        assertThat(locks, containsInAnyOrder(Boolean.TRUE, Boolean.FALSE, Boolean.FALSE));
    }

}
