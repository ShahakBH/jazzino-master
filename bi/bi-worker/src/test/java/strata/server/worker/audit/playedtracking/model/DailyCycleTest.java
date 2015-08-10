package strata.server.worker.audit.playedtracking.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DailyCycleTest {

    private static final DateTimeZone NEW_YORK = DateTimeZone.forID("America/New_York");

    public static final long CURRENT_TIME = new DateTime(2010, 1, 2, 13, 30, 45, 700, NEW_YORK).getMillis();

    @Test
    public void shouldDetermineIfInCurrentCycle() {
        final DailyCycle underTest = new DailyCycle();
        assertFalse(underTest.isInCurrentCycle(CURRENT_TIME, new DateTime(2010, 1, 1, 23, 59, 59, 999, NEW_YORK).getMillis()));
        assertTrue(underTest.isInCurrentCycle(CURRENT_TIME, new DateTime(2010, 1, 2, 0, 0, 0, 0, NEW_YORK).getMillis()));
        assertTrue(underTest.isInCurrentCycle(CURRENT_TIME, new DateTime(2010, 1, 2, 23, 59, 59, 999, NEW_YORK).getMillis()));
        assertFalse(underTest.isInCurrentCycle(CURRENT_TIME, new DateTime(2010, 1, 3, 0, 0, 0, 0, NEW_YORK).getMillis()));
    }
}
