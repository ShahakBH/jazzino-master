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
public class EngagementByPlatform extends Aggregator {

    private static final Logger LOG = LoggerFactory.getLogger(EngagementByPlatform.class);
    static final String ID = "engagement_by_platform";
    private static final String SELECT_MIN_TRANSACTION_TS_FROM_TRANSACTION_LOG = "SELECT min(transaction_ts) from transaction_log";

    //CGLIB
    EngagementByPlatform() {
    }

    public static final String FILL_ENGAGEMENT_BY_PLATFORM =
            "INSERT INTO engagement_by_platform "
                    + "(select\n"
                    + "      ?::date,\n"
                    + "      coalesce(platform, 'UNKNOWN') platform,\n"
                    + "      gvt.name game_variation_template_name,\n"
                    + "      transaction_type,\n"
                    + "      count(distinct tl.account_id) num_players,\n"
                    + "      count(1) num_transactions,\n"
                    + "      sum(amount) total_amount\n"
                    + "  from transaction_log tl\n"
                    + "  inner join table_definition td\n"
                    + "     on tl.table_id = td.table_id\n"
                    + "  inner join game_variation_template gvt\n"
                    + "     on td.game_variation_template_id = gvt.game_variation_template_id\n"
                    + "  left join account_session acs\n"
                    + "     on tl.session_id = acs.session_id\n"
                    + "  where transaction_ts >= ?\n"
                    + "  and transaction_ts < ?::date + interval '1 day'\n"
                    + "  and transaction_type in ('Stake', 'Return')\n"
                    + "  and name not in ('Jack1', 'texasnew1')\n"
                    + "group by 1, 2, 3, 4);";

    @Autowired
    public EngagementByPlatform(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate template,
                                final AggregatorLastUpdateDAO aggregatorLastUpdateDAO,
                                final AggregatorLockDao aggregatorLockDao,
                                final YazinoConfiguration configuration) {

        super(template, aggregatorLastUpdateDAO, aggregatorLockDao, ID, configuration, null);

    }

    @Scheduled(cron = "0 15 1 * * ?")//run at 6 30?
    public void update() {
        try {
            updateWithLocks(new Timestamp(new DateTime().getMillis()));
        } catch (Exception e) {
            LOG.error("failed to run update", e);
        }
    }

    @Transactional("externalDwTransactionManager")
    public Timestamp materializeData(final Timestamp timeLastRun, final Timestamp currentTimestamp) {

        DateTime fromDate = getEarliestDataDate(timeLastRun, SELECT_MIN_TRANSACTION_TS_FROM_TRANSACTION_LOG);
        getTemplate().update("DELETE FROM engagement_by_platform WHERE played_ts>= ?",
                new Timestamp(fromDate.withTimeAtStartOfDay().getMillis()));
        return executingQueryEveryDayForDateRange(currentTimestamp, fromDate, FILL_ENGAGEMENT_BY_PLATFORM);
    }


    protected PreparedStatementSetter getPreparedStatementSetter(final DateTime... runDay) {
        return new PreparedStatementSetter() {
            @Override
            public void setValues(final PreparedStatement ps) throws SQLException {

                final Timestamp from = new Timestamp(runDay[0].withTimeAtStartOfDay().getMillis());
                ps.setTimestamp(1, from);
                ps.setTimestamp(2, from);
                ps.setTimestamp(3, from);
            }
        };
    }

}
