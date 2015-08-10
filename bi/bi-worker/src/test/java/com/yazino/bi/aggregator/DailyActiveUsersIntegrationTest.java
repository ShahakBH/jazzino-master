package com.yazino.bi.aggregator;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;

import static java.lang.Boolean.TRUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
@DirtiesContext
@Transactional
public class DailyActiveUsersIntegrationTest {

    public static final String AGGREGATOR_ID = "daily_active_users";
    public static final String IOS = "IOS";
    public static final String POKER = "POKER";
    public static final String SLOTS = "SLOTS";
    @Autowired
    private JdbcTemplate template;
    @Mock
    private AggregatorLastUpdateDAO aggregatorLastUpdateDAO;
    @Mock
    private AggregatorLockDao aggregatorLockDao;
    @Mock
    private YazinoConfiguration configuration;

    DailyActiveUsers underTest;

    @Before
    public void setUp() {
        template.update("delete from dau_mau");
        template.update("delete from player_activity_daily");
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime().getMillis());
        MockitoAnnotations.initMocks(this);
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(Boolean.TRUE);
        when(configuration.getBoolean("data-warehouse.aggregators.enabled")).thenReturn(TRUE);
        when(aggregatorLockDao.lock(eq(AGGREGATOR_ID), anyString())).thenReturn(Boolean.TRUE);

