package com.yazino.bi.aggregator;

import com.yazino.configuration.YazinoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class AggregatorLockDao {
    private static final Logger LOG = LoggerFactory.getLogger(AggregatorLockDao.class);

    private static final String SELECT_LOCKS = "SELECT lock_client FROM aggregator_lock WHERE id=?";
    private static final String LOCK_TABLE = "LOCK TABLE aggregator_lock NOWAIT";
    private static final String INSERT_LOCK = "INSERT INTO aggregator_lock (id, lock_client) VALUES (?, ?)";

    private static final String DELETE_LOCK = "DELETE FROM aggregator_lock WHERE id=? AND lock_client=?";

    private final JdbcTemplate template;
    private final YazinoConfiguration configuration;

    // CGLib constructor
    AggregatorLockDao() {
        this.template = null;
        this.configuration = null;
    }

    @Autowired
    public AggregatorLockDao(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate template,
                             final YazinoConfiguration configuration) {
        notNull(template, "template may not be null");
        notNull(configuration, "config can't be null");

        this.configuration = configuration;
        this.template = template;
    }

    @Transactional(value = "externalDwTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public boolean lock(final String aggregatorId, final String clientId) {
        verifyInitialisation();
        notNull(aggregatorId, "aggregatorId may not be null");
        notNull(clientId, "clientId may not be null");

        LOG.debug("Attempting to acquire {} lock for {} on aggregator_lock table", aggregatorId, clientId);
        template.execute(LOCK_TABLE);

        final List<String> locks = template.query(SELECT_LOCKS, new RowMapper<String>() {
            @Override
            public String mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                return rs.getString("lock_client");
            }
        }, aggregatorId);

        if (locks.isEmpty()) {
            template.update(INSERT_LOCK, aggregatorId, clientId);
            LOG.debug("table locked by {}, {}", aggregatorId, clientId);
            return true;
        }

        LOG.debug("{} ALREADY locked by {}, you can go away now", aggregatorId, locks.get(0));

        return false;
    }

    private void verifyInitialisation() {
        if (template == null || configuration == null) {
            throw new IllegalStateException("Class was created with CGLib constructor");
        }
    }

    public void unlock(final String aggregatorId, final String clientId) {
        notNull(aggregatorId, "aggregatorId may not be null");
        notNull(clientId, "clientId may not be null");

        try {
            final int locksDeleted = template.update(DELETE_LOCK, aggregatorId, clientId);
            if (locksDeleted == 0) {
                throw new IllegalStateException("This client does not hold the lock for " + aggregatorId);
            }
            LOG.debug("lock for {} released for {} on aggregator_lock table", aggregatorId, clientId);
        } catch (Exception e) {
            LOG.error("Lock release failed for {} / {}", aggregatorId, clientId, e);
        }
    }

    public void clearLocks(String clientId) {
        template.update("DELETE FROM aggregator_lock WHERE lock_client=?", clientId);
    }
}
