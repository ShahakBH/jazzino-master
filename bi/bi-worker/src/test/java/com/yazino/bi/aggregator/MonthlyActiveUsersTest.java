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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class MonthlyActiveUsersTest {

    public static final String AGGREGATOR_ID = "monthly_active_users";
    @Mock
    private JdbcTemplate template;
    @Mock
    private AggregatorLastUpdateDAO aggregatorLastUpdateDAO;
    @Mock
    private AggregatorLockDao aggregatorLockDao;
    @Mock
    private YazinoConfiguration configuration;

    MonthlyActiveUsers underTest;


    @Before
    public void setUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime().getMillis());
        MockitoAnnotations.initMocks(this);
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(Boolean.TRUE);
        when(configuration.getBoolean("data-warehouse.aggregators.enabled")).thenReturn(TRUE);
        when(aggregatorLockDao.lock(eq(AGGREGATOR_ID), anyString())).thenReturn(Boolean.TRUE);

        underTest = new MonthlyActiveUsers(template, aggregatorLastUpdateDAO, aggregatorLockDao, configuration);
    }

    @Test
    public void updateShouldRunSqlForEveryDayFromTheFirstPlayerActivityMonthly()    {
        when(aggregatorLastUpdateDAO.getLastRun(AGGREGATOR_ID)).thenReturn(null);
        when(template.queryForObject(anyString(), any(RowMapper.class))).thenReturn(new DateTime().minusDays(20));
        underTest.update();

        verify(template, times(20)).update(anyString(), any(PreparedStatementSetter.class));
        verify(aggregatorLastUpdateDAO, times(2)).updateLastRunFor(AGGREGATOR_ID, new Timestamp(new DateTime().toDateMidnight().getMillis()));
        verify(aggregatorLastUpdateDAO, times(21)).updateLastRunFor(eq(AGGREGATOR_ID), any(Timestamp.class));
    }

    @Test
    public void updateShouldRunSQLWithCorrectParamsIfRunItHasBennRunTheDayBefore() throws Exception {
        when(aggregatorLastUpdateDAO.getLastRun(AGGREGATOR_ID)).thenReturn(new Timestamp(new DateTime().minusDays(1).getMillis()));
        underTest.update();

        verify(template, times(1)).update(anyString(), any(PreparedStatementSetter.class));
        verify(aggregatorLastUpdateDAO,times(2)).updateLastRunFor(AGGREGATOR_ID, new Timestamp(new DateTime().toDateMidnight().getMillis()));
    }

    @Test
    public void updateShouldTryToCatchUpADayIfItHasMissedADay() throws Exception {
        when(aggregatorLastUpdateDAO.getLastRun(AGGREGATOR_ID)).thenReturn(new Timestamp(new DateTime().minusDays(4).getMillis()));
        underTest.update();

        verify(template, times(4)).update(anyString(), any(PreparedStatementSetter.class));
        verify(aggregatorLastUpdateDAO,times(2)).updateLastRunFor(AGGREGATOR_ID, new Timestamp(new DateTime().toDateMidnight().getMillis()));
    }
}
