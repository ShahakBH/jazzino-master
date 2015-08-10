package com.yazino.bi.aggregator;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;

import static java.lang.Boolean.TRUE;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlayerActivityDailyTest {
    public static final String AGGREGATOR_ID = "player_activity_daily";
    @Mock
    private JdbcTemplate template;
    @Mock
    private AggregatorLastUpdateDAO aggregatorLastUpdateDAO;
    @Mock
    private AggregatorLockDao aggregatorLockDao;
    @Mock
    private YazinoConfiguration configuration;

    PlayerActivityDaily underTest;

    @Before
    public void setUp() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime().getMillis());
        MockitoAnnotations.initMocks(this);
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(Boolean.TRUE);
        when(configuration.getBoolean("data-warehouse.aggregators.enabled")).thenReturn(TRUE);
        when(aggregatorLockDao.lock(eq(AGGREGATOR_ID), anyString())).thenReturn(Boolean.TRUE);

        underTest = new PlayerActivityDaily(template, aggregatorLastUpdateDAO, aggregatorLockDao, configuration);
    }

    @Test
    public void updateShouldRunSqlForTenDaysAtATimeFromTheFirstAuditCommandIfNeverRunBefore()    {
        when(aggregatorLastUpdateDAO.getLastRun(AGGREGATOR_ID)).thenReturn(null);
        when(template.queryForObject(anyString(), any(RowMapper.class))).thenReturn(new DateTime().minusDays(135));
        underTest.update();

        verify(template, times(135)).update(contains("INSERT INTO player_activity_daily"), any(PreparedStatementSetter.class));
        verify(template, times(135)).update(contains("DELETE FROM player_activity_daily "), any(PreparedStatementSetter.class));
//        verify(template, times(135)).update(anyString(), any(PreparedStatementSetter.class));
        verify(aggregatorLastUpdateDAO).updateLastRunFor(AGGREGATOR_ID, new Timestamp(new DateTime().minusDays(125).getMillis()));
    }

    @Test
    public void updateShouldDoNothingIfNoAuditCommandsInAuditCommand()    {
        when(aggregatorLastUpdateDAO.getLastRun(AGGREGATOR_ID)).thenReturn(null);
        when(template.queryForObject(anyString(), any(RowMapper.class))).thenReturn(null);
        underTest.update();

        verify(template, times(0)).update(anyString(), any(PreparedStatementSetter.class));
        verify(aggregatorLastUpdateDAO, times(0)).updateLastRunFor(AGGREGATOR_ID, new Timestamp(new DateTime().minusDays(105).getMillis()));
    }

    @Test
    public void updateShouldRunSQLWithCorrectParamsIfRunItHasBeenRunTheDayBefore() throws Exception {
        when(aggregatorLastUpdateDAO.getLastRun(AGGREGATOR_ID)).thenReturn(new Timestamp(new DateTime().minusDays(1).getMillis()));
        underTest.update();

        verify(template, times(1)).update(contains("INSERT INTO player_activity_daily"), any(PreparedStatementSetter.class));
        verify(template, times(1)).update(contains("DELETE FROM player_activity_daily "), any(PreparedStatementSetter.class));
        verify(aggregatorLastUpdateDAO, times(2)).updateLastRunFor(AGGREGATOR_ID, new Timestamp(new DateTime().toDateMidnight().getMillis()));
    }

    @Test
    public void updateShouldTryToCatchUpADayIfItHasMissedSomeDays() throws Exception {
        when(aggregatorLastUpdateDAO.getLastRun(AGGREGATOR_ID)).thenReturn(new Timestamp(new DateTime().minusDays(4).getMillis()));
        underTest.update();

        verify(template, times(4)).update(contains("INSERT INTO player_activity_daily"), any(PreparedStatementSetter.class));
        verify(template, times(4)).update(contains("DELETE FROM player_activity_daily "), any(PreparedStatementSetter.class));
        verify(aggregatorLastUpdateDAO, times(2)).updateLastRunFor(AGGREGATOR_ID, new Timestamp(new DateTime().toDateMidnight().getMillis()));
        verify(aggregatorLastUpdateDAO).updateLastRunFor(AGGREGATOR_ID, new Timestamp(new DateTime().minusDays(1).getMillis()));
        verify(aggregatorLastUpdateDAO).updateLastRunFor(AGGREGATOR_ID, new Timestamp(new DateTime().minusDays(2).getMillis()));
        verify(aggregatorLastUpdateDAO).updateLastRunFor(AGGREGATOR_ID, new Timestamp(new DateTime().minusDays(3).getMillis()));
    }

    @Test
    public void updateShouldNeverUpdateLastTimeStampPastCurrentTime()   {
        when(aggregatorLastUpdateDAO.getLastRun(AGGREGATOR_ID)).thenReturn(null);
        when(template.queryForObject(anyString(), any(RowMapper.class))).thenReturn(new DateTime().minusDays(1));
        underTest.update();

        verify(aggregatorLastUpdateDAO, times(2)).updateLastRunFor(AGGREGATOR_ID, new Timestamp(new DateTime().toDateMidnight().getMillis()));
    }
}
