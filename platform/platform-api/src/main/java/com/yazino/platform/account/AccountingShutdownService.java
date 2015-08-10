package com.yazino.platform.account;

public interface AccountingShutdownService {
    void shutdownAccounting();

    void asyncShutdownAccounting();
}
