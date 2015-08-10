package com.yazino.bi.aggregator;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.Platform;
import com.yazino.platform.audit.message.CommandAudit;
import com.yazino.platform.event.message.TableEvent;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import strata.server.worker.audit.persistence.PostgresCommandAuditDAO;
import strata.server.worker.audit.persistence.PostgresSessionKeyDAO;
import strata.server.worker.event.persistence.PostgresPlayerDWDAO;
import strata.server.worker.event.persistence.PostgresPlayerProfileDWDAO;
import strata.server.worker.event.persistence.PostgresPlayerReferrerDWDAO;
import strata.server.worker.event.persistence.PostgresTableDWDAO;
import utils.PostgresTestValueHelper;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.Boolean.TRUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static utils.PostgresTestValueHelper.createPlayerProfileAndRef;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
@DirtiesContext
public class PlayerActivityDailyIntegrationTest {

    public static final BigDecimal PLAYER_ID = BigDecimal.valueOf(1.00);
    public static final DateTime NOW = new DateTime(2013, 1, 02, 10, 30);
    public static final BigDecimal ACCOUNT_ID = BigDecimal.TEN;
    public static final String SELECT_FROM_PLAYER_ACTIVITY_DAILY_WHERE_PLAYER_ID =
            "SELECT * FROM player_activity_daily WHERE player_id = ?;";
    public static final String AGGREGATOR_ID = "player_activity_daily";
    public static final String INVITE_REFFERER_ID = "referrer";
    public static final DateTime ACCOUNT_CREATION_TIME = new DateTime(2013, 1, 02, 9, 30);
    public static final DateTime GAME_PLAY_TIME = new DateTime(2013, 1, 02, 9, 33);
    public static final DateTime PLAYER_ACTIVITY_LAST_RUN = NOW.minusDays(1).minusHours(1);
    public static final String SLOTS = "SLOTS";
    public static final String POKER = "POKER";

    private long sessionIdSource = -1;

    @Autowired
    @Qualifier("playerActivityDaily")
    private Aggregator underTest;

    @Autowired
    private AggregatorLastUpdateDAO aggregatorLastUpdateDAO;

    @Autowired
    private PostgresCommandAuditDAO postgresCommandAuditDAO;

    @Autowired
    private PostgresPlayerProfileDWDAO postgresPlayerProfileDWDAO;

    @Autowired
    private PostgresSessionKeyDAO postgresSessionKeyDAO;

    @Autowired
    private PostgresTableDWDAO postgresTableDWDAO;

    @Autowired
    private PostgresPlayerDWDAO postgresPlayerDWDAO;

    @Autowired
    private PostgresPlayerReferrerDWDAO postgresPlayerReferrerDWDAO;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private YazinoConfiguration configuration;


    @Before
    public void setup() {
        configuration.setProperty("data-warehouse.write.enabled", TRUE);
        configuration.setProperty("data-warehouse.aggregators.enabled",TRUE);
        jdbcTemplate.update("delete from aggregator_lock");
        jdbcTemplate.update("delete from player_activity_daily");
        jdbcTemplate.update("delete from player_activity_hourly");
        jdbcTemplate.update("delete from audit_command");
        jdbcTemplate.update("delete from account_session");
        jdbcTemplate.update("delete from table_definition");
        jdbcTemplate.update("delete from game_variation_template");
        jdbcTemplate.update("delete from lobby_user");
        jdbcTemplate.update("delete from aggregator_last_update");

        ThreadLocalDateTimeUtils.setCurrentMillisFixed(NOW.getMillis());
    }

    @After
    public void tearDown() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void updateShouldInsertOneRecordIfPlayerHasPlayedInADayOnPlatform() {
        DateTime accountCreateTime = ACCOUNT_CREATION_TIME;
        DateTime playerActivityLastRunTime = NOW.minusDays(1);
        DateTime gamePlayTime = NOW.minusDays(1);

        createPlayerAccount(accountCreateTime, PLAYER_ID, ACCOUNT_ID);
        createPlayerAccount(accountCreateTime, PLAYER_ID.add(BigDecimal.ONE), ACCOUNT_ID.add(BigDecimal.ONE));

        aggregatorLastUpdateDAO.updateLastRunFor(AGGREGATOR_ID, new Timestamp(playerActivityLastRunTime.getMillis()));

        DateTime expectedActivityDailyTimestamp = gamePlayTime.withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);

