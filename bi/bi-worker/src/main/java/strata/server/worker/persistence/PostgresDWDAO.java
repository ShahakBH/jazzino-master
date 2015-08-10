package strata.server.worker.persistence;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;

public abstract class PostgresDWDAO<T> {
    private static final Logger LOG = LoggerFactory.getLogger(PostgresDWDAO.class);

    private JdbcTemplate jdbcTemplate;

    protected PostgresDWDAO(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private void verifyInitialisation() {
        if (jdbcTemplate == null) {
            throw new IllegalStateException("Class was initialised using CGLib constructor");
        }
    }

    public void save(Set<T> events) {
        final List<T> list = newArrayList();
        list.addAll(events);
        saveAll(list);
    }

    @Transactional("externalDwTransactionManager")
    public void saveAll(List<T> events) {
        Validate.notNull(events, "events may not be null");

        verifyInitialisation();

        if (events.isEmpty()) {
            return;
        }


        LOG.debug("Handling events: {}", events);

        try {
            String[] batchUpdates = getBatchUpdates(events);
            if (!StringUtils.isBlank(batchUpdates[0])) {
                jdbcTemplate.batchUpdate(batchUpdates);
                LOG.debug("Inserted {} events", events.size());
            } else {
                LOG.debug("No events contain client context");
            }


        } catch (final DataAccessResourceFailureException e) {
            LOG.warn("Cannot connect to database, returning to queue", e);
            throw e;

        } catch (final TransientDataAccessException e) {
            LOG.warn("Transient failure while writing to database, returning to queue", e);
            throw e;

        } catch (final Exception e) {
            LOG.error("Save failed for the beans:\n {}", events, e);
            logBatchExceptionsFrom(e);
        }
    }


    protected abstract String[] getBatchUpdates(final List<T> events);

    private void logBatchExceptionsFrom(final Exception e) {
        try {
            if (e.getMessage().contains("Call getNextException")) {
                final BatchUpdateException batchUpdateException = batchUpdateExceptionFrom(e);
                if (batchUpdateException != null) {
                    SQLException nextException = batchUpdateException;
                    while ((nextException = nextException.getNextException()) != null) {
                        LOG.error("Batch update: next exception is {}", nextException);
                    }
                }
            }
        } catch (Exception unexpected) {
            LOG.error("Received unexpected exception when logging batch exceptions.", unexpected);
        }
    }

    private BatchUpdateException batchUpdateExceptionFrom(final Throwable e) {
        if (e == null || e instanceof BatchUpdateException) {
            return (BatchUpdateException) e;
        }

        return batchUpdateExceptionFrom(e.getCause());
    }

}
