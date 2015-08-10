package com.yazino.bi.aggregator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
@DirtiesContext
@Transactional
public class AggregatorLastUpdateDAOIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AggregatorLastUpdateDAO underTest;

    @Before
    public void setUp() {
        jdbc.update("delete from aggregator_last_update");
    }

    @Test
    public void getLastRunShouldReturnNullWhenNoPreviousRunExists() {
        assertNull(underTest.getLastRun("non-existent"));
    }

    @Test
    public void getLastRunShouldReturnLastRunDate() {
        jdbc.update("insert into aggregator_last_update (id, last_run_ts) values (?,?)", "existent", new Timestamp(100l));
        assertThat(underTest.getLastRun("existent"), is(equalTo(new Timestamp(100l))));
    }

    @Test
    public void updateLastRunForShouldInsertNewValue() {
        String aggregatorId = "non-existent";
        underTest.updateLastRunFor(aggregatorId, new Timestamp(100l));
        Timestamp timestamp = jdbc.queryForObject("select last_run_ts from aggregator_last_update where id = ?", Timestamp.class, aggregatorId);
        assertThat(timestamp, is(equalTo(new Timestamp(100l))));
    }

    @Test
    public void updateLastRunShouldUpdateTimestamp() {
        jdbc.update("insert into aggregator_last_update (id, last_run_ts) values (?,?)", "existent", new Timestamp(100l));
        String aggregatorId = "existent";
        underTest.updateLastRunFor(aggregatorId, new Timestamp(200l));
        Timestamp timestamp = jdbc.queryForObject("select last_run_ts from aggregator_last_update where id = ?", Timestamp.class, aggregatorId);
        assertThat(timestamp, is(equalTo(new Timestamp(200l))));
    }
}
