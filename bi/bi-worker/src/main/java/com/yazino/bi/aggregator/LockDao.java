package com.yazino.bi.aggregator;

public interface LockDao {
    boolean lock(String aggregatorId, String clientId);

    void unlock(String aggregatorId, String clientId);

    void clearLocks(String clientId);
}
