package com.yazino.bi.aggregator;

import com.yazino.configuration.YazinoConfiguration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import static java.lang.String.format;
import static org.apache.commons.lang3.Validate.notNull;

public abstract class Aggregator {
    private static final Logger LOG = LoggerFactory.getLogger(Aggregator.class);

    private final JdbcTemplate template;
    private final AggregatorLastUpdateDAO aggregatorLastUpdateDAO;
    private final AggregatorLockDao aggregatorLockDao;
    private final String clientId;
    private final String aggregatorId;
    private final YazinoConfiguration configuration;
    private final String dependentUpon;

    protected Aggregator(final JdbcTemplate template,
                         final AggregatorLastUpdateDAO aggregatorLastUpdateDAO,
                         final AggregatorLockDao aggregatorLockDao,
                         final String aggregatorId,
                         final YazinoConfiguration configuration,
                         final String dependentUpon) {
        notNull(template, "template may not be null");
        notNull(aggregatorLastUpdateDAO, "aggregatorLastUpdatesDAO may not be null");
        notNull(aggregatorLockDao, "aggregatorLockDAO may not be null");
        notNull(aggregatorId, "aggregator Id cannot be null");
        notNull(configuration, "Yazino Configuration should not be empty");

        this.template = template;
        this.aggregatorLastUpdateDAO = aggregatorLastUpdateDAO;
        this.aggregatorLockDao = aggregatorLockDao;
        this.configuration = configuration;
        this.clientId = HostUtils.getHostName();
        this.aggregatorId = aggregatorId;
        this.dependentUpon = dependentUpon;
    }

    //CGlib Constructor
    Aggregator() {
        this.template = null;
        this.aggregatorId = null;
        this.aggregatorLockDao = null;
        this.clientId = null;
        this.aggregatorLastUpdateDAO = null;
        this.configuration = null;
        this.dependentUpon = null;
    }

    private boolean acquireLock() {
        try {
            return new LockRetryer(new Locker() {
                @Override
                public boolean lock() {
                    return aggregatorLockDao.lock(aggregatorId, clientId);
                }
            }, configuration).acquireLock();

        } catch (Exception e) {
            LOG.error("Lock acquisition failed for {} / {}", aggregatorId, clientId, e);
            return false;
        }
    }

    private void releaseLock() {
        aggregatorLockDao.unlock(aggregatorId, clientId);
    }

    protected Timestamp executingQueryEveryDayForDateRange(final Timestamp toDateTs,
                                                           final DateTime fromDate,
                                                           final String... materialisationQuery) {
        return executingQueryEveryDayForDateRange(toDateTs, fromDate, true, materialisationQuery);
    }

    public String getDependentUpon() {
        return dependentUpon;
    }

    protected Timestamp executingQueryEveryDayForDateRange(final Timestamp toDateTs,
                                                           final DateTime fromDate,
                                                           final Boolean runUpdateLastRun,
                                                           final String... materialisationQueries) {
        if (fromDate == null) {
            return null;
        }
        DateTime toDate = new DateTime(toDateTs);
        DateTime runDay = fromDate;
        final DateTime dependencyLastRunOn;
        if (getDependentUpon() != null) {
            dependencyLastRunOn = new DateTime(aggregatorLastUpdateDAO.getLastRun(getDependentUpon()));
        } else {
            dependencyLastRunOn = null;
        }
        if (dependantHasCompleted(dependencyLastRunOn, toDate)) {
            while (runDay.isBefore(toDate)) {
                try {
                    LOG.info("running {} materialiser for dates: {} to {}", getAggregatorId(), runDay, runDay.plusDays(1));
                    final PreparedStatementSetter preparedStatementSetter = getPreparedStatementSetter(runDay, runDay.plusDays(1));
                    for (String query : materialisationQueries) {
                        LOG.debug(query);
                        getTemplate().update(query, preparedStatementSetter);
                    }
                } catch (Exception e) {
                    LOG.error("caught problem inserting data {}", getAggregatorId(), e);
                    LOG.info("setting the last update for {} to {}", getAggregatorId(), runDay);
                    return new Timestamp(runDay.getMillis());

                }
                runDay = runDay.plusDays(1);
                if (toDate.isBefore(runDay)) {
                    runDay = toDate;
                }
                if (runUpdateLastRun) {
                    LOG.info("setting the last update for {} to {}", getAggregatorId(), runDay);
                    getAggregatorLastUpdateDAO().updateLastRunFor(aggregatorId, new Timestamp(runDay.getMillis()));
                }
            }

            return new Timestamp(runDay.getMillis());
        } else {
            LOG.info(
                    "skipping {} as the dependent aggregator {} seems to have only been run up to {} which is not later than this run for {}",
                    getAggregatorId(),
                    getDependentUpon(),
                    dependencyLastRunOn,
                    runDay);
            return null;
        }

    }

