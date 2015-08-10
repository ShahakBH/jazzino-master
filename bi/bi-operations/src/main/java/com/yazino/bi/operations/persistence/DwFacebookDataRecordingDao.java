package com.yazino.bi.operations.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.yazino.bi.operations.persistence.facebook.data.FacebookAdsStatsData;

import java.util.Date;
import java.util.Map;

@Repository("dwFacebookDataRecordingDao")
public class DwFacebookDataRecordingDao extends JdbcFacebookDataRecordingDao {

    @Autowired
    public DwFacebookDataRecordingDao(@Qualifier("dwJdbcTemplate") JdbcTemplate jdbcTemplate) {
        setJdbcTemplate(jdbcTemplate);
    }

    @Override
    public void saveFacebookData(final Date now,
                                 final Map<String, FacebookAdsStatsData> adGroupStats) {
        JdbcTemplate jdbcTemplate = getJdbcTemplate();

        for (final Map.Entry<String, FacebookAdsStatsData> entry : adGroupStats
                .entrySet()) {
            String source = entry.getKey();
            FacebookAdsStatsData data = entry.getValue();
            jdbcTemplate.update("REPLACE INTO FACEBOOK_AD_STATS(TRACKING_DATE,CLICKS,IMPRESSIONS,SPENT,SOURCE) "
                    + "VALUES(DATE(?),?,?,?,?)", now, data.getClicks(),
                    data.getImpressions(), data.getSpent(), source);
        }
    }
}
