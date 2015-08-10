package com.yazino.bi.aggregator;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;

import static java.lang.Boolean.TRUE;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class PlayerActivityHourlyTest {
    public static final String AGGREGATOR_ID = "player_activity_hourly";
    @Mock
    private JdbcTemplate template;
    @Mock
    private AggregatorLastUpdateDAO aggregatorLastUpdateDAO;
    @Mock
    private AggregatorLockDao aggregatorLockDao;
    @Mock
    private YazinoConfiguration configuration;

    PlayerActivityHourly underTest;

    @Before
    public void setUp() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime().getMillis());
        MockitoAnnotations.initMocks(this);
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(Boolean.TRUE);
        when(configuration.getBoolean("data-warehouse.aggregators.enabled")).thenReturn(TRUE);
        when(aggregatorLockDao.lock(eq(AGGREGATOR_ID), anyString())).thenReturn(Boolean.TRUE);

        underTest = new PlayerActivityHourly(template, aggregatorLastUpdateDAO, aggregatorLockDao, configuration);
    }

    @Test
    public void updateShouldRunSqlForOneDaysAtATimeFromTheFirstAuditCommandIfNeverRunBefore() {
        when(aggregatorLastUpdateDAO.getLastRun(AGGREGATOR_ID)).thenReturn(null);
        DateTime firstAuditCommandDate = new DateTime().minusDays(135);
        when(template.queryForObject(anyString(), any(RowMapper.class))).thenReturn(firstAuditCommandDate);
        underTest.update();

        verify(template, times(135)).update(anyString(), any(PreparedStatementSetter.class));

        for (int i = 1; i <= 134; ++i) {
            verify(aggregatorLastUpdateDAO).updateLastRunFor(AGGREGATOR_ID, new Timestamp(firstAuditCommandDate.plusDays(i).getMillis()));
        }
        verify(aggregatorLastUpdateDAO, times(2)).updateLastRunFor(AGGREGATOR_ID, new Timestamp(firstAuditCommandDate.plusDays(135).getMillis()));
    }

    @Test
    public void updateShouldDoNothingIfNoAuditCommandsInAuditCommand() {
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

        verify(template).update(anyString(), any(PreparedStatementSetter.class));
        verify(aggregatorLastUpdateDAO, times(2)).updateLastRunFor(AGGREGATOR_ID, new Timestamp(new DateTime().getMillis()));
    }
    @Test
    public void updateShouldInsertDataIntoStagingTable() {
        when(aggregatorLastUpdateDAO.getLastRun(AGGREGATOR_ID)).thenReturn(new Timestamp(new DateTime().minusDays(1).getMillis()));
        underTest.update();

        verify(template).update(Matchers.contains("INSERT INTO STG_PLAYER_ACTIVITY_HOURLY"), any(PreparedStatementSetter.class));
    }

    @Test
    public void updateShouldInsertFromStagingTableToPlayerHourlyTableWhereNoRecordExists(){
        when(aggregatorLastUpdateDAO.getLastRun(AGGREGATOR_ID)).thenReturn(new Timestamp(new DateTime().minusDays(1).getMillis()));
        underTest.update();

        verify(template).update(Matchers.contains("INSERT INTO STG_PLAYER_ACTIVITY_HOURLY"), any(PreparedStatementSetter.class));
        verify(template).batchUpdate(eq(new String[]{PlayerActivityHourly.SQL_EXECUTE_UPDATES,
                PlayerActivityHourly.SQL_EXECUTE_INSERTS,
                PlayerActivityHourly.SQL_CLEAN_STAGING}));
        //needs to match on dates as they are the unique id
    }


    @Test
    public void updateShouldTryToCatchUpADayIfItHasMissedADay() throws Exception {
        when(aggregatorLastUpdateDAO.getLastRun(AGGREGATOR_ID)).thenReturn(new Timestamp(new DateTime().minusDays(4).getMillis()));
        underTest.update();

        verify(template, times(4)).update(anyString(), any(PreparedStatementSetter.class));
        verify(aggregatorLastUpdateDAO).updateLastRunFor(AGGREGATOR_ID, new Timestamp(new DateTime().minusDays(3).getMillis()));
        verify(aggregatorLastUpdateDAO).updateLastRunFor(AGGREGATOR_ID, new Timestamp(new DateTime().minusDays(2).getMillis()));
        verify(aggregatorLastUpdateDAO).updateLastRunFor(AGGREGATOR_ID, new Timestamp(new DateTime().minusDays(1).getMillis()));
        verify(aggregatorLastUpdateDAO, times(2)).updateLastRunFor(AGGREGATOR_ID, new Timestamp(new DateTime().getMillis()));
    }

    @Test
    public void updateShouldNeverUpdateLastTimeStampPastCurrentTime() {
        when(aggregatorLastUpdateDAO.getLastRun(AGGREGATOR_ID)).thenReturn(null);
        when(template.queryForObject(anyString(), any(RowMapper.class))).thenReturn(new DateTime().minusDays(1));
        underTest.update();

        verify(aggregatorLastUpdateDAO, times(2)).updateLastRunFor(AGGREGATOR_ID, new Timestamp(new DateTime().getMillis()));
    }

}
