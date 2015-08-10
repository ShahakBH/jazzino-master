package com.yazino.bi.operations.persistence;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import com.yazino.bi.operations.persistence.facebook.data.FacebookAdsStatsData;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ContextConfiguration
@DirtiesContext
@RunWith(SpringJUnit4ClassRunner.class)
public class DwFacebookDataRecordingDaoIntegrationTest {
    @Autowired
    private FacebookDataRecordingDao underTest;

    @Autowired
    @Qualifier("dwJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    private Date future = new DateTime().plusMonths(1).withTime(0, 0, 0, 0).toDate();
    private SimpleDateFormat df = new SimpleDateFormat("yyy-MM-dd");

    private Map<String, FacebookAdsStatsData> fbDataMap;

    @Before
    public void init() {
        cleanUp();
        fbDataMap = createFaceBookAdsStatsDataMap();
    }

    private Map<String, FacebookAdsStatsData> createFaceBookAdsStatsDataMap() {
        Map<String, FacebookAdsStatsData> fbDataMap = new HashMap<String, FacebookAdsStatsData>();
        FacebookAdsStatsData fbDataX = createFacebookAdsStatsData(1000L, 100L, 99000L);
        fbDataMap.put("X", fbDataX);
        return fbDataMap;
    }

    private FacebookAdsStatsData createFacebookAdsStatsData(long clicks, long impressions, long spent) {
        FacebookAdsStatsData fbData = new FacebookAdsStatsData();
        fbData.setClicks(clicks);
        fbData.setImpressions(impressions);
        fbData.setSpent(spent);
        return fbData;
    }

    @After
    public void cleanUp() {
        jdbcTemplate.update("DELETE FROM FACEBOOK_AD_STATS WHERE TRACKING_DATE= DATE(?)", future);
    }

    @Test
    public void shouldSaveFacebookDataForDate() {
        // GIVEN the facebook data

        // WHEN saving the data
        underTest.saveFacebookData(future, fbDataMap);

        // THEN the data becomes available in the DB
        final List<Object> retval = jdbcTemplate
                .query("SELECT CLICKS,IMPRESSIONS,SPENT,SOURCE FROM FACEBOOK_AD_STATS WHERE TRACKING_DATE= DATE(?)",
                        new Object[]{future}, new RowMapper<Object>() {
                    @Override
                    public Object mapRow(final ResultSet rs,
                                         final int rowNum) throws SQLException {
                        assertThat(rs.getLong("CLICKS"), is(1000L));
                        assertThat(rs.getLong("IMPRESSIONS"), is(100L));
                        assertThat(rs.getLong("SPENT"), is(99000L));
                        assertThat(rs.getString("SOURCE"), is("X"));
                        return null;
                    }
                });
        assertThat(retval.size(), is(1));
    }

    @Test
    public void shouldReturnLastRecordedDate() {
        // GIVEN a recorded date in the future
        underTest.saveFacebookData(future, fbDataMap);

        // WHEN asking for the latest recorded date
        final Date lastDate = underTest.getLatestRecordDate();

        // THEN the right one is returned
        assertThat(df.format(lastDate), is(df.format(future)));
    }
}
