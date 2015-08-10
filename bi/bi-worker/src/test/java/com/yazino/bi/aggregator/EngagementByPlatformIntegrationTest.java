package com.yazino.bi.aggregator;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.Platform;
import com.yazino.platform.audit.message.Transaction;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import strata.server.worker.audit.persistence.PostgresSessionKeyDAO;
import strata.server.worker.audit.persistence.PostgresTransactionLogDAO;
import strata.server.worker.event.persistence.PostgresTableDWDAO;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Boolean.TRUE;
import static java.math.BigDecimal.valueOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static utils.PostgresTestValueHelper.createSession;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
@DirtiesContext
public class EngagementByPlatformIntegrationTest {

    private static final String AGGREGATOR_ID = EngagementByPlatform.ID;
    public static final BigDecimal PLAYER_ID = valueOf(1.00);
    public static final BigDecimal ACCOUNT_ID = BigDecimal.TEN;
    public static final DateTime NOW = new DateTime();
    private Timestamp NOW_TS = new Timestamp(NOW.getMillis());
    private Timestamp YESTERDAY_TS = new Timestamp(NOW.minusDays(1).getMillis());

    private long sessionIdCreateSource = -1;
    private long sessionIdWriteSource = -1;

    @Autowired
    @Qualifier("engagementByPlatform")
    EngagementByPlatform underTest;

    @Autowired
    private AggregatorLastUpdateDAO aggregatorLastUpdateDAO;

    @Autowired
    private PostgresTransactionLogDAO postgresTransactionLogDao;

    @Autowired
    private PostgresSessionKeyDAO postgresSessionKeyDAO;

    @Autowired
    private PostgresTableDWDAO postgresTableDWDAO;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private YazinoConfiguration configuration;

    @Autowired
    private AggregatorLockDao lockDao;

    @Before
    public void setUp() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(System.currentTimeMillis());
        configuration.setProperty("data-warehouse.write.enabled", TRUE);
        configuration.setProperty("data-warehouse.aggregators.enabled", TRUE);

