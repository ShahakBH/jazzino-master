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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.Boolean.TRUE;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.valueOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static utils.PostgresTestValueHelper.createPlayerProfileAndRef;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
@DirtiesContext
public class PlayerActivityHourlyIntegrationTest { //having this transactional breaks the tests.

    public static final BigDecimal PLAYER_ID = valueOf(1.00);
    public static final BigDecimal PLAYER_2_ID = valueOf(2.00);
    public static final BigDecimal PLAYER_3_ID = valueOf(3.00);
    public static final DateTime NOW_10_30 = new DateTime(2013, 1, 02, 10, 30);
    public static final BigDecimal ACCOUNT_ID = BigDecimal.TEN;
    public static final BigDecimal ACCOUNT_2_ID = BigDecimal.TEN.add(ONE);
    public static final BigDecimal ACCOUNT_3_ID = BigDecimal.TEN.add(valueOf(2));
    public static final String SELECT_FROM_PLAYER_ACTIVITY_HOURLY_WHERE_PLAYER_ID =
            "SELECT * FROM player_activity_hourly where player_id = ?;";

    public static final String AGGREGATOR_ID = "player_activity_hourly";
    public static final String SLOTS = "SLOTS";
    public static final String POKER = "POKER";
    public static final DateTime ACCOUNT_CREATION_TIME_9_30 = new DateTime(2013, 1, 02, 9, 30);
    public static final DateTime SESSION_START_TIME_9_32 = new DateTime(2013, 1, 02, 9, 32);
    public static final DateTime GAME_PLAY_TIME_9_33 = new DateTime(2013, 1, 02, 9, 33);
    public static final DateTime PLAYER_ACTIVITY_LAST_RUN_9_29 = new DateTime(2013, 1, 02, 9, 29);
    public static final String BLACKJACK = "BLACKJACK";
    private static final Logger LOG = LoggerFactory.getLogger(PlayerActivityHourlyIntegrationTest.class);

    private long sessionIdSource = -1;

    @Autowired
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
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private YazinoConfiguration configuration;

    @Autowired
    private PostgresPlayerReferrerDWDAO postgresPlayerReferrerDWDAO;

    @Autowired
    private AggregatorLockDao lockDao;

    @Before
    public void setup() {
        configuration.setProperty("data-warehouse.write.enabled", TRUE);
        configuration.setProperty("data-warehouse.aggregators.enabled",TRUE);

        jdbcTemplate.update("delete from aggregator_lock");
        jdbcTemplate.update("delete from player_activity_hourly");
        jdbcTemplate.update("delete from stg_player_activity_hourly");
        jdbcTemplate.update("delete from audit_command");
        jdbcTemplate.update("delete from account_session");
        jdbcTemplate.update("delete from table_definition");
        jdbcTemplate.update("delete from game_variation_template");
        jdbcTemplate.update("delete from lobby_user");
        jdbcTemplate.update("delete from aggregator_last_update");


        ThreadLocalDateTimeUtils.setCurrentMillisFixed(NOW_10_30.getMillis());
    }

    @After
    public void tearDown() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void updateShouldInsertARecordIfPlayerHasPlayedInAHour() {

        createPlayerAccount(ACCOUNT_CREATION_TIME_9_30, PLAYER_ID, ACCOUNT_ID);
        createPlayerAccount(ACCOUNT_CREATION_TIME_9_30, PLAYER_2_ID, ACCOUNT_2_ID);
        aggregatorLastUpdateDAO.updateLastRunFor(AGGREGATOR_ID, new Timestamp(PLAYER_ACTIVITY_LAST_RUN_9_29.getMillis()));

        createPlayerSessionAndPlayGame(PLAYER_ID, SLOTS, Platform.WEB, true, true,
                SESSION_START_TIME_9_32, GAME_PLAY_TIME_9_33, true, ACCOUNT_ID);
        createPlayerSessionAndPlayGame(PLAYER_2_ID, POKER, Platform.IOS, true, true, SESSION_START_TIME_9_32,
                GAME_PLAY_TIME_9_33, true, ACCOUNT_2_ID);
        underTest.update();

        DateTime expectedActivityDailyTimestamp = GAME_PLAY_TIME_9_33.withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);

