package strata.server.performance.rabbit;

/**
 * An interface for providing sleep facilities. Useful for abstracting Thread.sleep calls when testing.
 */
public interface Sleeper {
    /**
     * Sleep for a certain amount of time.
     *
     * @param time the time in millis
     * @throws InterruptedException, should it be interrupted
     */
    void sleep(long time) throws InterruptedException;
}
