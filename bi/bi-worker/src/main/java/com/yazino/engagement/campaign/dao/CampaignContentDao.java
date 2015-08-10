package com.yazino.engagement.campaign.dao;

import org.apache.commons.lang.Validate;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

@Repository
public class CampaignContentDao {
    private static final Logger LOG = LoggerFactory.getLogger(CampaignContentDao.class);

    public static final String UPDATE_PROGRESSIVE_BONUS_FOR_SEGMENT = "UPDATE SEGMENT_SELECTION SS "
            + "set progressive_bonus = "
            + "CASE when last_played_date >= ? then 2500 + (CONSECUTIVE_PLAY_DAYS * 500) + floor(CONSECUTIVE_PLAY_DAYS/4) * 500 else 2500 end "
            + "from PLAYER_PROMOTION_STATUS pps  "
            + "where SS.campaign_run_id=?"
            + "and SS.player_id = pps.player_id ";


    private final JdbcTemplate dwJdbcTemplate;

    @Autowired
    public CampaignContentDao(final JdbcTemplate externalDwJdbcTemplate) {
        Validate.notNull(externalDwJdbcTemplate, "externalDwJdbcTemplate CanNotBeNull");
        this.dwJdbcTemplate = externalDwJdbcTemplate;
    }

    public void fillProgressiveBonusAmount(final Long campaignRunId) {
        LOG.debug("adding progressive to segment selection ");
        dwJdbcTemplate.update(UPDATE_PROGRESSIVE_BONUS_FOR_SEGMENT,
                new Timestamp(new DateTime().minusDays(1).withHourOfDay(5).withMinuteOfHour(0).withSecondOfMinute(0).getMillis()),
                (campaignRunId)
        );
    }

    public void fillDisplayName(final Long campaignRunId) {
        LOG.debug("adding filling display name for segment selection ");
        dwJdbcTemplate.update("UPDATE SEGMENT_SELECTION ss SET DISPLAY_NAME = lui.DISPLAY_NAME "
                + "from LOBBY_USER lui "
                + "where ss.player_id = lui.player_id "
                + " and ss.campaign_run_id=?", campaignRunId);
    }
}
