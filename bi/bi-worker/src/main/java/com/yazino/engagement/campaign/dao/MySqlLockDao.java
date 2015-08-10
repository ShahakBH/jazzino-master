package com.yazino.engagement.campaign.dao;

import com.yazino.bi.aggregator.LockDao;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class MySqlLockDao implements LockDao {
    private static final Logger LOG = LoggerFactory.getLogger(MySqlLockDao.class);

    private final JdbcTemplate jdbc;

    //CGLIB Constructor
    public MySqlLockDao() {
        this.jdbc = null;
    }

    @Autowired
    public MySqlLockDao(@Qualifier("dwJdbcTemplate") final JdbcTemplate jdbc) {
        Validate.notNull(jdbc, "jdbc template cannot be null");
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public boolean lock(final String lockName, final String clientId) {
        try {
            verifyInitialisation();
            notNull(lockName, "lockname may not be null");
            notNull(clientId, "clientId may not be null");

            LOG.debug("Locking table for update for {} on worker_lock table", lockName);
            final List<String> locks = jdbc.query("SELECT LOCK_CLIENT from WORKER_LOCK where id=? FOR UPDATE", new RowMapper<String>() {
                @Override
                public String mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                    return rs.getString("lock_client");
                }
            }, lockName);

            if (locks.isEmpty()) {
                jdbc.update("INSERT INTO WORKER_LOCK (id, lock_client) VALUES (?,?)", lockName, clientId);
                LOG.debug("table locked by {}, {}", lockName, clientId);
                return true;
            }
        } catch (Exception e) {
            LOG.info("Lock acquisition failed for {} / {}", lockName, clientId, e);
        }
        return false;
    }

    @Override
    public void unlock(final String lockName, final String clientId) {
        notNull(lockName, "lockName may not be null");
        notNull(clientId, "clientId may not be null");

        try {
            final int locksDeleted = jdbc.update("DELETE FROM WORKER_LOCK where id=? and lock_client=?", lockName, clientId);
            if (locksDeleted == 0) {
                throw new IllegalStateException("This client does not hold the lock for " + lockName);
            }
            LOG.debug("lock released for {} on aggregator_lock table", clientId);
        } catch (Exception e) {
            LOG.error("Lock release failed for {} / {}", lockName, clientId, e);
        }
    }

    @Override
    public void clearLocks(final String clientId) {
        jdbc.update("delete from WORKER_LOCK where lock_client=?", clientId);
    }

    private void verifyInitialisation() {
        if (jdbc == null) {
            throw new IllegalStateException("Class was created with CGLib constructor");
        }
    }
}
