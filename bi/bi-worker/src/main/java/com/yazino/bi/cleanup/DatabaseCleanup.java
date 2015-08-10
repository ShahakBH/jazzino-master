package com.yazino.bi.cleanup;

import com.yazino.bi.aggregator.AggregatorLockDao;
import com.yazino.bi.aggregator.HostUtils;
import com.yazino.bi.aggregator.LockRetryer;
import com.yazino.bi.aggregator.Locker;
import com.yazino.configuration.YazinoConfiguration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import static org.joda.time.DateTime.now;

@Service
public class DatabaseCleanup {

    private final String tableId;
    private final AggregatorLockDao aggregatorLockDao;
    private final YazinoConfiguration configuration;
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseCleanup.class);
    private final String clientId;
    private final NamedParameterJdbcTemplate template;


    public DatabaseCleanup(final AggregatorLockDao aggregatorLockDao,
                           final YazinoConfiguration configuration,
                           final NamedParameterJdbcTemplate externalNamedParameterJdbcTemplate,
                           final String tableId) {
        this.aggregatorLockDao = aggregatorLockDao;
        this.configuration = configuration;
        this.template = externalNamedParameterJdbcTemplate;
        this.clientId = HostUtils.getHostName();
        this.tableId = tableId;
        LOG.info("started up DatabaseCleanup for {}", tableId);
    }

    //CGLIB
    DatabaseCleanup() {
        this.aggregatorLockDao = null;
        this.configuration = null;
        this.template = null;
        this.clientId = null;
        this.tableId = null;
    }

    public int run(String sql, SqlParameterSource paramSource) {
        final boolean enabled = configuration.getBoolean("strata.database.cleanup." + tableId + ".enable", false);
        if (!enabled) {
            LOG.info("strata.database.cleanup.{}.enable is disabled so skipping", tableId);
            return 0;
        }
        LOG.info("strata.database.cleanup.{}.enable is enabled so cleaning db", tableId);
        DateTime start = now();
        if (!acquireLock()) {
            LOG.info("{} is locked, skipping execution on client {}", tableId, clientId);
            return 0;
        }
        try {
            final int count = template.update(sql, paramSource);
            LOG.info("{} rows deleted in {} seconds", count, now().minus(start.getMillis()).getMillis() / 1000L);
            return count;
        } catch (Exception e) {
            LOG.error("Update of {} failed", tableId, e);
            return 0;
        } finally {
            releaseLock();
        }
    }

    private boolean acquireLock() {
        try {
            return new LockRetryer(
                    new Locker() {
                        @Override
                        public boolean lock() {
                            return aggregatorLockDao.lock(tableId, clientId);
                        }
                    }, configuration
            ).acquireLock();

        } catch (Exception e) {
            LOG.error("Lock acquisition failed for {} / {}", tableId, clientId, e);
            return false;
        }
    }

    private void releaseLock() {
        aggregatorLockDao.unlock(tableId, clientId);
    }

}
