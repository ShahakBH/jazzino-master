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
public class AdNetBandsAggregator extends Aggregator {


    private static final Logger LOG = LoggerFactory.getLogger(AdNetBandsAggregator.class);

    private static final String DELETE_BANDS_DATA = "delete from adnet_registration_bands "
            + "where registration_date >= ? and registration_date < ?";

    private static final String UPDATE_REGISTRATION_BANDS = "insert into adnet_registration_bands\n"
            + " select\n"
            + "    registration_date,\n"
            + "    registration_adnet,\n"
            + "    case\n"
            + "        when days_ago = 0 then '0 days'\n"
            + "        when days_ago = 1 then '1 day'\n"
            + "        when days_ago <= 6 then '1st wk'\n"
            + "        when days_ago <= 27 then '2-4 wks'\n"
            + "        when days_ago <= 364 then '2-12 m'\n"
            + "        else '1+ y'\n"
            + "    end band,\n"
            + "    sum(num_registrations) num_registrations\n"
            + "from adnet_registrations\n"
            + "where registration_date >= ? and registration_date < ?\n"
            + "group by 1, 2, 3;";


    static final String ID = "adnet_bands";
    private static final String SELECT_MIN_REGISTRATION_DATE = "SELECT min(registration_date) from adnet_registrations";


    @Autowired
    public AdNetBandsAggregator(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate template,
                                final AggregatorLastUpdateDAO aggregatorLastUpdateDAO,
                                final AggregatorLockDao aggregatorLockDAO,
                                final YazinoConfiguration configuration) {
        super(template, aggregatorLastUpdateDAO, aggregatorLockDAO, ID, configuration, AdNetworkAggregator.ID);
    }

    //CGLIB
    AdNetBandsAggregator() {
    }

    @Scheduled(cron = "0 40 6 * * ?")
    public void update() {
        try {
            updateWithLocks(new Timestamp(new DateTime().getMillis()));
        } catch (Exception e) {
            LOG.error("failed to run update", e);
        }
    }

    @Transactional("externalDwTransactionManager")
    public Timestamp materializeData(final Timestamp timeLastRun, final Timestamp toDate) {
        Timestamp startOfToDate = new Timestamp(new DateTime(toDate).withTimeAtStartOfDay().getMillis());
        DateTime fromDate = getEarliestDataDate(timeLastRun, SELECT_MIN_REGISTRATION_DATE);
        return executingQueryEveryDayForDateRange(startOfToDate, fromDate, DELETE_BANDS_DATA, UPDATE_REGISTRATION_BANDS);
    }


    protected PreparedStatementSetter getPreparedStatementSetter(final DateTime... runDay) {
        return new PreparedStatementSetter() {
            @Override
            public void setValues(final PreparedStatement ps) throws SQLException {
                ps.setTimestamp(1, new Timestamp(runDay[0].withTimeAtStartOfDay().getMillis()));
                ps.setTimestamp(2, new Timestamp(runDay[1].withTimeAtStartOfDay().getMillis()));
            }
        };
    }

}
