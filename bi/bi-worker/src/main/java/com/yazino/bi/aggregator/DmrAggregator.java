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
public class DmrAggregator extends Aggregator {


    private static final Logger LOG = LoggerFactory.getLogger(DmrAggregator.class);
    private static final String FILL_DAILY_MANAGEMENT_REPORT =
            "insert into dmr_player_activity_and_purchases ("
                    + "select distinct "
                    + "    pad.player_id, '',"
                    + "    pad.platform, "
                    + "    pad.activity_ts::date activity_date, "
                    + "    pad.referrer, "
                    + "    pad.reg_ts::date registration_date, "
                    + "    pr.registration_platform, "
                    + "    r.num_registrations, "
                    + "    ppd.player_id player_purchase_daily_player_id, "
                    + "    ppd.registration_date player_purchase_daily_registration_date, "
                    + "    ppd.purchase_platform, "
                    + "    ppd.purchase_date, "
                    + "    ppd.total_amount_gbp, "
                    + "    ppd.num_purchases "
                    + "from player_activity_daily pad inner join player_referrer pr "
                    + "on pad.player_id = pr.player_id "
                    + "inner join registrations r "
                    + "on pad.reg_ts::date = r.registration_date "
                    + "and pr.registration_platform = r.registration_platform "
                    + "left join player_purchase_daily ppd "
                    + "on pad.player_id = ppd.player_id "
                    + "and pad.platform = ppd.purchase_platform "
                    + "and pad.activity_ts= ppd.purchase_date "
                    + "WHERE pad.activity_ts >= ? and pad.activity_ts < ?); ";

    private static final String FILL_REGISTRATIONS =
            "insert into dmr_registrations ("
                    + "select "
                    + "    r1.registration_date, "
                    + "    r1.registration_platform, "
                    + "    r1.registration_date - r2.registration_date days_ago, "
                    + "    r2.num_registrations "
                    + "from registrations r1, registrations r2 "
                    + "where r1.registration_platform = r2.registration_platform "
                    + "and r1.registration_date >= r2.registration_date "
                    + "and r1.registration_date>=? and r1.registration_date < ?)";

    private static final String DELETE_DMR_DATA = "delete from dmr_player_activity_and_purchases "
            + "where activity_date >= ? and activity_date < ?; ";

    private static final String DELETE_REGISTRATION_DATA = "delete from dmr_registrations "
            + "where registration_date >= ? and registration_date < ?; ";

    static final String ID = "daily_management_report";
    private static final String SELECT_MIN_AUDIT_TS_AS_AUDIT_TS_FROM_PLAYER_ACTIVITY_DAILY = "SELECT min(activity_ts) from player_activity_daily";

    @Autowired
    public DmrAggregator(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate template,
                         final AggregatorLastUpdateDAO aggregatorLastUpdateDAO,
                         final AggregatorLockDao aggregatorLockDAO,
                         final YazinoConfiguration configuration) {
        super(template, aggregatorLastUpdateDAO, aggregatorLockDAO, ID, configuration, PlayerActivityDaily.ID);
    }

    //CGLIB
    DmrAggregator() {
    }

    // after 2am (for BST) every night needs to run after the PAD
    @Scheduled(cron = "0 10 3 * * ?")
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
        DateTime fromDate = getEarliestDataDate(timeLastRun, SELECT_MIN_AUDIT_TS_AS_AUDIT_TS_FROM_PLAYER_ACTIVITY_DAILY);
        return executingQueryEveryDayForDateRange(startOfToDate,
                fromDate,
                new String[]{DELETE_REGISTRATION_DATA, DELETE_DMR_DATA, FILL_REGISTRATIONS, FILL_DAILY_MANAGEMENT_REPORT});
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
