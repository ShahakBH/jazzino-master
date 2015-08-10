package com.yazino.bi.operations.persistence;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;
import com.yazino.bi.operations.persistence.FacebookDataRecordingDao;
import com.yazino.bi.operations.persistence.facebook.data.FacebookAdsStatsData;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;

@ContextConfiguration
@DirtiesContext
@RunWith(SpringJUnit4ClassRunner.class)
@TransactionConfiguration
@Transactional
public class ExternalDwFacebookDataRecordingDaoIntegrationTest {

    @Autowired
    @Qualifier("externalDwFacebookDataRecordingDao")
    private FacebookDataRecordingDao underTest;

    @Autowired
    @Qualifier("externalDwJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    private Date currentDate = new DateTime(2013, 6, 11, 0, 0, 0).toDate();

    @Before
    public void setUp() throws Exception {
        jdbcTemplate.execute("DELETE from FACEBOOK_AD_STATS");
        jdbcTemplate.execute("DELETE from STG_FACEBOOK_AD_STATS");
    }

    @Test
    public void saveFacebookDataShouldInsertTheDataIfNotAlreadyPresentInDb() {

        Map<String, FacebookAdsStatsData> adGroupStats = new TreeMap<String, FacebookAdsStatsData>();
        FacebookAdsStatsData adStatsA = createFaceBookAdsStatsData(100L, 200L, 900L);
        adGroupStats.put("A", adStatsA);
        FacebookAdsStatsData adStatsB = createFaceBookAdsStatsData(101L, 201L, 901L);
        adGroupStats.put("B", adStatsB);
        underTest.saveFacebookData(currentDate, adGroupStats);

        List<FacebookAdsStatsData> valuesFromDb = getFacebookAdsFromDb(currentDate);
        assertThat(valuesFromDb, hasItems(adStatsA, adStatsB));
    }

    @Test
    public void saveFacebookDataShouldUpdateTheDataIfAlreadyPresentInDb() {

        jdbcTemplate.execute("INSERT INTO FACEBOOK_AD_STATS (TRACKING_DATE,SOURCE,CLICKS,IMPRESSIONS,SPENT) VALUES"
                + " ('2013-06-11','A',111,211,333),('2013-06-11','B',444,555,666)");

        Map<String, FacebookAdsStatsData> adGroupStats = new TreeMap<String, FacebookAdsStatsData>();
        FacebookAdsStatsData adStatsA = createFaceBookAdsStatsData(10L, 20L, 90L);
        adGroupStats.put("A", adStatsA);
        FacebookAdsStatsData adStatsB = createFaceBookAdsStatsData(60L, 70L, 801L);
        adGroupStats.put("B", adStatsB);
        underTest.saveFacebookData(currentDate, adGroupStats);

        List<FacebookAdsStatsData> valuesFromDb = getFacebookAdsFromDb(currentDate);
        assertThat(valuesFromDb, hasItems(adStatsA, adStatsB));
    }

    private FacebookAdsStatsData createFaceBookAdsStatsData(long clicks, long impressions, long spent) {

        FacebookAdsStatsData facebookAdsStatsData = new FacebookAdsStatsData();
        facebookAdsStatsData.setClicks(clicks);
        facebookAdsStatsData.setImpressions(impressions);
        facebookAdsStatsData.setSpent(spent);

        return facebookAdsStatsData;

    }

    private List<FacebookAdsStatsData> getFacebookAdsFromDb(Date currentDate) {
        return jdbcTemplate.query("SELECT CLICKS,IMPRESSIONS,SPENT,SOURCE FROM FACEBOOK_AD_STATS WHERE "
                + "TRACKING_DATE= DATE(?) order by TRACKING_DATE, SOURCE",
                new Object[]{currentDate}, new ResultSetExtractor<List<FacebookAdsStatsData>>() {

            @Override
            public List<FacebookAdsStatsData> extractData(final ResultSet rs) throws SQLException,
                    DataAccessException {
                List<FacebookAdsStatsData> values = new ArrayList<FacebookAdsStatsData>();
                while (rs.next()) {
                    values.add(createFaceBookAdsStatsData(rs.getLong("CLICKS"), rs.getLong("IMPRESSIONS"),
                            rs.getLong("SPENT")));
                }
                return values;
            }
        });
    }
}
