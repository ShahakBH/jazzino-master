package com.yazino.bi.operations.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import com.yazino.bi.operations.persistence.facebook.data.FacebookAdsStatsData;

import java.util.Date;
import java.util.Map;

public abstract class JdbcFacebookDataRecordingDao implements FacebookDataRecordingDao {

    private JdbcTemplate jdbcTemplate;

    @Override
    public abstract void saveFacebookData(final Date now,
                                          final Map<String, FacebookAdsStatsData> adGroupStats);

    @Override
    public Date getLatestRecordDate() {
        return jdbcTemplate.queryForObject(
                "SELECT MAX(TRACKING_DATE) FROM FACEBOOK_AD_STATS", Date.class);
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }
}
