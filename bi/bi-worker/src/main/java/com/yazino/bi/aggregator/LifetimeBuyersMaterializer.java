package com.yazino.bi.aggregator;

import com.yazino.configuration.YazinoConfiguration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

@Service
public class LifetimeBuyersMaterializer extends Aggregator {

    private static final Logger LOG = LoggerFactory.getLogger(LifetimeBuyersMaterializer.class);

    private static final String REFRESH = "REFRESH MATERIALIZED VIEW lifetime_value_buyers_mv";

    static final String ID = "lifetime_buyers_mv_refresh";

    @Autowired
    public LifetimeBuyersMaterializer(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate template,
                                      final AggregatorLastUpdateDAO aggregatorLastUpdateDAO,
                                      final AggregatorLockDao aggregatorLockDAO,
                                      final YazinoConfiguration configuration) {
        super(template, aggregatorLastUpdateDAO, aggregatorLockDAO, ID, configuration, null);
    }

    //CGLIB
    LifetimeBuyersMaterializer() {
    }

    @Scheduled(cron = "${strata.aggregators.lifetime-buyers.timing}")
    public void update() {
        try {
            updateWithLocks(new Timestamp(new DateTime().getMillis()));
        } catch (Exception e) {
            LOG.error("failed to run update", e);
        }
    }

    @Transactional("externalDwTransactionManager")
    public Timestamp materializeData(final Timestamp timeLastRun, final Timestamp toDate) {
        //just run once.
        return executingQueryEveryDayForDateRange(toDate, new DateTime(timeLastRun), REFRESH);
    }

    protected PreparedStatementSetter getPreparedStatementSetter(final DateTime... runDay) {
        return new PreparedStatementSetter() {
            @Override
            public void setValues(final PreparedStatement ps) throws SQLException {
                //nothing
            }
        };
    }

}