        underTest = new DailyActiveUsers(template, aggregatorLastUpdateDAO, aggregatorLockDao, configuration);
    }


    @Test
    public void updateShouldInsertUniquePlayersForPlatformAndGame() {
        when(aggregatorLastUpdateDAO.getLastRun(AGGREGATOR_ID)).thenReturn(new Timestamp(new DateTime().minusDays(1).getMillis()));
        insertIntoPlayerDailyActivity(BigDecimal.TEN, POKER, IOS, new Timestamp(new DateTime().minusDays(1).getMillis()), "your mum", new Timestamp(12313l));
        insertIntoPlayerDailyActivity(new BigDecimal("2"), SLOTS, IOS, new Timestamp(new DateTime().minusDays(1).getMillis()), "your mum", new Timestamp(12313l));


        underTest.update();
        Map<String, Object> resultMap = getRecordForPlatformAndGame(IOS, POKER);

        assertThat((Integer) resultMap.get("num_players"), is(1));
    }

    @Test
    public void updateShouldWork() {
        when(aggregatorLastUpdateDAO.getLastRun(PlayerActivityDaily.ID)).thenReturn(new Timestamp(new DateTime().toDateMidnight().getMillis()));
        when(aggregatorLastUpdateDAO.getLastRun(AGGREGATOR_ID)).thenReturn(new Timestamp(new DateTime().minusDays(3).getMillis()));
        insertIntoPlayerDailyActivity(BigDecimal.TEN, POKER, IOS, new Timestamp(new DateTime().minusDays(1).getMillis()), "your mum", new Timestamp(12313l));
        insertIntoPlayerDailyActivity(new BigDecimal("2"), SLOTS, IOS, new Timestamp(new DateTime().minusDays(1).getMillis()), "your mum", new Timestamp(12313l));


        underTest.update();
        Map<String, Object> resultMap = getRecordForPlatformAndGame(IOS, POKER);

        assertThat((Integer) resultMap.get("num_players"), is(1));
    }

    @Test
    public void updateShouldCountUniquePlayersOnAllGamesForOnePlatform() {
        when(aggregatorLastUpdateDAO.getLastRun(AGGREGATOR_ID)).thenReturn(new Timestamp(new DateTime().minusDays(1).getMillis()));
        insertIntoPlayerDailyActivity(BigDecimal.TEN, POKER, IOS, new Timestamp(new DateTime().minusDays(1).getMillis()), "your mum", new Timestamp(12313l));
        insertIntoPlayerDailyActivity(BigDecimal.TEN, SLOTS, IOS, new Timestamp(new DateTime().minusDays(1).getMillis()), "your mum", new Timestamp(12313l));
        insertIntoPlayerDailyActivity(BigDecimal.ONE, SLOTS, IOS, new Timestamp(new DateTime().minusDays(1).getMillis()), "your mum", new Timestamp(12313l));

        underTest.update();
        Map<String, Object> resultMap = getRecordForPlatformAndGame(IOS, "*");

        assertThat((Integer) resultMap.get("num_players"), is(2));
    }

    @Test
    public void updateShouldOnlyInsertRecordsForCorrectDay() {
        when(aggregatorLastUpdateDAO.getLastRun(AGGREGATOR_ID)).thenReturn(new Timestamp(new DateTime().minusDays(1).getMillis()));
        insertIntoPlayerDailyActivity(BigDecimal.TEN, POKER, IOS, new Timestamp(new DateTime().minusDays(1).getMillis()), "your mum", new Timestamp(12313l));
        insertIntoPlayerDailyActivity(BigDecimal.ONE, POKER, IOS, new Timestamp(new DateTime().minusDays(2).getMillis()), "your mum", new Timestamp(12313l));
        insertIntoPlayerDailyActivity(new BigDecimal("2"), POKER, IOS, new Timestamp(new DateTime().getMillis()), "your mum", new Timestamp(12313l));

        underTest.update();
        Map<String, Object> resultMap = getRecordForPlatformAndGame(IOS, POKER);

        assertThat((Integer) resultMap.get("num_players"), is(1));
    }

    @Test
    public void updateCountNumberOfPlayersPerGame() {
        when(aggregatorLastUpdateDAO.getLastRun(AGGREGATOR_ID)).thenReturn(new Timestamp(new DateTime().minusDays(1).getMillis()));
        insertIntoPlayerDailyActivity(BigDecimal.TEN, POKER, IOS, new Timestamp(new DateTime().minusDays(1).getMillis()), "your mum", new Timestamp(12313l));
        insertIntoPlayerDailyActivity(BigDecimal.ONE, POKER, IOS, new Timestamp(new DateTime().minusDays(1).getMillis()), "your mum", new Timestamp(12313l));
        insertIntoPlayerDailyActivity(new BigDecimal("2"), POKER, IOS, new Timestamp(new DateTime().minusDays(1).getMillis()), "your mum", new Timestamp(12313l));
        insertIntoPlayerDailyActivity(new BigDecimal("4"), SLOTS, IOS, new Timestamp(new DateTime().minusDays(1).getMillis()), "your mum", new Timestamp(12313l));

        underTest.update();
        Map<String, Object> resultMap = getRecordForPlatformAndGame(IOS, POKER);

        assertThat((Integer) resultMap.get("num_players"), is(3));
    }

    @Test
    public void updateShouldOnlyCountPlayerOnDifferentPlatformsOnce() {
        when(aggregatorLastUpdateDAO.getLastRun(AGGREGATOR_ID)).thenReturn(new Timestamp(new DateTime().minusDays(1).getMillis()));
        insertIntoPlayerDailyActivity(BigDecimal.TEN, POKER, IOS, new Timestamp(new DateTime().minusDays(1).getMillis()), "your mum", new Timestamp(12313l));
        insertIntoPlayerDailyActivity(BigDecimal.TEN, POKER, "WEB", new Timestamp(new DateTime().minusDays(1).getMillis()), "your mum", new Timestamp(12313l));
        insertIntoPlayerDailyActivity(BigDecimal.TEN, POKER, "ANDROID", new Timestamp(new DateTime().minusDays(1).getMillis()), "your mum", new Timestamp(12313l));

        underTest.update();
        Map<String, Object> resultMap = getRecordForPlatformAndGame("*", POKER);

        assertThat((Integer) resultMap.get("num_players"), is(1));
    }

    @Test
    public void updateShouldCountAllPlayersOnAllPlatforms() {
        when(aggregatorLastUpdateDAO.getLastRun(AGGREGATOR_ID)).thenReturn(new Timestamp(new DateTime().minusDays(1).getMillis()));
        insertIntoPlayerDailyActivity(BigDecimal.TEN, POKER, IOS, new Timestamp(new DateTime().minusDays(1).getMillis()), "your mum", new Timestamp(12313l));
        insertIntoPlayerDailyActivity(BigDecimal.TEN, POKER, "WEB", new Timestamp(new DateTime().minusDays(1).getMillis()), "your mum", new Timestamp(12313l));
        insertIntoPlayerDailyActivity(BigDecimal.TEN, POKER, "ANDROID", new Timestamp(new DateTime().minusDays(1).getMillis()), "your mum", new Timestamp(12313l));
        insertIntoPlayerDailyActivity(BigDecimal.TEN, SLOTS, IOS, new Timestamp(new DateTime().minusDays(1).getMillis()), "your mum", new Timestamp(12313l));
        insertIntoPlayerDailyActivity(BigDecimal.TEN, SLOTS, "WEB", new Timestamp(new DateTime().minusDays(1).getMillis()), "your mum", new Timestamp(12313l));
        insertIntoPlayerDailyActivity(BigDecimal.TEN, SLOTS, "ANDROID", new Timestamp(new DateTime().minusDays(1).getMillis()), "your mum", new Timestamp(12313l));

        underTest.update();
        Map<String, Object> resultMap = getRecordForPlatformAndGame("*", "*");

        assertThat((Integer) resultMap.get("num_players"), is(1));
    }



    private Map<String, Object> getRecordForPlatformAndGame(String platform, String game) {
        return template.queryForMap("SELECT * FROM dau_mau where platform = ? and game_type = ?", platform, game);
    }

    private void insertIntoPlayerDailyActivity(final BigDecimal playerId, final String game, final String platform, final Timestamp activity_ts, final String referrer, final Timestamp reg_ts) {
        template.update("INSERT INTO player_activity_daily (player_id, game, platform, activity_ts, referrer, reg_ts) VALUES (?,?,?,?,?,?)"
        , new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps) throws SQLException {
                ps.setBigDecimal(1, playerId);
                ps.setString(2, game);
                ps.setString(3, platform);
                ps.setTimestamp(4, activity_ts);
                ps.setString(5, referrer);
                ps.setTimestamp(6, reg_ts);

            }
        });
    }
}
