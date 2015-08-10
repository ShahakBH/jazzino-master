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
public class AdNetworkAggregator extends Aggregator {

    private static final Logger LOG = LoggerFactory.getLogger(AdNetworkAggregator.class);


    private static final String UPDATE_FBOG_ADNET = "update dmr_player_activity_and_purchases "
            + "set registration_adnet = 'Viral' "
            + "where referrer like 'fb_og_%' and activity_date > now() - interval '1 month'; ";

    private static final String UPDATE_EMAIL_ADNET = "update dmr_player_activity_and_purchases "
            + "set registration_adnet = 'Organic' "
            + "where referrer like 'Email_%' and activity_date > now() - interval '1 month'; ";

    private static final String UPDATE_FB_WALL_ADNET = "update dmr_player_activity_and_purchases "
            + "set registration_adnet = 'Organic' "
            + "where referrer like 'FBWall_%' and activity_date > now() - interval '1 month'; ";

    private static final String UPDATE_FROM_REFERRER_ADNET = "update dmr_player_activity_and_purchases "
            + "set registration_adnet = substring(referrer,0,32) "
            + "where registration_adnet is null "
            + "and referrer is not null and activity_date > now() - interval '1 month'; ";

    private static final String UPDATE_NULL_ADNET = "update dmr_player_activity_and_purchases "
            + "set registration_adnet = 'Organic - was null' "
            + "where referrer is null and activity_date > now() - interval '1 month'; ";

    private static final String DELETE_TMP_DMR_DAYS = "truncate adnet_dmr_days_tmp; ";

    private static final String FILL_TMP_DMR_DAYS = "insert into adnet_dmr_days_tmp "
            + "select "
            + "distinct registration_date "
            + "from dmr_registrations; ";

    private static final String FILL_TMP_ADNET_REGS = "insert into adnet_registrations_tmp_data "
            + "select "
            + "reg_ts::date registration_date, "
            + "coalesce(registration_adnet, substring(registration_referrer,0,32), 'Organic - was null') registration_adnet, "
            + "count(1) num_registrations "
            + "from lobby_user "
            + "left join adnet_mappings nd "
            + "on registration_referrer = nd.referrer "
            + "group by 1, 2; ";

    private static final String FILL_ADNET_REGS = "insert into adnet_registrations "
            + "select  "
            + "r1.registration_date, "
            + "r2.registration_adnet, "
            + "r1.registration_date - r2.registration_date days_ago, "
            + "r2.num_registrations "
            + "from adnet_dmr_days_tmp r1, adnet_registrations_tmp_data r2 "
            + "where r1.registration_date >= r2.registration_date;";


    private static final String DELETE_ADNET_REG_TMP_DATA = "truncate adnet_registrations_tmp_data;";
    private static final String DELETE_ADNET_REGS = "truncate adnet_registrations;";

    static final String ID = "ad_network_aggregator";
    private final YazinoConfiguration configuration;


    @Autowired
    public AdNetworkAggregator(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate template,
                               final AggregatorLastUpdateDAO aggregatorLastUpdateDAO,
                               final AggregatorLockDao aggregatorLockDAO,
                               final YazinoConfiguration configuration) {
        super(template, aggregatorLastUpdateDAO, aggregatorLockDAO, ID, configuration, DmrAggregator.ID);
        this.configuration = configuration;
    }

    //CGLIB
    AdNetworkAggregator() {
        configuration = null;
    }

    @Scheduled(cron = "0 10 4 * * ?")
    public void update() {
        try {
            updateWithLocks(new Timestamp(new DateTime().getMillis()));
        } catch (Exception e) {
            LOG.error("failed to run update", e);
        }
    }

    @Transactional("externalDwTransactionManager")
    public Timestamp materializeData(final Timestamp timeLastRun, final Timestamp toDate) {
        String[] queries = getQueries();
        //just run once.
        return executingQueryEveryDayForDateRange(new Timestamp(new DateTime(toDate).withTimeAtStartOfDay().getMillis()),
                new DateTime(toDate).withTimeAtStartOfDay().minusDays(1).toDateTime(),
                false,
                queries);
    }

    String[] getQueries() {
        return new String[]{DELETE_ADNET_REG_TMP_DATA,
                DELETE_ADNET_REGS,
                DELETE_TMP_DMR_DAYS,
                configuration.getString("strata.aggregators.adnet.mapping"),
                UPDATE_FBOG_ADNET,
                UPDATE_EMAIL_ADNET,
                UPDATE_FB_WALL_ADNET,
                UPDATE_FROM_REFERRER_ADNET,
                UPDATE_NULL_ADNET,
                FILL_TMP_DMR_DAYS,
                FILL_TMP_ADNET_REGS,
                FILL_ADNET_REGS
        };
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
