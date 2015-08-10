package com.yazino.bi.operations.persistence;

import com.yazino.bi.persistence.InsertStatementBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.yazino.bi.operations.persistence.facebook.data.FacebookAdsStatsData;

import java.util.Date;
import java.util.Map;

import static com.yazino.bi.persistence.InsertStatementBuilder.*;
import static org.apache.commons.lang3.Validate.notNull;

@Repository("externalDwFacebookDataRecordingDao")
public class ExternalDwFacebookDataRecordingDao extends JdbcFacebookDataRecordingDao {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalDwFacebookDataRecordingDao.class);

    private static final String SQL_UPDATES = "UPDATE FACEBOOK_AD_STATS SET"
            + " TRACKING_DATE = stage.TRACKING_DATE, SOURCE = stage.SOURCE, CLICKS = stage.CLICKS,"
            + " IMPRESSIONS = stage.IMPRESSIONS, SPENT = stage.SPENT FROM STG_FACEBOOK_AD_STATS stage"
            + " WHERE (FACEBOOK_AD_STATS.TRACKING_DATE = stage.TRACKING_DATE AND"
            + " FACEBOOK_AD_STATS.SOURCE = stage.SOURCE)";

    private static final String INSERTS = "INSERT INTO FACEBOOK_AD_STATS"
            + " SELECT stage.* FROM STG_FACEBOOK_AD_STATS stage"
            + " LEFT JOIN FACEBOOK_AD_STATS target ON stage.TRACKING_DATE=target.TRACKING_DATE"
            + " AND stage.SOURCE = target.SOURCE "
            + " where (target.TRACKING_DATE IS NULL and target.SOURCE IS NULL)";

    private static final String SQL_CLEAN_STAGING = "DELETE FROM STG_FACEBOOK_AD_STATS";


    @Autowired
    public ExternalDwFacebookDataRecordingDao(@Qualifier("externalDwJdbcTemplate") JdbcTemplate jdbcTemplate) {
        setJdbcTemplate(jdbcTemplate);
    }

    @Override
    public void saveFacebookData(final Date givenDate, final Map<String, FacebookAdsStatsData> adGroupStats) {

        notNull(adGroupStats, "AdGroup Stats should not be null");
        if (adGroupStats.isEmpty()) {
            LOG.debug("AdGroupStatus is empty and hence not updating values");
            return;
        }
        try {

            getJdbcTemplate().batchUpdate(new String[]{
                    createInsertStatementForFaceBookAdStatus(givenDate, adGroupStats),
                    SQL_UPDATES,
                    INSERTS,
                    SQL_CLEAN_STAGING
            });

        } catch (final DataAccessResourceFailureException e) {
            LOG.warn("Cannot connect to database, returning to queue", e);
            throw e;

        } catch (final TransientDataAccessException e) {
            LOG.warn("Transient failure while writing to database, returning to queue", e);
            throw e;

        } catch (final Exception e) {
            LOG.error("Save failed for the beans: {}", adGroupStats, e);
        }

    }

    private String createInsertStatementForFaceBookAdStatus(Date givenDate, Map<String, FacebookAdsStatsData> adGroupStats) {
        InsertStatementBuilder insertStatementBuilder = new InsertStatementBuilder("STG_FACEBOOK_AD_STATS",
                "TRACKING_DATE", "SOURCE", "CLICKS", "IMPRESSIONS", "SPENT");
        for (final Map.Entry<String, FacebookAdsStatsData> entry : adGroupStats
                .entrySet()) {
            String source = entry.getKey();
            FacebookAdsStatsData data = entry.getValue();
            insertStatementBuilder = insertStatementBuilder.withValues(
                    sqlDate(givenDate),
                    sqlString(source),
                    sqlLong(data.getClicks()),
                    sqlLong(data.getImpressions()),
                    sqlLong(data.getSpent()));
        }
        return insertStatementBuilder.toSql();
    }
}
