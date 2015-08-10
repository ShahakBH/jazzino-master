package com.yazino.bi.aggregator;

import com.yazino.configuration.YazinoConfiguration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.joda.time.DateTime.now;

@Service
public class DayZeroAggregator extends Aggregator {


    private static final Logger LOG = LoggerFactory.getLogger(DayZeroAggregator.class);


    private static final String DELETE_DAY_ZERO_DATA = "delete from day_zero "
            + "where reg_ts >= :regStart and reg_ts < :regEnd; ";

    static final String ID = "day_zero";

    private static final String FILL_DAY_ZERO = "insert into day_zero("
            + "select"
            + "  lu.player_id,"
            + "  lu.reg_ts,"
            + "  COALESCE(pl.level,1), "
            + "  lu.account_id,"
            + "  count(session_key) sessions,"
            + "  0.00 ," // payout
            + "  0," // bonus_collection
            + "  acc.balance,"
            + "  lu.registration_platform,"
            + "  lu.registration_game_type,"
            + "  0, " // bet
            + "  :day_offset, "
            + "  0" // purchase
            + "  From lobby_user lu "
            + "   left join (select max(level) as level, player_id from player_level group by player_id)  pl on pl.player_id=lu.player_id"
            + "   join account_session acs on acs.account_id = lu.account_id"
            + "   join account acc on acc.account_id = lu.account_id"
            + "  where reg_ts>= :regStart::timestamp and reg_ts< :regEnd::timestamp"
            + "  and acs.start_ts >= :regStart::timestamp and acs.start_ts< :toNow::timestamp"
            + "   group by 1,2,3,4,8,9,10 order by lu.player_id);";


    public static final String UPDATE_BETS = "update day_zero dz set stakes= a.bets from "
            + "(select dz.player_id, count(tl.transaction_type)  bets "
            + " from day_zero dz "
            + " join transaction_log tl on tl.account_id = dz.account_id "
            + " where transaction_type='Stake' and transaction_ts>= :regStart and transaction_ts< :toNow"
            + " group by dz.player_id) "
            + "a where a.player_id = dz.player_id;";

    public static final String UPDATE_BONII = "update day_zero dz set bonus_collections= a.bonii from "
            + "(select lu.player_id, count(tl.transaction_type) bonii "
            + " from day_zero lu "
            + " join transaction_log tl on tl.account_id = lu.account_id "
            + " where transaction_type='LockoutBonus' and transaction_ts>= :regStart and transaction_ts< :toNow and lu.bonus_collections =0"
            + " group by lu.player_id) "
            + "a where a.player_id = dz.player_id;";

    public static final String UPDATE_PURCHASES = "update day_zero dz set purchases= a.purchases from "
            + "(select dz.player_id, count(*) purchases "
            + " from day_zero dz "
            + " join external_transaction p on p.player_id = dz.player_id "
            + " where message_ts>= :regStart and message_ts< :toNow and dz.purchases=0"
            + " group by dz.player_id) "
            + "a where a.player_id = dz.player_id;";

    public static final String UPDATE_PAYOUT = "update day_zero dz set payout= a.payout from ("
            + "select stakes.account_id account_id, -1*stakes.total,returns.total, returns.total/(-1*stakes.total) payout"
            + " from"
            + "   (select dz.account_id, sum(amount) total from transaction_log tl join day_zero dz on dz.account_id=tl.account_id"
            + "       where transaction_type='Stake' and dz.payout=0 and dz.reg_ts>= :regStart group by dz.account_id) stakes,"
            + " (select dz.account_id, sum(amount) total from transaction_log tl join day_zero dz on dz.account_id=tl.account_id"
            + "       where transaction_type='Return' and dz.payout=0 and dz.reg_ts>= :regStart group by dz.account_id) returns"
            + " where stakes.account_id=returns.account_id)"
            + " a where a.account_id=dz.account_id;";

    private Map<String, Object> params = newHashMap();

    @Autowired
    public DayZeroAggregator(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate template,
                             final AggregatorLastUpdateDAO aggregatorLastUpdateDAO,
                             final AggregatorLockDao aggregatorLockDAO,
                             final YazinoConfiguration configuration) {
        super(template, aggregatorLastUpdateDAO, aggregatorLockDAO, ID, configuration, PlayerActivityDaily.ID);
    }

    //CGLIB
    DayZeroAggregator() {
    }

    //every hour
    @Scheduled(cron = "0 1 * * * ?")
    public void update() {
        try {
            updateWithLocks(new Timestamp(now().getMillis()));
        } catch (Exception e) {
            LOG.error("failed to run update", e);
        }
    }

    @Transactional("externalDwTransactionManager")
    public Timestamp materializeData(final Timestamp timeLastRun, final Timestamp toDate) {
        //ignore last run time
        DateTime regStart = new DateTime(toDate).withMinuteOfHour(0).withSecondOfMinute(0).minusHours(24);
        DateTime regEnd = regStart.plusHours(1);
        DateTime now = new DateTime(toDate);

        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(getTemplate().getDataSource());
        LOG.info(
                "clearing data and aggregating registrations from {} to {} and their data to {}",
                regStart, regEnd, regStart, regEnd, now);
        params.put("regStart", toTs(regStart));
        params.put("regEnd", toTs(regEnd));
        params.put("toNow", toTs(now));
        params.put("day_offset", 0);

        for (String query : new String[]{
                DELETE_DAY_ZERO_DATA,
                FILL_DAY_ZERO,
                UPDATE_BETS,
                UPDATE_BONII,
                UPDATE_PURCHASES,
                UPDATE_PAYOUT
        }) {
            LOG.debug(query);
            template.update(query, params);
        }
        return toDate;
    }

    private Timestamp toTs(final DateTime dateTime) {
        return new Timestamp(dateTime.getMillis());
    }


    protected PreparedStatementSetter getPreparedStatementSetter(final DateTime... runDay) {
        return null; // not used in this as it doesn't run every day but every hour
    }

}
