package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.model.tournament.TournamentLeaderboardUpdateRequest;
import com.yazino.platform.repository.tournament.TournamentRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openspaces.core.GigaSpace;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TournamentLeaderboardNotificationServiceTest {

    @Mock
    private ScheduledExecutorService scheduledExecutorService;
    @Mock
    private GigaSpace localGigaSpace;
    @Mock
    private TournamentRepository tournamentRepository;
    @Mock
    private ScheduledFuture future;

    private TournamentLeaderboardNotificationService underTest;

    @Before
    public void setUp() {
        underTest = new TournamentLeaderboardNotificationService(tournamentRepository, localGigaSpace);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testStop() {
        final ScheduledFuture future = mock(ScheduledFuture.class);

        ReflectionTestUtils.setField(underTest, "scheduledFuture", future);
        ReflectionTestUtils.setField(underTest, "executorService", scheduledExecutorService);

        when(future.cancel(eq(true))).thenReturn(true);

        underTest.stop();

        verify(future).cancel(eq(true));
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
    public void testProcess() {
        final List<BigDecimal> tournamentIds = Arrays.asList(bd(10), bd(15), bd(20));
        final Set<Tournament> tournaments = new HashSet<>();
        for (final BigDecimal id : tournamentIds) {
            tournaments.add(new Tournament(id));
        }

        when(tournamentRepository.findLocalForLeaderboardUpdates()).thenReturn(tournaments);

        underTest.process();

        verify(localGigaSpace).write(new TournamentLeaderboardUpdateRequest(bd(10)));
        verify(localGigaSpace).write(new TournamentLeaderboardUpdateRequest(bd(15)));
        verify(localGigaSpace).write(new TournamentLeaderboardUpdateRequest(bd(20)));
    }

    private BigDecimal bd(long value) {
        return BigDecimal.valueOf(value);
    }
}

