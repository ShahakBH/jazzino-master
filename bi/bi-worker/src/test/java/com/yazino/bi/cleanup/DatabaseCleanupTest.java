package com.yazino.bi.cleanup;

import com.yazino.bi.aggregator.AggregatorLockDao;
import com.yazino.bi.aggregator.HostUtils;
import com.yazino.configuration.YazinoConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DatabaseCleanupTest {

    private DatabaseCleanup underTest;
    @Mock
    private YazinoConfiguration mockConfig;
    @Mock
    private AggregatorLockDao mockAggLockDao;

    @Mock
    private NamedParameterJdbcTemplate mockTemplate;
    private SqlParameterSource paramSource;
    private final static String sql = "delete stuff";

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        underTest = new DatabaseCleanup(mockAggLockDao, mockConfig, mockTemplate, "DBCleanup");
    }

    @Test
    public void runShouldAcquireLockAndExecute() {
        final String hostName = HostUtils.getHostName();
        paramSource = new MapSqlParameterSource();
        when(mockConfig.getBoolean("strata.database.cleanup.DBCleanup.enable", false)).thenReturn(true);
        when(mockAggLockDao.lock(anyString(), anyString())).thenReturn(true);
        when(mockTemplate.update(sql, paramSource)).thenReturn(1);

        assertThat(underTest.run(sql, paramSource), is(1));

        verify(mockAggLockDao).lock("DBCleanup", hostName);
        verify(mockAggLockDao).unlock("DBCleanup", hostName);
        verify(mockTemplate).update(sql, paramSource);
    }

    @Test
    public void lockedDaoShouldNotExecute() {
        paramSource = new MapSqlParameterSource();
        when(mockConfig.getBoolean("strata.database.cleanup.DBCleanup.enable", false)).thenReturn(true);
        when(mockAggLockDao.lock(anyString(), anyString())).thenReturn(false);

        assertThat(underTest.run(sql, paramSource), is(0));

        verifyZeroInteractions(mockTemplate);
        verify(mockAggLockDao, times(0)).unlock(anyString(), anyString());
    }

    @Test
    public void disabledCleanupShouldNotRun() {
        when(mockConfig.getBoolean("strata.database.cleanup.DBCleanup.enable", false)).thenReturn(false);

        assertThat(underTest.run(sql, paramSource), is(0));

        verifyZeroInteractions(mockTemplate);
        verifyZeroInteractions(mockAggLockDao);
    }

}