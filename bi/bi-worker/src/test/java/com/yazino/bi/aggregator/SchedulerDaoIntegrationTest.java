package com.yazino.bi.aggregator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
@DirtiesContext
@Transactional
public class SchedulerDaoIntegrationTest {

    @Autowired
    private JdbcTemplate externalDwJdbcTemplate;

    @Autowired
    private SchedulerDao schedulerDao;

    @Before
    public void cleanUp() {
        externalDwJdbcTemplate.execute("DELETE FROM scheduled_aggregators WHERE aggregator = 'your mum'");
    }

    @Test(expected = DuplicateKeyException.class)
    public void getScheduledAggregatorsShouldLoadUniqueAggregators() {
        externalDwJdbcTemplate.execute("insert into scheduled_aggregators values('your mum')");
        externalDwJdbcTemplate.execute("insert into scheduled_aggregators values('your mum')");
    }

    @Test
    public void getScheduledAggregatorsShouldDeleteAggregatorsAfterwards() {
        externalDwJdbcTemplate.execute("insert into scheduled_aggregators values('your mum')");
        schedulerDao.getScheduledAggregators();
        assertThat(externalDwJdbcTemplate.queryForInt("select count(*) from scheduled_aggregators"),is(0));
    }

}
