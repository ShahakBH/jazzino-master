package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.TrophyLeaderboard;
import com.yazino.platform.model.tournament.TrophyLeaderboardResultingRequest;
import com.yazino.platform.repository.tournament.TrophyLeaderboardRepository;
import com.yazino.platform.util.concurrent.ThreadPoolFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openspaces.core.GigaSpace;
import com.yazino.game.api.time.SettableTimeSource;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@RunWith(MockitoJUnitRunner.class)
public class PeriodicTrophyLeaderboardResultingCheckerTest {

    @Mock
    private ScheduledExecutorService scheduledExecutorService;
    @Mock
    private GigaSpace gigaSpace;
    @Mock
    private ThreadPoolFactory threadPoolFactory;
    @Mock
    private TrophyLeaderboardRepository trophyLeaderboardRepository;
    @Mock
    private ScheduledFuture future;

    private PeriodicTrophyLeaderboardResultingChecker unit;
    private PeriodicTrophyLeaderboardResultingChecker.EventTask eventTask;
    private TrophyLeaderboard trophyLeaderboard1 = new TrophyLeaderboard(BigDecimal.valueOf(10));
    private TrophyLeaderboard trophyLeaderboard2 = new TrophyLeaderboard(BigDecimal.valueOf(20));

    @Before
    public void setUp() {
        final SettableTimeSource timeSource = new SettableTimeSource();

        when(trophyLeaderboardRepository.findLocalResultingRequired(timeSource)).thenReturn(
                new HashSet<TrophyLeaderboard>(Arrays.asList(trophyLeaderboard1, trophyLeaderboard2)));

        unit = new PeriodicTrophyLeaderboardResultingChecker(
                gigaSpace, threadPoolFactory, trophyLeaderboardRepository, timeSource);
        eventTask = unit.new EventTask();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void initialisationConfiguresTheEventTask() {
        final long checkDelay = 1234;

        unit.setCheckDelay(1234);

        when(threadPoolFactory.getScheduledThreadPool(1)).thenReturn(scheduledExecutorService);
        when(scheduledExecutorService.scheduleAtFixedRate(
                any(PeriodicTournamentChecker.EventTask.class), eq(checkDelay), eq(checkDelay), eq(TimeUnit.MILLISECONDS)))
                .thenReturn(future);

        unit.init();

        verify(scheduledExecutorService).scheduleAtFixedRate(
                any(PeriodicTournamentChecker.EventTask.class), eq(checkDelay), eq(checkDelay), eq(TimeUnit.MILLISECONDS));
        verify(threadPoolFactory).getScheduledThreadPool(1);
    }

    @Test
    public void stopTerminatedTheExecutor() {
        setField(unit, "scheduledFuture", future);
        setField(unit, "executorService", scheduledExecutorService);

        when(future.cancel(true)).thenReturn(true);

        unit.stop();

        verify(future).cancel(true);
        verify(scheduledExecutorService).shutdown();
    }

    @Test
    public void stopVerifiesState() {
        verifyNoMoreInteractions(future);
        verifyNoMoreInteractions(scheduledExecutorService);

        try {
            unit.stop();
            fail("Expected exception not thrown");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    public void taskReadsItemsRequiringResultingAndSchedulesResulting() {
        eventTask.run();

        verify(gigaSpace).write(new TrophyLeaderboardResultingRequest(trophyLeaderboard1.getId()));
        verify(gigaSpace).write(new TrophyLeaderboardResultingRequest(trophyLeaderboard2.getId()));
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    @Test
    public void taskDoesNotPropagateExceptions() {
        doThrow(new RuntimeException("anException")).when(gigaSpace).write(
                new TrophyLeaderboardResultingRequest(trophyLeaderboard1.getId()));

        eventTask.run();
    }

}
