package com.yazino.bi.aggregator;

import com.yazino.configuration.YazinoConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AggregatorLockDAOTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    YazinoConfiguration configuration;
    private AggregatorLockDao underTest;


    @Before
    public void setUp() throws Exception {

        underTest = new AggregatorLockDao(jdbcTemplate, configuration);
    }

    @Test(expected = CannotAcquireLockException.class)
    public void lockShouldThrowCannotAcquireLockExceptionOnLockFailure() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString())).thenThrow(new CannotAcquireLockException("aTestException"));
        underTest.lock("", "");
    }

    @Test
    public void unLockShouldCatchExceptionAndNotBlowUp() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString())).thenThrow(new RuntimeException());
        underTest.unlock("", "");
    }

}
