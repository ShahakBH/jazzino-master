package com.yazino.model.log;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LogStorageTest {

    @Test
    public void loggedEntriesCanBeRetrievedOnlyOnce() {
        final LogStorage logStorage = new LogStorage();
        logStorage.log("log1");
        logStorage.log("log2");
        assertEquals("[{\"c\":\"\",\"v\":\"log1\"},{\"c\":\"\",\"v\":\"log2\"}]", logStorage.popJSON());
        assertEquals("[]", logStorage.popJSON());
    }

    @Test
    public void loggedEntriesCanBeCategorized() {
        final LogStorage logStorage = new LogStorage();
        logStorage.log("cat1", "log1");
        logStorage.log("cat2", "log2");
        assertEquals("[{\"c\":\"cat1\",\"v\":\"log1\"},{\"c\":\"cat2\",\"v\":\"log2\"}]", logStorage.popJSON());
    }

    @Test
    public void oldestLoggedEntriesAreDiscardedIfLimitIsReached() {
        final LogStorage logStorage = new LogStorage(1);
        logStorage.log("cat1", "log1");
        logStorage.log("cat2", "log2");
        logStorage.log("cat3", "log3");
        assertEquals("[{\"c\":\"cat3\",\"v\":\"log3\"}]", logStorage.popJSON());
    }
}
