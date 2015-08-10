package com.yazino.bi.campaign.dao;

import com.yazino.bi.campaign.domain.CampaignSchedule;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class MySqlCampaignScheduleDao implements CampaignScheduleDao {
    private static final Logger LOG = LoggerFactory.getLogger(MySqlCampaignScheduleDao.class);
    private JdbcTemplate jdbc;

    @Autowired
    public MySqlCampaignScheduleDao(@Qualifier("dwJdbcTemplate") final JdbcTemplate jdbc) {
        notNull(jdbc, "jdbcTemplate should not be null");
        this.jdbc = jdbc;
    }

    @Override
    public void updateNextRunTs(final Long campaignId, final DateTime nextRunTs) {
        LOG.debug("updating next run ts for Campaign Id {}, to {}", campaignId, nextRunTs);
        jdbc.update("UPDATE CAMPAIGN_SCHEDULE SET NEXT_RUN_TS = ? WHERE CAMPAIGN_ID=?", toTimestamp(nextRunTs), campaignId);
    }

    @Override
    public List<CampaignSchedule> getDueCampaigns(final DateTime currentTimestamp) {
        LOG.debug("fetching overdue campaigns");
        return jdbc.query(
                "SELECT CAMPAIGN_ID, NEXT_RUN_TS, RUN_HOURS, RUN_MINUTES, END_TIME from CAMPAIGN_SCHEDULE cs" +
                        " join CAMPAIGN_DEFINITION cd on cs.CAMPAIGN_ID= cd.id where NEXT_RUN_TS <= ? and (END_TIME >= ? OR END_TIME is NULL) and enabled=1",
                new RowMapper<CampaignSchedule>() {
                    @Override
                    public CampaignSchedule mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                        return new CampaignSchedule(
                                rs.getLong("CAMPAIGN_ID"),
                                toDateTime(rs.getTimestamp("NEXT_RUN_TS")),
                                rs.getLong("RUN_HOURS"),
                                rs.getLong("RUN_MINUTES"), toDateTime(rs.getTimestamp("END_TIME")));

                    }
                }, toTimestamp(currentTimestamp), toTimestamp(currentTimestamp));
    }

    @Override
    public void save(final CampaignSchedule campaignSchedule) {

        insertOrUpdateIntoCampaignSchedule(
                campaignSchedule.getCampaignId(),
                campaignSchedule.getNextRunTs(),
                campaignSchedule.getRunHours(),
                campaignSchedule.getRunMinutes(),
                campaignSchedule.getEndTime());
    }

    @Override
    public void update(final CampaignSchedule campaignSchedule) {
        insertOrUpdateIntoCampaignSchedule(
                campaignSchedule.getCampaignId(),
                campaignSchedule.getNextRunTs(),
                campaignSchedule.getRunHours(),
                campaignSchedule.getRunMinutes(),
                campaignSchedule.getEndTime());
    }

    @Override
    public CampaignSchedule getCampaignSchedule(final Long campaignId) {
        return jdbc.queryForObject("SELECT CAMPAIGN_ID, NEXT_RUN_TS, RUN_HOURS, RUN_MINUTES, END_TIME from CAMPAIGN_SCHEDULE where CAMPAIGN_ID = ?",
                new RowMapper<CampaignSchedule>() {
                    @Override
                    public CampaignSchedule mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                        return new CampaignSchedule(
                                rs.getLong("CAMPAIGN_ID"),
                                toDateTime(rs.getTimestamp("NEXT_RUN_TS")),
                                rs.getLong("RUN_HOURS"),
                                rs.getLong("RUN_MINUTES"),
                                toDateTime(rs.getTimestamp("END_TIME")));
                    }
                }, campaignId);
    }

    private Timestamp toTimestamp(final DateTime currentTimestamp) {
        if (currentTimestamp == null) {
            return null;
        }

        return new Timestamp(currentTimestamp.getMillis());
    }

    private DateTime toDateTime(Timestamp timestamp) throws SQLException {
        if (timestamp == null) {
            return null;
        }

        return new DateTime(timestamp.getTime());
    }

    private int insertOrUpdateIntoCampaignSchedule(final Long campaignId, final DateTime nextRunTs, final Long runHours, final Long runMinutes, final DateTime endTime) {
        return jdbc.update("REPLACE INTO CAMPAIGN_SCHEDULE (CAMPAIGN_ID, NEXT_RUN_TS, RUN_HOURS,RUN_MINUTES, END_TIME) VALUES (?,?,?,?,?)",
                campaignId,
                toTimestamp(nextRunTs),
                runHours,
                runMinutes,
                toTimestamp(endTime));
    }

}
