package com.yazino.bi.aggregator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class AggregatorLockCleanup {
    private static final Logger LOG = LoggerFactory.getLogger(AggregatorLockCleanup.class);

    private final AggregatorLockDao aggregatorLockDao;

    @Autowired
    public AggregatorLockCleanup(AggregatorLockDao aggregatorLockDao) {
        this.aggregatorLockDao = aggregatorLockDao;
    }

    @PostConstruct
    public void cleanupLocks() {
        final String hostname = HostUtils.getHostName();
        LOG.info("Clearing locks for client {}", hostname);
        try {
            aggregatorLockDao.clearLocks(hostname);
        } catch (Exception e) {
            LOG.error("couldn't clear locks.", e);
        }
    }

}
