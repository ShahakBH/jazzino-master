package strata.server.worker.audit.playedtracking;

import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.yazino.platform.audit.message.Transaction;
import strata.server.worker.audit.playedtracking.model.Clock;
import strata.server.worker.audit.playedtracking.model.DailyCycle;
import strata.server.worker.audit.playedtracking.model.SystemClock;

import java.math.BigDecimal;
import java.util.Collection;

@Component("playerPlayedTracking")
public class PlayerPlayedTracking {
    private final LastPlayerPlayedEvents lastPlayerPlayedEvents;
    private final DailyCycle cycle = new DailyCycle();
    private final Clock clock = new SystemClock();
    private static final String TX_TYPE = "Stake";

    @Autowired
    public PlayerPlayedTracking(final LastPlayerPlayedEvents lastPlayerPlayedEvents) {
        this.lastPlayerPlayedEvents = lastPlayerPlayedEvents;
    }

    public void track(final Collection<Transaction> transactions) {
        for (final Transaction tx : transactions) {
            if (isRelevantTransaction(tx)) {
                createEventForFirstTransactionOfCycle(tx);
            }
        }
    }

    private boolean isRelevantTransaction(final Transaction tx) {
        return tx.getTimestamp() != null && TX_TYPE.equals(tx.getType());
    }

    private void createEventForFirstTransactionOfCycle(final Transaction tx) {
        final BigDecimal accountId = tx.getAccountId();
        final long lastTimestamp = lastPlayerPlayedEvents.getLastEventTimestampForAccount(accountId);
        final long newTimestamp = tx.getTimestamp();
        if (isRelevantTimestamp(lastTimestamp, newTimestamp)) {
            lastPlayerPlayedEvents.registerEvent(accountId, tx.getPlayerId(), new DateTime(newTimestamp));
        }
    }

    private boolean isRelevantTimestamp(final long lastEvent, final long newEvent) {
        final long currentTime = clock.getCurrentTime();
        return !cycle.isInCurrentCycle(currentTime, lastEvent)
                && cycle.isInCurrentCycle(currentTime, newEvent);
    }
}
