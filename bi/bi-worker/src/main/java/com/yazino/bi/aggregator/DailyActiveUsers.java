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

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

@Service
public class DailyActiveUsers extends Aggregator {

    private static final Logger LOG = LoggerFactory.getLogger(DailyActiveUsers.class);
    private static final String ID = "daily_active_users";

    public static final String DAILY_ACTIVE_USERS_INSERT = "insert into dau_mau "
            + "select ?::date , 'D', platform, game, count(distinct player_id) "
            + "from player_activity_daily "
            + "where activity_ts >= ?::date  "
            + "and activity_ts < ?::date + interval '1 day' "
            + "group by 1, 2, 3, 4 "
            + "union "
            + "select ?::date , 'D', platform, '*', count(distinct player_id) "
            + "from player_activity_daily "
            + "where activity_ts >= ?::date "
            + "and activity_ts < ?::date + interval '1 day' "
            + "group by 1, 2, 3, 4 "
            + "union "
            + "select ?::date , 'D', '*', game, count(distinct player_id) "
            + "from player_activity_daily "
            + "where activity_ts >= ?::date "
            + "and activity_ts < ?::date + interval '1 day' "
            + "group by 1, 2, 3, 4 "
            + "union "
            + "select ?::date , 'D', '*', '*', count(distinct player_id) "
            + "from player_activity_daily "
            + "where activity_ts >= ?::date "
            + "and activity_ts < ?::date + interval '1 day' "
            + "group by 1, 2, 3, 4 ";

    public static final String SELECT_MIN_ACTIVITY_TS_FROM_PLAYER_ACTIVITY_DAILY =
            "SELECT min(activity_ts) as activity_ts from player_activity_daily";

    // CGLIB constructor
    DailyActiveUsers() {
    }

    @Autowired
    public DailyActiveUsers(@Qualifier("externalDwJdbcTemplate") JdbcTemplate template,
                            AggregatorLastUpdateDAO aggregatorLastUpdateDAO,
                            AggregatorLockDao aggregatorLockDao,
                            YazinoConfiguration configuration) {

        super(template, aggregatorLastUpdateDAO, aggregatorLockDao, ID, configuration, PlayerActivityDaily.ID);
    }

    // 3:00am every morning MUST run after PAD
    @Scheduled(cron = "0 5 3 * * ?")
    public void update() {
        try {
            updateWithLocks(new Timestamp(new DateTime().getMillis()));
        } catch (Exception e) {
            LOG.error("failed to run update", e);
        }
    }

    @Transactional("externalDwTransactionManager")
    @Override
    public Timestamp materializeData(Timestamp timeLastRun, Timestamp toDate) {
        Timestamp startOfToDate = new Timestamp(new DateTime(toDate).withTimeAtStartOfDay().getMillis());

        DateTime fromDate = getEarliestDataDate(timeLastRun, SELECT_MIN_ACTIVITY_TS_FROM_PLAYER_ACTIVITY_DAILY);

        return executingQueryEveryDayForDateRange(startOfToDate, fromDate, DAILY_ACTIVE_USERS_INSERT);
    }

    protected PreparedStatementSetter getPreparedStatementSetter(final DateTime... dateTimeToRunQueryOn) {
        return new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps) throws SQLException {
                Date today = new Date(dateTimeToRunQueryOn[0].getMillis());
                //Yowzah!
                int parameterIndex = 1;
                ps.setDate(parameterIndex++, today);
                ps.setDate(parameterIndex++, today);
                ps.setDate(parameterIndex++, today);
                ps.setDate(parameterIndex++, today);
                ps.setDate(parameterIndex++, today);
                ps.setDate(parameterIndex++, today);
                ps.setDate(parameterIndex++, today);
                ps.setDate(parameterIndex++, today);
                ps.setDate(parameterIndex++, today);
                ps.setDate(parameterIndex++, today);
                ps.setDate(parameterIndex++, today);
                ps.setDate(parameterIndex++, today);
            }
        };
    }
}
