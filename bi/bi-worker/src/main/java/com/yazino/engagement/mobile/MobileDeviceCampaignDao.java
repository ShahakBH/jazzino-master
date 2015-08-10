package com.yazino.engagement.mobile;

import com.yazino.bi.messaging.SegmentSelectionCustomDataHelper;
import com.yazino.engagement.PlayerTarget;
import com.yazino.platform.Platform;
import com.yazino.platform.util.BigDecimals;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import static com.yazino.engagement.mobile.JdbcParams.buildParams;

@Repository
public class MobileDeviceCampaignDao {

    private static final String SELECT_PLAYERS_FOR_CAMPAIGN = "SELECT md.game_type, md.push_token, md.app_id, ss.* "
            + "FROM segment_selection ss, mobile_device md "
            + "WHERE ss.campaign_run_id = :campaignRunId "
            + "AND ss.player_id = md.player_id "
            + "AND platform = :platform "
            + "AND active = true";


    private static final String SELECT_PLAYERS_FOR_CAMPAIGN_WITH_FROM_TO = SELECT_PLAYERS_FOR_CAMPAIGN
            + " AND ss.valid_from > :from AND ss.valid_from <= :to";

    private NamedParameterJdbcTemplate externalDwNamedJdbcTemplate;

    private MobileDeviceCampaignDao() {
    }

    @Autowired
    public MobileDeviceCampaignDao(final NamedParameterJdbcTemplate externalDwNamedJdbcTemplate) {
        this.externalDwNamedJdbcTemplate = externalDwNamedJdbcTemplate;
    }

    public List<PlayerTarget> getEligiblePlayerTargets(final Long campaignRunId, Platform platform,
                                                       final Long from, final Long to) {
        final String queryString;
        Map<String, Object> params = buildParams(
                "campaignRunId", campaignRunId,
                "platform", platform.name());

        if (from != null && to != null) {
            params.put("from", new Timestamp(from));
            params.put("to", new Timestamp(to));
            queryString = SELECT_PLAYERS_FOR_CAMPAIGN_WITH_FROM_TO;
        } else {
            queryString = SELECT_PLAYERS_FOR_CAMPAIGN;
        }

        return externalDwNamedJdbcTemplate.query(
                queryString, params, new RowMapper<PlayerTarget>() {
                    @Override
                    public PlayerTarget mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return new PlayerTarget(
                                rs.getString("GAME_TYPE"),
                                "", // no externalId
                                BigDecimals.strip(rs.getBigDecimal("PLAYER_ID")),
                                rs.getString("PUSH_TOKEN"),
                                rs.getString("APP_ID"),
                                SegmentSelectionCustomDataHelper.getCustomData(rs));
                    }
                });
    }

}
