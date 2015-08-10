package strata.server.worker.audit.playedtracking;

import com.yazino.platform.audit.message.Transaction;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import strata.server.worker.audit.playedtracking.model.Clock;
import strata.server.worker.audit.playedtracking.model.DailyCycle;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;

import static java.math.BigDecimal.ZERO;
import static java.math.BigDecimal.valueOf;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class PlayerPlayedTrackingTest {
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(3141592);
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(899834);

    private PlayerPlayedTracking underTest;
    private LastPlayerPlayedEvents lastPlayerPlayedEvents;
    private DailyCycle dailyCycle;
    private Clock clock;
    private Long gameId = 123l;
    private BigDecimal tableId = BigDecimal.TEN;

    @Before
    public void setUp() throws Exception {
        clock = mock(Clock.class);
        dailyCycle = mock(DailyCycle.class);
        lastPlayerPlayedEvents = mock(LastPlayerPlayedEvents.class);
        stub(clock.getCurrentTime()).toReturn(0l);
        underTest = new PlayerPlayedTracking(lastPlayerPlayedEvents);
        ReflectionTestUtils.setField(underTest, "cycle", dailyCycle);
        ReflectionTestUtils.setField(underTest, "clock", clock);
    }

    @Test
    public void shouldStoreStakeTransactionForPlayer() {
        final Transaction tx1 = new Transaction(valueOf(1), ZERO, "Return", "ref", 0l, valueOf(1000), gameId, tableId, SESSION_ID, PLAYER_ID);
        final Transaction tx2 = new Transaction(valueOf(2), ZERO, "Stake", "ref", 0l, valueOf(1000), gameId, tableId, SESSION_ID, PLAYER_ID);
        final Transaction tx3 = new Transaction(valueOf(3), ZERO, "Tournament Entry Fee", "ref", 0l, valueOf(1000), gameId, tableId, SESSION_ID, PLAYER_ID);
        final Collection<Transaction> transactions = Arrays.asList(tx1, tx2, tx3);
        when(lastPlayerPlayedEvents.getLastEventTimestampForAccount(any(BigDecimal.class))).thenReturn(-1l);
        when(dailyCycle.isInCurrentCycle(clock.getCurrentTime(), -1)).thenReturn(false);
        when(dailyCycle.isInCurrentCycle(clock.getCurrentTime(), tx2.getTimestamp())).thenReturn(true);
        underTest.track(transactions);
        verify(lastPlayerPlayedEvents, times(1)).registerEvent(eq(valueOf(2)), eq(PLAYER_ID), eq(new DateTime(0)));
        verify(lastPlayerPlayedEvents, never()).registerEvent(eq(valueOf(1)), eq(PLAYER_ID), any(DateTime.class));
        verify(lastPlayerPlayedEvents, never()).registerEvent(eq(valueOf(3)), eq(PLAYER_ID), any(DateTime.class));
    }

    @Test
    public void shouldStoreOnlyOneTransactionPerPeriod() {
        final Transaction tx1 = new Transaction(valueOf(1), ZERO, "Stake", "ref", 0l, valueOf(1000), gameId, tableId, SESSION_ID, PLAYER_ID);
        final Transaction tx2 = new Transaction(valueOf(1), ZERO, "Stake", "ref", 1l, valueOf(1000), gameId, tableId, SESSION_ID, PLAYER_ID);
        final Transaction tx3 = new Transaction(valueOf(1), ZERO, "Stake", "ref", 2l, valueOf(1000), gameId, tableId, SESSION_ID, PLAYER_ID);
        final Collection<Transaction> transactions = Arrays.asList(tx1, tx2, tx3);
        when(dailyCycle.isInCurrentCycle(clock.getCurrentTime(), -1)).thenReturn(false);
        when(dailyCycle.isInCurrentCycle(clock.getCurrentTime(), tx1.getTimestamp())).thenReturn(false);
        when(dailyCycle.isInCurrentCycle(clock.getCurrentTime(), tx2.getTimestamp())).thenReturn(true);
        when(dailyCycle.isInCurrentCycle(clock.getCurrentTime(), tx3.getTimestamp())).thenReturn(true);
        when(lastPlayerPlayedEvents.getLastEventTimestampForAccount(valueOf(1))).thenReturn(-1l, -1l, tx2.getTimestamp(), tx2.getTimestamp());
        underTest.track(transactions);
        verify(lastPlayerPlayedEvents, times(1)).registerEvent(eq(valueOf(1)), eq(PLAYER_ID), eq(new DateTime(1l)));
        verify(lastPlayerPlayedEvents, never()).registerEvent(eq(valueOf(1)), eq(PLAYER_ID), eq(new DateTime(2l)));
    }

    @Test
    public void shouldIgnoreTransactionWithoutTimestamp() {
        final Transaction tx = new Transaction(valueOf(2), ZERO, "Stake", "ref", null, valueOf(1000), gameId, tableId, SESSION_ID, PLAYER_ID);
        underTest.track(Arrays.asList(tx));
        verifyZeroInteractions(lastPlayerPlayedEvents);
    }

}