        assertThat(getPlayerActivityRecord(PLAYER_ID), equalTo(new PlayerActivity(PLAYER_ID, SLOTS, "WEB", expectedActivityDailyTimestamp)));
        assertThat(getPlayerActivityRecord(PLAYER_2_ID),
                equalTo(new PlayerActivity(PLAYER_2_ID, POKER, "IOS", expectedActivityDailyTimestamp)));
    }

    @Test
    public void updateShouldHandleLateEntriesDueToWorkerThreads() {
        createPlayerAccount(ACCOUNT_CREATION_TIME_9_30, PLAYER_ID, ACCOUNT_ID);
        createPlayerAccount(ACCOUNT_CREATION_TIME_9_30, PLAYER_2_ID, ACCOUNT_2_ID);
        LOG.warn("first player plays at "+ GAME_PLAY_TIME_9_33);
        createPlayerSessionAndPlayGame(PLAYER_ID, SLOTS, Platform.WEB, true, true, SESSION_START_TIME_9_32, GAME_PLAY_TIME_9_33, true, ACCOUNT_ID);

        LOG.warn("second player plays at " + GAME_PLAY_TIME_9_33.minusMinutes(50));
        createPlayerSessionAndPlayGame(PLAYER_2_ID, POKER, Platform.IOS, true, true, SESSION_START_TIME_9_32.minusMinutes(50), GAME_PLAY_TIME_9_33.minusMinutes(50), true, ACCOUNT_2_ID);

        LOG.warn("agg runs with time "+ GAME_PLAY_TIME_9_33.minusMinutes(10));
        aggregatorLastUpdateDAO.updateLastRunFor(AGGREGATOR_ID, new Timestamp(GAME_PLAY_TIME_9_33.minusMinutes(10).getMillis()));

        underTest.update();
        DateTime expectedActivityDailyTimestamp = GAME_PLAY_TIME_9_33.withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);
        DateTime previousActivityDailyTimestamp = expectedActivityDailyTimestamp.minusHours(1);

        assertThat(getPlayerActivityRecord(PLAYER_ID), equalTo(new PlayerActivity(PLAYER_ID, SLOTS, "WEB", expectedActivityDailyTimestamp)));
        assertThat(getPlayerActivityRecord(PLAYER_2_ID),
                equalTo(new PlayerActivity(PLAYER_2_ID, POKER, "IOS", previousActivityDailyTimestamp)));
    }


    private PlayerActivity getPlayerActivityRecord(BigDecimal playerId) {
        return jdbcTemplate.queryForObject(SELECT_FROM_PLAYER_ACTIVITY_HOURLY_WHERE_PLAYER_ID, playerActivityRowMapper(), playerId);
    }

    @Test
    public void updateShouldInsertOneRecordIfPlayerHasPlayedMoreThanOnceInAnHour() {
        aggregatorLastUpdateDAO.updateLastRunFor(AGGREGATOR_ID, new Timestamp(PLAYER_ACTIVITY_LAST_RUN_9_29.getMillis()));
        createPlayerAccount(ACCOUNT_CREATION_TIME_9_30, PLAYER_ID, ACCOUNT_ID);

        createPlayerSessionAndPlayGame(PLAYER_ID, SLOTS, Platform.WEB, true, true,
                SESSION_START_TIME_9_32, GAME_PLAY_TIME_9_33, true, ACCOUNT_ID);
        createPlayerSessionAndPlayGame(PLAYER_ID, SLOTS, Platform.WEB, true, true, SESSION_START_TIME_9_32.plusMinutes(4), GAME_PLAY_TIME_9_33.plusMinutes(4), false, ACCOUNT_ID);
        underTest.update();

        PlayerActivity actual = getPlayerActivityRecord(PLAYER_ID);
        assertThat(actual, equalTo(new PlayerActivity(PLAYER_ID, SLOTS, "WEB", GAME_PLAY_TIME_9_33.withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0))));
    }

    @Test
    public void updateShouldInsertAllRecordsIfPlayerHasPlayedOnMoreThanOnePlatform() {
        aggregatorLastUpdateDAO.updateLastRunFor(AGGREGATOR_ID, new Timestamp(PLAYER_ACTIVITY_LAST_RUN_9_29.getMillis()));
        createPlayerAccount(ACCOUNT_CREATION_TIME_9_30, PLAYER_ID, ACCOUNT_ID);

        createPlayerSessionAndPlayGame(PLAYER_ID, SLOTS, Platform.WEB, true, true,
                SESSION_START_TIME_9_32, GAME_PLAY_TIME_9_33, true, ACCOUNT_ID);
        createPlayerSessionAndPlayGame(PLAYER_ID,
                SLOTS,
                Platform.IOS,
                true,
                false,
                SESSION_START_TIME_9_32.plusMinutes(4),
                GAME_PLAY_TIME_9_33.plusMinutes(5),
                true,
                ACCOUNT_ID);
        underTest.update();


        List<PlayerActivity> playerActivityList = getPlayerActivityList(PLAYER_ID);
        assertThat(playerActivityList.size(), is(2));
        assertThat(playerActivityList, containsInAnyOrder(
                new PlayerActivity(PLAYER_ID, SLOTS, "WEB", GAME_PLAY_TIME_9_33.withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0)),
                new PlayerActivity(PLAYER_ID, SLOTS, "IOS", GAME_PLAY_TIME_9_33.withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0))
        ));
    }

    @Test
    public void updateShouldInsertAllRecordsInIfPlayerHasPlayedAndRunTimeStampIsNull() {
        createPlayerAccount(ACCOUNT_CREATION_TIME_9_30.minusMonths(2), PLAYER_ID, ACCOUNT_ID);

        createPlayerSessionAndPlayGame(PLAYER_ID,
                SLOTS,
                Platform.WEB,
                true,
                true,
                SESSION_START_TIME_9_32.minusMonths(2),
                GAME_PLAY_TIME_9_33.minusMonths(2),
                true,
                ACCOUNT_ID);
        createPlayerSessionAndPlayGame(PLAYER_ID, SLOTS, Platform.WEB, true, true, SESSION_START_TIME_9_32.minusMonths(1), GAME_PLAY_TIME_9_33.minusMonths(1), true, ACCOUNT_ID);
        underTest.update();

        final List<PlayerActivity> actualPlayerActivityList = getPlayerActivityList(PLAYER_ID);
        assertThat(actualPlayerActivityList, containsInAnyOrder(
                new PlayerActivity(PLAYER_ID,
                        SLOTS,
                        "WEB",
                        GAME_PLAY_TIME_9_33.minusMonths(2).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0)),
                new PlayerActivity(PLAYER_ID,
                        SLOTS,
                        "WEB",
                        GAME_PLAY_TIME_9_33.minusMonths(1).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0))));
    }

    @Test
    public void updateShouldInsertTwoRecordsIfAPlayerHasPlayedOverAMidnightBoundary() {
        createPlayerAccount(ACCOUNT_CREATION_TIME_9_30.minusDays(10), PLAYER_ID, ACCOUNT_ID);
        aggregatorLastUpdateDAO.updateLastRunFor(AGGREGATOR_ID, new Timestamp(PLAYER_ACTIVITY_LAST_RUN_9_29.minusDays(1).getMillis()));

        createPlayerSessionAndPlayGame(PLAYER_ID, SLOTS, Platform.WEB, true, true, SESSION_START_TIME_9_32.minusDays(1).withHourOfDay(23), GAME_PLAY_TIME_9_33.minusDays(1).withHourOfDay(23), true, ACCOUNT_ID);
        createPlayerSessionAndPlayGame(PLAYER_ID, POKER, Platform.WEB, true, true, null, GAME_PLAY_TIME_9_33.withHourOfDay(0).withMinuteOfHour(1), false, ACCOUNT_ID);
        createPlayerSessionAndPlayGame(PLAYER_ID, BLACKJACK, Platform.WEB, true, true, null, GAME_PLAY_TIME_9_33.withHourOfDay(2).withMinuteOfHour(30), false, ACCOUNT_ID);
        underTest.update();

        List<PlayerActivity> playerActivityList = getPlayerActivityList(PLAYER_ID);
        assertThat(playerActivityList.size(), is(3));
        assertThat(playerActivityList, containsInAnyOrder(
                new PlayerActivity(PLAYER_ID,
                        SLOTS,
                        "WEB",
                        GAME_PLAY_TIME_9_33.minusDays(1).withHourOfDay(23).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0)),
                new PlayerActivity(PLAYER_ID,
                        POKER,
                        "WEB",
                        GAME_PLAY_TIME_9_33.withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0)),
                new PlayerActivity(PLAYER_ID,
                        BLACKJACK,
                        "WEB",
                        GAME_PLAY_TIME_9_33.withHourOfDay(2).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0))
        ));
    }

    @Test
    public void updateShouldInsertRecordIntoStagingTable(){
        assertThat(jdbcTemplate.queryForInt("select count(*) from player_activity_hourly"), is(0));
        createPlayerAccount(ACCOUNT_CREATION_TIME_9_30.minusDays(10), PLAYER_ID, ACCOUNT_ID);
        createPlayerSessionAndPlayGame(PLAYER_ID,
                SLOTS,
                Platform.WEB,
                true,
                true,
                SESSION_START_TIME_9_32.minusDays(1).withHourOfDay(23),
                GAME_PLAY_TIME_9_33.minusDays(1).withHourOfDay(23),
                true,
                ACCOUNT_ID);
        aggregatorLastUpdateDAO.updateLastRunFor(AGGREGATOR_ID, new Timestamp(PLAYER_ACTIVITY_LAST_RUN_9_29.minusDays(1).getMillis()));
        underTest.update();
        assertThat(jdbcTemplate.queryForInt("select count(*) from player_activity_hourly"), is(1));

        createPlayerSessionAndPlayGame(PLAYER_ID,
                SLOTS,
                Platform.WEB,
                true,
                true,
                SESSION_START_TIME_9_32.minusDays(1).withHourOfDay(23),
                GAME_PLAY_TIME_9_33.minusDays(1).withHourOfDay(23).withMinuteOfHour(10),
                true,
                ACCOUNT_ID);
        aggregatorLastUpdateDAO.updateLastRunFor(AGGREGATOR_ID, new Timestamp(PLAYER_ACTIVITY_LAST_RUN_9_29.minusDays(1).getMillis()));

        underTest.update();
        assertThat(jdbcTemplate.queryForInt("select count(*) from player_activity_hourly"),is(1));
    }

    private void createPlayerAccount(DateTime createTime, BigDecimal playerId, BigDecimal accountId) {
        createPlayerProfileAndRef(postgresPlayerProfileDWDAO,
                postgresPlayerDWDAO,
                postgresPlayerReferrerDWDAO,
                playerId,
                createTime,
                accountId);
    }


    private RowMapper<PlayerActivity> playerActivityRowMapper() {
        return new RowMapper<PlayerActivity>() {
            @Override
            public PlayerActivity mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                return new PlayerActivity(rs.getBigDecimal("player_id"),
                        rs.getString("game"),
                        rs.getString("platform"),
                        new DateTime(rs.getTimestamp("activity_ts"))
                );
            }
        };
    }

    private List<PlayerActivity> getPlayerActivityList(BigDecimal playerId) {
        return jdbcTemplate.query(SELECT_FROM_PLAYER_ACTIVITY_HOURLY_WHERE_PLAYER_ID, playerActivityRowMapper(), playerId);
    }

    private void createPlayerSessionAndPlayGame(
            final BigDecimal playerId,
            final String game,
            final Platform platform,
            final boolean isPlayed, final boolean isNewTable, DateTime sessionTs, DateTime auditTs, boolean aSesssion, BigDecimal accountId) {
        BigDecimal gameVariationTemplateId;
        Long gameId;
        BigDecimal tableId;

        if (aSesssion) {
            createSessionForPlayer(playerId, sessionTs, platform, accountId);
        }

        if (POKER.equals(game)) {
            tableId = valueOf(1735l);
            gameId = 6937563L;
            gameVariationTemplateId = new BigDecimal(58635);
        } else if (SLOTS.equals(game)) {
            tableId = valueOf(123);
            gameId = 456l;
            gameVariationTemplateId = new BigDecimal(58639);
        } else if (BLACKJACK.equals(game))  {
            tableId = valueOf(125);
            gameId = 457l;
            gameVariationTemplateId = new BigDecimal(58649);
        } else {
            throw new RuntimeException("don't have game variation template Id for this game");
        }

        if (isNewTable) {
            createTable(game, tableId, gameVariationTemplateId);
        }
        if (isPlayed) {
            playGame(playerId, auditTs, tableId, gameId);
        }
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
        PostgresTestValueHelper.createSession(postgresSessionKeyDAO, jdbcTemplate, playerId, auditTs, platform, accountId, valueOf(
                sessionIdSource--));
    }

}


