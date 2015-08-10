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
public class PlayerActivityHourly extends Aggregator {

    private static final Logger LOG = LoggerFactory.getLogger(PlayerActivityHourly.class);
    static final String ID = "player_activity_hourly";

    static final String FILL_PLAYER_ACTIVITY_HOURLY =
            "INSERT INTO STG_PLAYER_ACTIVITY_HOURLY "
                    + "SELECT DISTINCT ac.player_id, gvt.game_type, "
                    + "first_value(platform) over (partition by acs.start_ts, acs.account_id "
                    + "order by start_ts desc rows between unbounded preceding and unbounded following) platform, "
                    + "date_trunc('hour', ac.audit_ts),  "
                    + "first_value(audit_ts) over "
                    + "  (partition by platform, game_type, acs.account_id "
                    + "   order by audit_ts desc rows between unbounded preceding and unbounded following) "
                    + "FROM audit_command ac "
                    + "JOIN table_definition td ON ac.table_id = td.table_id "
                    + "JOIN game_variation_template gvt ON td.game_variation_template_id = gvt.game_variation_template_id "
                    + "LEFT JOIN player_definition p ON ac.player_id = p.player_id "
                    + "LEFT JOIN account_session acs ON p.account_id = acs.account_id "
                    + "WHERE ac.command_type::text <> 'Leave'::text AND ac.command_type::text <> 'GetStatus'::text "
                    + "and start_ts < audit_ts and start_ts >= audit_ts - interval '1 day' "
                    + "AND ac.audit_ts between ? AND ?";

    static final String SQL_EXECUTE_UPDATES = "UPDATE PLAYER_ACTIVITY_HOURLY SET "
            + "last_played_ts=stg.last_played_ts "
            + "FROM stg_player_activity_hourly stg "
            + "WHERE "
            + "PLAYER_ACTIVITY_HOURLY.player_id = stg.player_id AND "
            + "PLAYER_ACTIVITY_HOURLY.game = stg.game AND "
            + "PLAYER_ACTIVITY_HOURLY.platform = stg.platform AND "
            + "PLAYER_ACTIVITY_HOURLY.activity_ts = stg.activity_ts";

    static final String SQL_EXECUTE_INSERTS =
            "INSERT INTO PLAYER_ACTIVITY_HOURLY "
                    + "select stg.* from STG_PLAYER_ACTIVITY_HOURLY stg "
                    + "LEFT JOIN PLAYER_ACTIVITY_HOURLY pah ON "
                    + "pah.player_id= stg.player_id AND "
                    + "pah.game= stg.game AND "
                    + "pah.platform= stg.platform AND "
                    + "pah.activity_ts=stg.activity_ts "
                    + "WHERE pah.PLAYER_ID is null ";

    static final String SQL_CLEAN_STAGING = "delete from stg_player_activity_hourly";

    private static final String SELECT_MIN_AUDIT_TS_FROM_AUDIT_COMMAND = "SELECT min(audit_ts) from audit_command";

    // CGLIB Constructor
    PlayerActivityHourly() {
    }

    @Autowired
    public PlayerActivityHourly(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate template,
                                final AggregatorLastUpdateDAO aggregatorLastUpdateDAO,
                                final AggregatorLockDao aggregatorLockDao,
                                final YazinoConfiguration configuration) {

        super(template, aggregatorLastUpdateDAO, aggregatorLockDao, ID, configuration, null);

    }

    @Scheduled(cron = "0 10 * * * ?")//every hour at 10 past the hour
    public void update() {
        try {
            updateWithLocks(new Timestamp(new DateTime().getMillis()));
        } catch (Exception e) {
            LOG.error("failed to run update", e);
        }
    }

    @Transactional("externalDwTransactionManager")
    public Timestamp materializeData(final Timestamp timeLastRun, final Timestamp currentTimestamp) {

        DateTime fromDate = getEarliestDataDate(timeLastRun, SELECT_MIN_AUDIT_TS_FROM_AUDIT_COMMAND);

        final Timestamp timestamp = executingQueryEveryDayForDateRange(currentTimestamp, fromDate, FILL_PLAYER_ACTIVITY_HOURLY);

        getTemplate().batchUpdate(new String[]{
                SQL_EXECUTE_UPDATES,
                SQL_EXECUTE_INSERTS,
                SQL_CLEAN_STAGING});
        return timestamp;
    }

    protected PreparedStatementSetter getPreparedStatementSetter(final DateTime... runDate) {
        return new PreparedStatementSetter() {
            @Override
            public void setValues(final PreparedStatement ps) throws SQLException {
                LOG.debug("running from " + new Timestamp(new DateTime(runDate[0])
                        .withMinuteOfHour(0)
                        .withSecondOfMinute(0)
                        .withMillisOfSecond(0).minusHours(1)
                        .getMillis()) + " to " + new Timestamp(new DateTime(runDate[1])
                        .withMinuteOfHour(0)
                        .withSecondOfMinute(0)
                        .withMillisOfSecond(0)
                        .getMillis()));
                ps.setTimestamp(1, new Timestamp(new DateTime(runDate[0])
                        .withMinuteOfHour(0)
                        .withSecondOfMinute(0)
                        .withMillisOfSecond(0).minusHours(1)
                        .getMillis()));
                ps.setTimestamp(2, new Timestamp(new DateTime(runDate[1])
                        .withMinuteOfHour(0)
                        .withSecondOfMinute(0)
                        .withMillisOfSecond(0)
                        .getMillis()));
            }
        };
    }

}
