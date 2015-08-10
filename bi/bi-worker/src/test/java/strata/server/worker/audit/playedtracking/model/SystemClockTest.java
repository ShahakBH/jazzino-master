package strata.server.worker.audit.playedtracking.model;

import org.junit.Test;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class SystemClockTest {
    @Test
    public void shouldReturnSystemTime() {
        final long t = System.currentTimeMillis();
        final long t1 = new SystemClock().getCurrentTime();
        assertThat(t1, is(greaterThanOrEqualTo(t)));
    }
}