        insertIntoPlayeyActivityHourly(PLAYER_ID, SLOTS, "WEB", new Timestamp(gamePlayTime.getMillis()));
        insertIntoPlayeyActivityHourly(PLAYER_ID.add(BigDecimal.ONE), POKER, "ANDROID", new Timestamp(gamePlayTime.getMillis()));

        underTest.update();

        assertThat(getPlayerActivityDailyRecord(PLAYER_ID), equalTo(new PlayerActivityDailyRecord(PLAYER_ID, "SLOTS", "WEB", expectedActivityDailyTimestamp, INVITE_REFFERER_ID, accountCreateTime)));
        assertThat(getPlayerActivityDailyRecord(PLAYER_ID.add(BigDecimal.ONE)), equalTo(new PlayerActivityDailyRecord(PLAYER_ID.add(BigDecimal.ONE), POKER, "ANDROID", expectedActivityDailyTimestamp, INVITE_REFFERER_ID, accountCreateTime)));
    }

    @Test
    public void updateShouldNotInsertRecordforTodaysData() {
        aggregatorLastUpdateDAO.updateLastRunFor(AGGREGATOR_ID, new Timestamp(PLAYER_ACTIVITY_LAST_RUN.getMillis()));
        createPlayerAccount(ACCOUNT_CREATION_TIME, PLAYER_ID, ACCOUNT_ID);

        insertIntoPlayeyActivityHourly(PLAYER_ID, SLOTS, "WEB", new Timestamp(NOW.getMillis()));
        insertIntoPlayeyActivityHourly(PLAYER_ID, SLOTS, "WEB", new Timestamp(NOW.minusDays(1).getMillis()));
        underTest.update();

        assertThat(jdbcTemplate.queryForInt("select count(*) from player_activity_daily where activity_ts='2012-12-31'::date"),is(0));
        assertThat(jdbcTemplate.queryForInt("select count(*) from player_activity_daily where activity_ts='2013-1-01'::date"),is(1));
        assertThat(jdbcTemplate.queryForInt("select count(*) from player_activity_daily where activity_ts='2013-1-02'::date"),is(0));


    }

    @Test
    public void updateShouldBeAbleToBeRunTwice() {
        aggregatorLastUpdateDAO.updateLastRunFor(AGGREGATOR_ID, new Timestamp(PLAYER_ACTIVITY_LAST_RUN.getMillis()));
        createPlayerAccount(ACCOUNT_CREATION_TIME, PLAYER_ID, ACCOUNT_ID);

        insertIntoPlayeyActivityHourly(PLAYER_ID, SLOTS, "WEB", new Timestamp(NOW.plusDays(1).getMillis()));
        insertIntoPlayeyActivityHourly(PLAYER_ID, SLOTS, "WEB", new Timestamp(NOW.getMillis()));
        insertIntoPlayeyActivityHourly(PLAYER_ID, SLOTS, "WEB", new Timestamp(NOW.minusDays(1).getMillis()));

        underTest.update();
        aggregatorLastUpdateDAO.updateLastRunFor(AGGREGATOR_ID, new Timestamp(PLAYER_ACTIVITY_LAST_RUN.getMillis()));
        underTest.update();


        assertThat(jdbcTemplate.queryForInt("select count(*) from player_activity_daily where activity_ts='2012-12-31'::date"),is(0));
        assertThat(jdbcTemplate.queryForInt("select count(*) from player_activity_daily where activity_ts='2013-1-01'::date"),is(1));
        assertThat(jdbcTemplate.queryForInt("select count(*) from player_activity_daily where activity_ts='2013-1-02'::date"),is(0));


    }

    @Test
    public void updateShouldInsertOneRecordIfPlayerHasPlayedMoreThanOnceInADay() {
        aggregatorLastUpdateDAO.updateLastRunFor(AGGREGATOR_ID, new Timestamp(PLAYER_ACTIVITY_LAST_RUN.getMillis()));
        createPlayerAccount(ACCOUNT_CREATION_TIME, PLAYER_ID, ACCOUNT_ID);

        insertIntoPlayeyActivityHourly(PLAYER_ID, SLOTS, "WEB", new Timestamp(NOW.minusDays(1).getMillis()));
        insertIntoPlayeyActivityHourly(PLAYER_ID, SLOTS, "WEB", new Timestamp(NOW.minusDays(1).minusHours(1).getMillis()));
        underTest.update();

        PlayerActivityDailyRecord actual = getPlayerActivityDailyRecord(PLAYER_ID);
        assertThat(actual, equalTo(new PlayerActivityDailyRecord(PLAYER_ID, "SLOTS", "WEB", NOW.minusDays(1).withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0), INVITE_REFFERER_ID, ACCOUNT_CREATION_TIME)));
    }

    @Test
    public void updateShouldInsertAllRecordsIfPlayerHasPlayedOnMoreThanOnePlatform() {
        aggregatorLastUpdateDAO.updateLastRunFor(AGGREGATOR_ID, new Timestamp(PLAYER_ACTIVITY_LAST_RUN.getMillis()));
        createPlayerAccount(ACCOUNT_CREATION_TIME, PLAYER_ID, ACCOUNT_ID);

        insertIntoPlayeyActivityHourly(PLAYER_ID, SLOTS, "WEB", new Timestamp(NOW.minusDays(1).getMillis()));
        insertIntoPlayeyActivityHourly(PLAYER_ID, SLOTS, "IOS", new Timestamp(NOW.minusDays(1).minusHours(1).getMillis()));

        underTest.update();

        List<PlayerActivityDailyRecord> playerActivityList = getPlayerActivityList(PLAYER_ID);
        assertThat(playerActivityList.size(), is(2));
        assertThat(playerActivityList, containsInAnyOrder(
                new PlayerActivityDailyRecord(PLAYER_ID, "SLOTS", "WEB", NOW.minusDays(1).withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0), INVITE_REFFERER_ID, ACCOUNT_CREATION_TIME),
                new PlayerActivityDailyRecord(PLAYER_ID, "SLOTS", "IOS", NOW.minusDays(1).withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0), INVITE_REFFERER_ID, ACCOUNT_CREATION_TIME)
        ));
    }

    @Test
    public void updateShouldInsertRecordsInIfPlayerHasPlayedAndTheRunTimeStampIsNull() {
        createPlayerAccount(ACCOUNT_CREATION_TIME.minusMonths(2), PLAYER_ID, ACCOUNT_ID);

        insertIntoPlayeyActivityHourly(PLAYER_ID, SLOTS, "WEB", new Timestamp(NOW.minusMonths(2).getMillis()));
        insertIntoPlayeyActivityHourly(PLAYER_ID, SLOTS, "WEB", new Timestamp(NOW.minusMonths(1).minusHours(1).getMillis()));

        underTest.update();

        List<PlayerActivityDailyRecord> playerActivityList = jdbcTemplate.query(SELECT_FROM_PLAYER_ACTIVITY_DAILY_WHERE_PLAYER_ID, playerActvityRowMapper(), PLAYER_ID);
                assertThat(playerActivityList.size(), is(2));
                assertThat(playerActivityList, containsInAnyOrder(
                        new PlayerActivityDailyRecord(PLAYER_ID, "SLOTS", "WEB", GAME_PLAY_TIME.minusMonths(1).withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0), INVITE_REFFERER_ID, ACCOUNT_CREATION_TIME.minusMonths(2)),
                        new PlayerActivityDailyRecord(PLAYER_ID, "SLOTS", "WEB", GAME_PLAY_TIME.minusMonths(2).withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0), INVITE_REFFERER_ID, ACCOUNT_CREATION_TIME.minusMonths(2)))
                );
    }

    @Test
    public void updateShouldARecordsIfAPlayerHasPlayedOnAMidnightBoundary() {
        createPlayerAccount(ACCOUNT_CREATION_TIME.minusDays(1), PLAYER_ID, ACCOUNT_ID);
        aggregatorLastUpdateDAO.updateLastRunFor(AGGREGATOR_ID, new Timestamp(NOW.minusDays(1).getMillis()));


        insertIntoPlayeyActivityHourly(PLAYER_ID, SLOTS, "WEB", new Timestamp(NOW.minusDays(2).withHourOfDay(23).withMinuteOfHour(59).getMillis()));
        insertIntoPlayeyActivityHourly(PLAYER_ID, SLOTS, "WEB", new Timestamp(NOW.minusDays(1).withHourOfDay(0).withMinuteOfHour(0).getMillis()));


        underTest.update();

        List<PlayerActivityDailyRecord> playerActivityList = getPlayerActivityList(PLAYER_ID);
        assertThat(playerActivityList.size(), is(1));
        assertThat(getPlayerActivityDailyRecord(PLAYER_ID), equalTo(
                new PlayerActivityDailyRecord(PLAYER_ID, "SLOTS", "WEB",
                        NOW.minusDays(1).toDateMidnight().toDateTime(),
                        INVITE_REFFERER_ID, ACCOUNT_CREATION_TIME.minusDays(1))
        ));
    }

    @Test
    public void updateShouldARecordsIfAPlayerHasBeforeAMidnightBoundary() {
        createPlayerAccount(ACCOUNT_CREATION_TIME.minusDays(1), PLAYER_ID, ACCOUNT_ID);
        aggregatorLastUpdateDAO.updateLastRunFor(AGGREGATOR_ID, new Timestamp(NOW.minusDays(1).getMillis()));


        insertIntoPlayeyActivityHourly(PLAYER_ID, SLOTS, "WEB", new Timestamp(NOW.withHourOfDay(0).withMinuteOfHour(0).getMillis()));
        insertIntoPlayeyActivityHourly(PLAYER_ID, SLOTS, "WEB", new Timestamp(NOW.minusDays(1).withHourOfDay(23).withMinuteOfHour(59).getMillis()));


        underTest.update();

        List<PlayerActivityDailyRecord> playerActivityList = getPlayerActivityList(PLAYER_ID);
        assertThat(playerActivityList.size(), is(1));
        assertThat(getPlayerActivityDailyRecord(PLAYER_ID), equalTo(
                new PlayerActivityDailyRecord(PLAYER_ID, "SLOTS", "WEB",
                        NOW.minusDays(1)
                                .withHourOfDay(0)
                                .withMinuteOfHour(0)
                                .withSecondOfMinute(0)
                                .withMillisOfSecond(0),
                        INVITE_REFFERER_ID, ACCOUNT_CREATION_TIME.minusDays(1))
        ));
    }

    private void createPlayerAccount(DateTime createTime, BigDecimal playerId, BigDecimal accountId) {
        createPlayerProfileAndRef(postgresPlayerProfileDWDAO,
                postgresPlayerDWDAO,
                postgresPlayerReferrerDWDAO,
                playerId,
                createTime,
                accountId);
    }

    private RowMapper<PlayerActivityDailyRecord> playerActvityRowMapper() {
        return new RowMapper<PlayerActivityDailyRecord>() {
            @Override
            public PlayerActivityDailyRecord mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                return new PlayerActivityDailyRecord(rs.getBigDecimal("player_id"),
                        rs.getString("game"),
                        rs.getString("platform"),
                        new DateTime(rs.getTimestamp("activity_ts")),
                        rs.getString("referrer"),
                        new DateTime(rs.getTimestamp("reg_ts"))

                );
            }
        };
    }


    private List<PlayerActivityDailyRecord> getPlayerActivityList(BigDecimal playerId) {
        return jdbcTemplate.query(SELECT_FROM_PLAYER_ACTIVITY_DAILY_WHERE_PLAYER_ID, playerActvityRowMapper(), playerId);
    }


    private PlayerActivityDailyRecord getPlayerActivityDailyRecord(BigDecimal playerId) {
        return jdbcTemplate.queryForObject(SELECT_FROM_PLAYER_ACTIVITY_DAILY_WHERE_PLAYER_ID, playerActvityRowMapper(), playerId);
    }

    private void playGame(final BigDecimal playerId, final DateTime auditTs, final BigDecimal tableId, final Long gameId) {
        postgresCommandAuditDAO.saveAll(newArrayList(new CommandAudit("auditLabel",
                "hostname",
                auditTs.toDate(),
                tableId,
                gameId,
                "type",
                null,
                playerId,
                "uuid")));
    }

    private void createTable(final String game, final BigDecimal tableId, BigDecimal gameVariationTemplateId) {
        postgresTableDWDAO.saveAll(newArrayList(new TableEvent(tableId, game, gameVariationTemplateId, "templateName")));
    }

    private void createSessionForPlayer(final BigDecimal playerId, final DateTime auditTs, final Platform platform, BigDecimal accountId) {
        PostgresTestValueHelper.createSession(postgresSessionKeyDAO, jdbcTemplate, playerId, auditTs, platform, accountId, BigDecimal.valueOf(sessionIdSource--));
    }

    private void insertIntoPlayeyActivityHourly(final BigDecimal playerId, final String game, final String platform, final Timestamp activity_ts) {
        jdbcTemplate.update("INSERT INTO player_activity_hourly (player_id, game, platform, activity_ts) VALUES (?,?,?,?)"
                , new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps) throws SQLException {
                ps.setBigDecimal(1, playerId);
                ps.setString(2, game);
                ps.setString(3, platform);
                ps.setTimestamp(4, activity_ts);
            }
        });
    }

}


