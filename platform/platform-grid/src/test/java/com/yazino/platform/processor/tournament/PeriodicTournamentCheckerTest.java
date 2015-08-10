package com.yazino.platform.processor.tournament;

import com.j_spaces.core.client.SQLQuery;
import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.model.tournament.TournamentEventRequest;
import com.yazino.platform.util.concurrent.ThreadPoolFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openspaces.core.GigaSpace;
import org.springframework.test.util.ReflectionTestUtils;
import com.yazino.game.api.time.SettableTimeSource;
import com.yazino.game.api.time.TimeSource;

import java.math.BigDecimal;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class PeriodicTournamentCheckerTest {

    @Mock
    private ScheduledExecutorService scheduledExecutorService;
    @Mock
    private GigaSpace gigaSpace;
    @Mock
    private ThreadPoolFactory threadPoolFactory;

    private TimeSource timeSource;
    private PeriodicTournamentChecker underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        timeSource = new SettableTimeSource();

        underTest = new PeriodicTournamentChecker(gigaSpace, threadPoolFactory, timeSource);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInitialisation() {
        final long initialCheckDelay = 3424;
        final long checkDelay = 1234;

        underTest.setCheckDelay(1234);
        underTest.setInitialCheckDelay(initialCheckDelay);

        final ScheduledFuture future = mock(ScheduledFuture.class);

        when(threadPoolFactory.getScheduledThreadPool(1)).thenReturn(scheduledExecutorService);
        when(scheduledExecutorService.scheduleAtFixedRate(
                any(PeriodicTournamentChecker.EventTask.class), eq(initialCheckDelay), eq(checkDelay), eq(TimeUnit.MILLISECONDS)))
                .thenReturn(future);

        underTest.init();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testStop() {
        final ScheduledFuture future = mock(ScheduledFuture.class);

        ReflectionTestUtils.setField(underTest, "scheduledFuture", future);
        ReflectionTestUtils.setField(underTest, "executorService", scheduledExecutorService);

        when(future.cancel(eq(true))).thenReturn(true);

        underTest.stop();

        verify(scheduledExecutorService).shutdown();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testStopVerifiedState() {
        try {
            underTest.stop();
            fail("Expected exception not thrown");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEventTask() {
        final Tournament tournament1 = new Tournament(BigDecimal.valueOf(1));
        final Tournament tournament2 = new Tournament(BigDecimal.valueOf(2));

        final TournamentEventRequest request1 = new TournamentEventRequest(tournament1.getTournamentId());
        final TournamentEventRequest request2 = new TournamentEventRequest(tournament2.getTournamentId());

        final SQLQuery expectedQuery = new SQLQuery<Tournament>(Tournament.class, "nextEvent <= ?");
        expectedQuery.setParameter(1, timeSource.getCurrentTimeStamp());

        final PeriodicTournamentChecker.EventTask eventTask = underTest.new EventTask();

        final ArgumentCaptor<SQLQuery> capturedQuery = ArgumentCaptor.forClass(SQLQuery.class);

        when(gigaSpace.write(request1)).thenReturn(null);
        when(gigaSpace.write(request2)).thenReturn(null);
        when(gigaSpace.readMultiple(capturedQuery.capture(), eq(Integer.MAX_VALUE)))
                .thenReturn(new Tournament[]{tournament1, tournament2});

        eventTask.run();

        assertEquals(expectedQuery.toString(), capturedQuery.getValue().toString());
    }

}
