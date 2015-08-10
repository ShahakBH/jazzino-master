package strata.server.performance.rabbit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sleeps a {@link Thread} for the specified time.
 */
public class ThreadSleeper implements Sleeper {
    private static final Logger LOG = LoggerFactory.getLogger(ThreadSleeper.class);

    @Override
    public void sleep(final long time) throws InterruptedException {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Sleeping for [%d]ms", time));
        }
        Thread.sleep(time);
    }
}
