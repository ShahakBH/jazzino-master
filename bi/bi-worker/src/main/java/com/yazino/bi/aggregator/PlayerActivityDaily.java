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
public class PlayerActivityDaily extends Aggregator {

    private static final Logger LOG = LoggerFactory.getLogger(PlayerActivityDaily.class);
    static final String ID = "player_activity_daily";
    private static final String SELECT_MIN_AUDIT_TS_AS_AUDIT_TS_FROM_AUDIT_COMMAND = "SELECT min(activity_ts) from player_activity_hourly";

    //CGLIB
    PlayerActivityDaily() {
    }

    public static final String FILL_PLAYER_ACTIVITY_DAILY = "INSERT INTO player_activity_daily "
            + "SELECT DISTINCT "
            + " a.player_id, game, platform, activity_ts::date, registration_referrer, created_ts "
            + "FROM public.player_activity_hourly a left join player_definition b "
            + "ON a.player_id = b.player_id "
            + "LEFT JOIN player_referrer c "
            + "ON b.player_id = c.player_id "
            + "WHERE activity_ts >= ? and activity_ts < ?";


    private static final String DELETE_REGISTRATION_DATA = "DELETE FROM player_activity_daily "
            + "where activity_ts >= ? and activity_ts < ?; ";

    @Autowired
    public PlayerActivityDaily(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate template,
                               final AggregatorLastUpdateDAO aggregatorLastUpdateDAO,
                               final AggregatorLockDao aggregatorLockDao,
                               final YazinoConfiguration configuration) {

        super(template, aggregatorLastUpdateDAO, aggregatorLockDao, ID, configuration, PlayerActivityHourly.ID);

    }

    // after 2am (for BST) every night needs to run before the DAU and MAU
    @Scheduled(cron = "0 10 2 * * ?")
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
        DateTime fromDate = getEarliestDataDate(timeLastRun, SELECT_MIN_AUDIT_TS_AS_AUDIT_TS_FROM_AUDIT_COMMAND);

        executingQueryEveryDayForDateRange(startOfToDate, fromDate, false, DELETE_REGISTRATION_DATA);
        return executingQueryEveryDayForDateRange(startOfToDate, fromDate, FILL_PLAYER_ACTIVITY_DAILY);
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