    boolean dependantHasCompleted(final DateTime dependencyLastRunOn, final DateTime toDate) {
        LOG.debug("is {} less than {}", toDate, dependencyLastRunOn);
        return dependencyLastRunOn == null || toDate.isBefore(dependencyLastRunOn) || toDate.isEqual(dependencyLastRunOn);
    }

    protected abstract PreparedStatementSetter getPreparedStatementSetter(final DateTime... runDay);

    public String getClientId() {
        return clientId;
    }

    public String getAggregatorId() {
        return aggregatorId;
    }

    protected abstract void update();

    public void updateWithLocks(Timestamp currentTimestamp) {
        if (!(configuration.getBoolean("data-warehouse.write.enabled"))) {
            LOG.info("Aggregation of {} is currently off because Redshift is not enabled", clientId);
            return;
        }
        if (aggregatorTurnedOff()) {
            return;
        }

        if (!acquireLock()) {
            LOG.info("{} is locked, skipping execution on client {}", getAggregatorId(), getClientId());
            return;
        }

        try {
            final Timestamp timeLastRun = aggregatorLastUpdateDAO.getLastRun(getAggregatorId());
            LOG.info("Update of {} starting, last run time was {}", getAggregatorId(), timeLastRun);

            final Timestamp lastRun = materializeData(timeLastRun, currentTimestamp);
            if (lastRun == null) {
                LOG.info("No data has been materialized since there is no data to update, {}", getAggregatorId());
            } else {
                aggregatorLastUpdateDAO.updateLastRunFor(getAggregatorId(), lastRun);
                //need to record last run
            }

        } catch (Exception e) {
            LOG.error("Update of {} failed", getAggregatorId(), e);

        } finally {
            releaseLock();
        }
    }

    private boolean aggregatorTurnedOff() {
        if (!(configuration.getBoolean("data-warehouse.aggregators.enabled"))) {
            LOG.info("All aggregation on {} is currently turned off", getAggregatorId(), clientId);
            return true;
        }
        if (specificAggregatorDisabled()) {
            LOG.info("Aggregation of {} on {} is currently turned off", getAggregatorId(), clientId);
            return true;
        }
        return false;
    }

    private boolean specificAggregatorDisabled() {
        final String key = format("data-warehouse.aggregators.%s.disabled", aggregatorId);
        return configuration.containsKey(key) && configuration.getBoolean(key);
    }

    protected DateTime getEarliestDataDate(Timestamp fromDate, String getEarliestDataDateSQL) {
        DateTime fromDateTime = dateTimeOf(fromDate);

        if (fromDateTime == null) {
            fromDateTime = getTemplate().queryForObject(getEarliestDataDateSQL, getDateTimeRowMapper());
            if (fromDateTime == null) {
                return null;
            }
        }
        LOG.info("from date is {}", fromDateTime);
        return fromDateTime;
    }

    public abstract Timestamp materializeData(final Timestamp timeLastRun, final Timestamp toDate);

    public JdbcTemplate getTemplate() {
        return template;
    }

    public AggregatorLastUpdateDAO getAggregatorLastUpdateDAO() {
        return aggregatorLastUpdateDAO;
    }

    public AggregatorLockDao getAggregatorLockDao() {
        return aggregatorLockDao;
    }

    public DateTime dateTimeOf(Timestamp timeLastRun) {
        if (timeLastRun != null) {
            return new DateTime(timeLastRun);
        }
        return null;
    }

    private RowMapper<DateTime> getDateTimeRowMapper() {
        return new RowMapper<DateTime>() {
            @Override
            public DateTime mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new DateTime(rs.getTimestamp(1));
            }
        };
    }

}
