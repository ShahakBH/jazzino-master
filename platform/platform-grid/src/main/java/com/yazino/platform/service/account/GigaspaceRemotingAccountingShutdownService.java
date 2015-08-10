package com.yazino.platform.service.account;

import com.yazino.platform.account.AccountingShutdownService;
import com.yazino.platform.processor.PersisterContainerSpawner;
import org.openspaces.remoting.RemotingService;
import org.springframework.beans.factory.annotation.Autowired;

import static org.apache.commons.lang3.Validate.notNull;

@RemotingService
public class GigaspaceRemotingAccountingShutdownService implements AccountingShutdownService {
    private final PersisterContainerSpawner spawner;

    @Autowired
    public GigaspaceRemotingAccountingShutdownService(final PersisterContainerSpawner spawner) {
        notNull(spawner, "spawner is null");

        this.spawner = spawner;
    }

    @Override
    public void shutdownAccounting() {
        spawner.createContainers();
    }

    @Override
    public void asyncShutdownAccounting() {
        // as per http://www.gigaspaces.com/wiki/display/XAP7/Executor+Based+Remoting, the
        // implementation is empty and #shutdownAccounting will be invoked
    }
}
