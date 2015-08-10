package com.yazino.bi.aggregator;

import com.yazino.configuration.YazinoConfiguration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
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
public class ManagementReportAggregator extends Aggregator {

    private static final Logger LOG = LoggerFactory.getLogger(ManagementReportAggregator.class);
    static final String ID = "management_report";
    private static final String SELECT_MIN_AUDIT_TS_AS_AUDIT_TS_FROM_AUDIT_COMMAND = "SELECT min(reg_ts) from lobby_user";

    //CGLIB
    ManagementReportAggregator() {
    }

    //    public static final String CREATE_DATE_ENTRY = "INSERT INTO management_report (activity_date) values (?)";
    private static final String INSERT_REGISTRATIONS = "insert into management_report (activity_date, registrations, players, revenue, purchases)  "
            + "select ?, regs.regs, players.players, revenues.revenue, purchases.purchases from "
            + "(select count(distinct lu.player_id) regs from lobby_user lu "
            + "   where lu.reg_ts>=? and lu.reg_ts< ?)regs, "
            + "(select count(distinct player_id) players from player_activity_hourly "
            + "   where activity_ts>=? and activity_ts<?)players, "
            + "(select sum(amount_gbp) revenue from external_transaction_mv where purchase_ts>=? and purchase_ts<?) revenues, "
            + "(select count(*) purchases from purchase_view where purchase_ts>=? and purchase_ts<?) purchases";


    private static final String DELETE_DATA_FOR_DATE = "DELETE FROM management_report where activity_date >= ? ";

    @Autowired
    public ManagementReportAggregator(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate template,
                                      final AggregatorLastUpdateDAO aggregatorLastUpdateDAO,
                                      final AggregatorLockDao aggregatorLockDao,
                                      final YazinoConfiguration configuration) {

        super(template, aggregatorLastUpdateDAO, aggregatorLockDao, ID, configuration, null);

    }

    // after PAD 2am (for BST) every night
    @Scheduled(cron = "${strata.aggregators.management-report.timing}")
    public void update() {
        try {
            updateWithLocks(new Timestamp(DateTimeUtils.currentTimeMillis()));
        } catch (Exception e) {
            LOG.error("failed to run update", e);
        }
    }

    @Transactional("externalDwTransactionManager")
    public Timestamp materializeData(final Timestamp timeLastRun, final Timestamp toDate) {
        final DateTime fromDate = getEarliestDataDate(timeLastRun,
                SELECT_MIN_AUDIT_TS_AS_AUDIT_TS_FROM_AUDIT_COMMAND).withTimeAtStartOfDay().minusDays(1).toDateTime();

        getTemplate().update(DELETE_DATA_FOR_DATE, new PreparedStatementSetter() {
            @Override
            public void setValues(final PreparedStatement ps) throws SQLException {
                ps.setTimestamp(1, new Timestamp(fromDate.getMillis()));
            }
        });

        return executingQueryEveryDayForDateRange(toDate, fromDate, INSERT_REGISTRATIONS);
    }


    protected PreparedStatementSetter getPreparedStatementSetter(final DateTime... runDay) {
        return new PreparedStatementSetter() {
            @Override
            public void setValues(final PreparedStatement ps) throws SQLException {
                ps.setTimestamp(1, new Timestamp(runDay[0].withTimeAtStartOfDay().getMillis()));
                ps.setTimestamp(2, new Timestamp(runDay[0].withTimeAtStartOfDay().getMillis()));
                ps.setTimestamp(3, new Timestamp(runDay[1].withTimeAtStartOfDay().getMillis()));
                ps.setTimestamp(4, new Timestamp(runDay[0].withTimeAtStartOfDay().getMillis()));
                ps.setTimestamp(5, new Timestamp(runDay[1].withTimeAtStartOfDay().getMillis()));
                ps.setTimestamp(6, new Timestamp(runDay[0].withTimeAtStartOfDay().getMillis()));
                ps.setTimestamp(7, new Timestamp(runDay[1].withTimeAtStartOfDay().getMillis()));
                ps.setTimestamp(8, new Timestamp(runDay[0].withTimeAtStartOfDay().getMillis()));
                ps.setTimestamp(9, new Timestamp(runDay[1].withTimeAtStartOfDay().getMillis()));

            }
        };
    }

}