        underTest = new EngagementByPlatform(jdbcTemplate, aggregatorLastUpdateDAO, lockDao, configuration);
    }

    @Before
    @After
    public void cleanUpDatabase() {
        jdbcTemplate.update("DELETE FROM aggregator_lock");
        jdbcTemplate.update("DELETE FROM engagement_by_platform");
        jdbcTemplate.update("DELETE FROM audit_command");
        jdbcTemplate.update("DELETE FROM transaction_log");
        jdbcTemplate.update("DELETE FROM account_session");
        jdbcTemplate.update("DELETE FROM table_definition");
        jdbcTemplate.update("DELETE FROM lobby_user");
        jdbcTemplate.update("DELETE FROM aggregator_last_update");
        jdbcTemplate.update("DELETE FROM game_variation_template");

        jdbcTemplate.update("INSERT INTO game_variation_template VALUES (1,'BLACKJACK', 'European Template')");
    }

    @After
    public void tearDown() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void updateShouldMaterializeDataForSameDayIfRunTwice() {
        final Timestamp dayBeforeYesterday = new Timestamp(NOW.minusDays(2).getMillis());
        createSessionForPlayer(2, 2, YESTERDAY_TS);
        createSessionForPlayer(2, 2, dayBeforeYesterday);
        createTable("BLACKJACK", BigDecimal.ONE, BigDecimal.ONE);

        postgresTransactionLogDao.saveAll(getTransaction(2, 2, YESTERDAY_TS));
        postgresTransactionLogDao.saveAll(getTransaction(2, 2, dayBeforeYesterday));

        aggregatorLastUpdateDAO.updateLastRunFor(AGGREGATOR_ID, YESTERDAY_TS);
        underTest.updateWithLocks(NOW_TS);

        aggregatorLastUpdateDAO.updateLastRunFor(AGGREGATOR_ID, YESTERDAY_TS);
        underTest.updateWithLocks(NOW_TS);

        assertThat(aggregatorLastUpdateDAO.getLastRun(AGGREGATOR_ID), equalTo(NOW_TS));
        final List<Map<String, Object>> engagementEnties = jdbcTemplate.queryForList("SELECT * FROM engagement_by_platform");
        assertThat(engagementEnties.size(), equalTo(1));
        Map<String, Object> entry = engagementEnties.get(0);
        assertThat(entry.get("platform").toString(), equalTo("WEB"));
        assertThat(entry.get("num_players").toString(), equalTo("2"));
        assertThat(entry.get("num_transactions").toString(), equalTo("4"));
        assertThat(entry.get("total_amount").toString(), equalTo("2698.00"));
    }


    @Test
    public void updateShouldMaterializeDataForPreviousDay() {
        final Timestamp dayBeforeYesterday = new Timestamp(NOW.minusDays(2).getMillis());
        createSessionForPlayer(2, 2, YESTERDAY_TS);
        createSessionForPlayer(2, 2, dayBeforeYesterday);
        createTable("BLACKJACK", BigDecimal.ONE, BigDecimal.ONE);

        postgresTransactionLogDao.saveAll(getTransaction(2, 2, YESTERDAY_TS));
        postgresTransactionLogDao.saveAll(getTransaction(2, 2, dayBeforeYesterday));

        aggregatorLastUpdateDAO.updateLastRunFor(AGGREGATOR_ID, YESTERDAY_TS);
        underTest.updateWithLocks(NOW_TS);

        assertThat(aggregatorLastUpdateDAO.getLastRun(AGGREGATOR_ID), equalTo(NOW_TS));
        final List<Map<String, Object>> engagementEnties = jdbcTemplate.queryForList("SELECT * FROM engagement_by_platform");
        assertThat(engagementEnties.size(), equalTo(1));
        Map<String, Object> entry = engagementEnties.get(0);
        assertThat(entry.get("platform").toString(), equalTo("WEB"));
        assertThat(entry.get("num_players").toString(), equalTo("2"));
        assertThat(entry.get("num_transactions").toString(), equalTo("4"));
        assertThat(entry.get("total_amount").toString(), equalTo("2698.00"));
    }

    @Test
    public void updateShouldMaterializeDataForMultipleDays() {
        final Timestamp dayBeforeYesterday = new Timestamp(NOW.minusDays(2).getMillis());
        createSessionForPlayer(2, 2, YESTERDAY_TS);
        createSessionForPlayer(2, 2, dayBeforeYesterday);
        createTable("BLACKJACK", BigDecimal.ONE, BigDecimal.ONE);

        postgresTransactionLogDao.saveAll(getTransaction(2, 2, YESTERDAY_TS));
        postgresTransactionLogDao.saveAll(getTransaction(2, 2, dayBeforeYesterday));

        aggregatorLastUpdateDAO.updateLastRunFor(AGGREGATOR_ID, dayBeforeYesterday);
        underTest.updateWithLocks(NOW_TS);

        assertThat(aggregatorLastUpdateDAO.getLastRun(AGGREGATOR_ID), equalTo(NOW_TS));
        final List<Map<String, Object>> engagementEnties = jdbcTemplate.queryForList("SELECT * FROM engagement_by_platform");
        assertThat(engagementEnties.size(), equalTo(2));
        Map<String, Object> entry = engagementEnties.get(0);
        assertThat(entry.get("platform").toString(), equalTo("WEB"));
        assertThat(entry.get("num_players").toString(), equalTo("2"));
        assertThat(entry.get("num_transactions").toString(), equalTo("4"));
        assertThat(entry.get("total_amount").toString(), equalTo("2698.00"));
        entry = engagementEnties.get(1);
        assertThat(entry.get("platform").toString(), equalTo("WEB"));
        assertThat(entry.get("num_players").toString(), equalTo("2"));
        assertThat(entry.get("num_transactions").toString(), equalTo("4"));
        assertThat(entry.get("total_amount").toString(), equalTo("2698.00"));
    }

    private List<Transaction> getTransaction(int players, int transactions, Timestamp yesterday) {
        List<Transaction> list = newArrayList();
        players--;//meh. this is test code.
        for (int i = 0; i <= players; i++) {
            for (int j = 1; j <= transactions; j++) {
                list.add(new Transaction(ACCOUNT_ID.add(valueOf(i)),
                        valueOf(666l).add(valueOf(2 * i + 5 * j)),
                        "Stake",
                        "",
                        yesterday.getTime() + 10000 * i + 100 * j,
                        valueOf(555.55),
                        1l,
                        valueOf(1),
                        BigDecimal.valueOf(sessionIdWriteSource--),
                        valueOf(players)));
            }
        }
        return list;
    }

    private void createTable(final String game, final BigDecimal tableId, BigDecimal gameVariationTemplateId) {
        postgresTableDWDAO.saveAll(newArrayList(new TableEvent(tableId, game, gameVariationTemplateId, "templateName")));
    }

    private void createSessionForPlayer(int players, int sessions, Timestamp date) {
        players--;//meh. this is test code.
        for (int i = 0; i <= players; i++) {
            for (int j = 1; j <= sessions; j++) {
                createSession(postgresSessionKeyDAO,
                        jdbcTemplate,
                        PLAYER_ID.add(valueOf(i)),
                        new DateTime(date).minusMinutes(10 * j + 2 * i),
                        Platform.WEB,
                        ACCOUNT_ID.add(BigDecimal.valueOf(i)),
                        BigDecimal.valueOf(sessionIdCreateSource--));
            }
        }

    }
}
