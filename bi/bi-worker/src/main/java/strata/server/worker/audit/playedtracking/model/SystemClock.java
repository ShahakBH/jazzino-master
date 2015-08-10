package strata.server.worker.audit.playedtracking.model;

public class SystemClock implements Clock {
    @Override
    public long getCurrentTime() {
        return System.currentTimeMillis();
    }
}
