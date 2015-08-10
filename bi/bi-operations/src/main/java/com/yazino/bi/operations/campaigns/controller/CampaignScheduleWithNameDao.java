package com.yazino.bi.operations.campaigns.controller;

import com.yazino.bi.campaign.dao.MySqlCampaignScheduleDao;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class CampaignScheduleWithNameDao extends MySqlCampaignScheduleDao {
    public static final int DEFAULT_OFFSET = 0;
    public static final int DEFAULT_LIMIT = 1000;
    private final JdbcTemplate template;

    @Autowired
    public CampaignScheduleWithNameDao(@Qualifier("dwJdbcTemplate") final JdbcTemplate template) {
        super(template);
        this.template = template;
    }

    public List<CampaignScheduleWithName> getCampaignList(boolean includeDisabled) {
        return getCampaignList(DEFAULT_OFFSET, DEFAULT_LIMIT, includeDisabled);
    }

    private DateTime getNullDateTimeOrDateTime(final Timestamp timeStamp) throws SQLException {
        if (timeStamp == null) {
            return null;
        }
        return new DateTime(timeStamp);
    }

    public Integer getCampaignRecordCount() {
        return template.queryForInt("SELECT count(*) FROM CAMPAIGN_DEFINITION");
    }


    public List<CampaignScheduleWithName> getCampaignList(final int offset, final int limit, boolean includeDisabled) {
        final String sql = "SELECT ID, name, NEXT_RUN_TS, END_TIME, RUN_HOURS, RUN_MINUTES FROM CAMPAIGN_DEFINITION cd JOIN CAMPAIGN_SCHEDULE cs "
                + "ON cd.id = cs.campaign_id "
                + includeDisabled(includeDisabled)
                + "ORDER BY NEXT_RUN_TS DESC, ID LIMIT ? OFFSET ?";
        return template.query(sql
                , getCampaignScheduleWithNameRowMapper(), limit, offset);
    }

    private String includeDisabled(final boolean includeDisabled) {
        if (!includeDisabled) {
            return "WHERE enabled=true ";
        } else {
            return "";
        }
    }

    public CampaignScheduleWithName getCampaignScheduleWithName(final Long campaignId) {
        return template.queryForObject(
                "SELECT ID, name, NEXT_RUN_TS, END_TIME, RUN_HOURS, RUN_MINUTES FROM CAMPAIGN_DEFINITION cd JOIN CAMPAIGN_SCHEDULE cs "
                        + "ON cd.id = cs.campaign_id WHERE campaign_id = ?",
                getCampaignScheduleWithNameRowMapper(),
                campaignId);
    }

    private RowMapper<CampaignScheduleWithName> getCampaignScheduleWithNameRowMapper() {
        return new RowMapper<CampaignScheduleWithName>() {
            @Override
            public CampaignScheduleWithName mapRow(final ResultSet rs, final int rowNum) throws SQLException {

                return new CampaignScheduleWithName(rs.getLong("ID"),
                        rs.getString("name"),
                        getNullDateTimeOrDateTime(rs.getTimestamp("next_run_ts")),
                        getNullDateTimeOrDateTime(rs.getTimestamp("end_Time")),
                        rs.getLong("RUN_HOURS"),
                        rs.getLong("RUN_MINUTES"));
            }
        };
    }
}
