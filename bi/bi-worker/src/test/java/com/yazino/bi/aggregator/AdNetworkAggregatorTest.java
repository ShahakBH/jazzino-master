package com.yazino.bi.aggregator;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;

import java.sql.Timestamp;

import static java.lang.Boolean.TRUE;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;


public class AdNetworkAggregatorTest {
    @Mock
    private JdbcTemplate template;
    @Mock
    private AggregatorLastUpdateDAO aggregatorLastUpdateDAO;
    @Mock
    private AggregatorLockDao aggregatorLockDao;
    @Mock
    private YazinoConfiguration configuration;

    private AdNetworkAggregator underTest;
    private DateTime now;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        now = new DateTime(2010, 6, 15, 12, 0, 0);

        ThreadLocalDateTimeUtils.setCurrentMillisFixed(now.getMillis());
        underTest = new AdNetworkAggregator(template, aggregatorLastUpdateDAO, aggregatorLockDao, configuration);
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(Boolean.TRUE);
        when(configuration.getBoolean("data-warehouse.aggregators.enabled")).thenReturn(TRUE);
        when(configuration.getString("strata.aggregators.adnet.mapping")).thenReturn("update dmr_player_activity_and_purchases  set registration_adnet = adnet_mappings.registration_adnet from adnet_mappings where dmr_player_activity_and_purchases.referrer like adnet_mappings.referrer+'%';");
        when(aggregatorLockDao.lock(eq(underTest.getAggregatorId()), anyString())).thenReturn(Boolean.TRUE);


    }

    @After
    public void tearDown() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();

    }

    @Test
    public void updateShouldRunOnce() {
        underTest.update();
        verify(aggregatorLastUpdateDAO, times(1)).updateLastRunFor(AdNetworkAggregator.ID, new Timestamp(now.toDateMidnight().getMillis()));

    }

    @Test
    public void updateShouldRunAllQueries() {
        underTest.update();
        for (String query : underTest.getQueries()) {
            verify(template).update(eq(query), any(PreparedStatementSetter.class));
        }

    }

}
