package strata.server.worker.audit.playedtracking.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class DailyCycle {

    private static final DateTimeZone NEW_YORK = DateTimeZone.forID("America/New_York");

    public boolean isInCurrentCycle(final long currentTime,
                                    final long eventTime) {
        final DateTime today = new DateTime(currentTime, NEW_YORK);
        final DateTime event = new DateTime(eventTime, NEW_YORK);
        return event.getDayOfYear() == today.getDayOfYear();
    }
}
