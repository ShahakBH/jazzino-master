package com.yazino.bi.campaign.dao;

import com.yazino.bi.campaign.domain.CampaignDefinition;
import com.yazino.engagement.ChannelType;
import com.yazino.engagement.campaign.domain.NotificationChannelConfigType;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

@Repository
public class MySqlCampaignDefinitionDao implements CampaignDefinitionDao {
    private static final Logger LOG = LoggerFactory.getLogger(MySqlCampaignDefinitionDao.class);
    public static final String INSERT_CAMPAIGN_DEFINITION =
            "INSERT INTO CAMPAIGN_DEFINITION (name, segmentSqlQuery, hasPromo, enabled, delay_notifications) VALUES (?,?,?,?,?)";
    public static final String UPDATE_REPLACE_CAMPAIGN_DEFINITION =
            "UPDATE CAMPAIGN_DEFINITION SET name=?, segmentSqlQuery=?, hasPromo=?, enabled=?, delay_notifications=? WHERE ID = ?";
    public static final String INSERT_CAMPAIGN_CONTENT =
            "REPLACE INTO CAMPAIGN_CONTENT (CAMPAIGN_ID, CONTENT_KEY, CONTENT_VALUE) VALUES (?,?,?)";
    private static final String INSERT_CAMPAIGN_CHANNEL_CONFIG =
            "insert into CAMPAIGN_CHANNEL_CONFIG (CAMPAIGN_ID, CONFIG_KEY, CONFIG_VALUE) VALUES(?,?,?)";
    private JdbcTemplate jdbc;

    @Autowired
    public MySqlCampaignDefinitionDao(@Qualifier("dwJdbcTemplate") final JdbcTemplate jdbc) {
        Validate.notNull(jdbc, "jdbcTemplate should not be null");
        this.jdbc = jdbc;
    }

    @Override
    public CampaignDefinition fetchCampaign(final Long campaignId) {
        CampaignDefinition campaignDefinition = null;

        final Map<String, String> contentMap = getContent(campaignId);
        final Map<NotificationChannelConfigType, String> channelConfig = getChannelConfig(campaignId);

        try {
            campaignDefinition = getCampaignDefinition(campaignId, contentMap, channelConfig);
            LOG.debug("Campaign Id {} retrieved campaign: {}", campaignId, campaignDefinition);
        } catch (DataAccessException e) {
            LOG.error("No campaign found for Campaign ID {}", campaignId);
        }

        return campaignDefinition;
    }

    @Override
    public Map<NotificationChannelConfigType, String> getChannelConfig(final Long campaignId) {
        return jdbc.query("SELECT CONFIG_KEY, CONFIG_VALUE FROM CAMPAIGN_CHANNEL_CONFIG WHERE CAMPAIGN_ID = ?",
                new ResultSetExtractor<HashMap<NotificationChannelConfigType, String>>() {
                    @Override
                    public HashMap<NotificationChannelConfigType, String> extractData(final ResultSet rs) throws SQLException, DataAccessException {
                        final HashMap<NotificationChannelConfigType, String> channelConfig = new HashMap<>();
                        while (rs.next()) {
                            channelConfig.put(NotificationChannelConfigType.valueOf(rs.getString(1)), rs.getString(2));
                        }
                        return channelConfig;
                    }
                }, campaignId);
    }

    @Override
    public Map<String, String> getContent(final Long campaignRunId) {
        return jdbc.query("SELECT CONTENT_KEY, CONTENT_VALUE FROM CAMPAIGN_CONTENT WHERE CAMPAIGN_ID = ?",
                new ResultSetExtractor<HashMap<String, String>>() {
                    @Override
                    public HashMap<String, String> extractData(final ResultSet rs) throws SQLException, DataAccessException {
                        final HashMap<String, String> contentMap = new HashMap<>();
                        while (rs.next()) {
                            contentMap.put(rs.getString(1), rs.getString(2));
                        }
                        return contentMap;
                    }
                }, campaignRunId);
    }

    @Override
    public void update(final CampaignDefinition campaignDefinition) {
        updateCampaignDefinition(
                campaignDefinition.getId(),
                campaignDefinition.getName(),
                campaignDefinition.getSegmentSelectionQuery(),
                campaignDefinition.hasPromo(),
                campaignDefinition.isEnabled(),
                campaignDefinition.delayNotifications());

        updateCampaignContent(campaignDefinition, campaignDefinition.getId());
        updateCampaignChannels(campaignDefinition.getId(), campaignDefinition.getChannels());
        updateCampaignChannelConfig(campaignDefinition.getId(), campaignDefinition.getChannelConfig());
    }

    @Override
    public void setEnabledStatus(final Long campaignId, boolean enabled) {
        jdbc.execute(format("update CAMPAIGN_DEFINITION set enabled=%s where id=%s", Boolean.toString(enabled), campaignId));
    }

    @Override
    public Long save(final CampaignDefinition campaignDefinition) {
        LOG.debug("saving campaignDefinition {}", campaignDefinition);

        final long campaignId = insertCampaignDefinition(campaignDefinition.getName(), campaignDefinition.getSegmentSelectionQuery(),
                campaignDefinition.hasPromo(), campaignDefinition.isEnabled(), campaignDefinition.delayNotifications());
        insertCampaignContent(campaignDefinition, campaignId);
        insertCampaignChannels(campaignId, campaignDefinition.getChannels());
        insertCampaignChannelConfig(campaignId, campaignDefinition.getChannelConfig());

        return campaignId;
    }

    private CampaignDefinition getCampaignDefinition(final Long campaignId,
                                                     final Map<String, String> contentMap,
                                                     final Map<NotificationChannelConfigType, String> channelConfig) {

        final List<ChannelType> channelTypes = getChannelTypes(campaignId);

        return jdbc.queryForObject(
                "SELECT ID, NAME, SEGMENTSQLQUERY as QUERY, hasPromo, enabled, delay_notifications " +
                        "FROM CAMPAIGN_DEFINITION WHERE ID = ?",
                new RowMapper<CampaignDefinition>() {
                    @Override
                    public CampaignDefinition mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                        return new CampaignDefinition(
                                rs.getLong("ID"),
                                rs.getString("NAME"),
                                rs.getString("QUERY"),
                                contentMap,
                                channelTypes,
                                rs.getBoolean("hasPromo"),
                                channelConfig,
                                rs.getBoolean("enabled"),
                                rs.getBoolean("delay_notifications"));
                    }
                }, campaignId);
    }

    public List<ChannelType> getChannelTypes(Long campaignId) {
        List<ChannelType> channelTypes;
        try {
            channelTypes = jdbc.query("select CHANNEL_NAME from CAMPAIGN_CHANNEL cc join CHANNEL_TYPE ct on cc.channel_id = ct.id "
                    + " where cc.campaign_id=?", new RowMapper<ChannelType>() {
                @Override
                public ChannelType mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                    return ChannelType.valueOf(rs.getString("CHANNEL_NAME"));
                }
            }, campaignId);
        } catch (EmptyResultDataAccessException e) {
            channelTypes = new ArrayList<>();
        }
        return channelTypes;
    }

    private Long insertCampaignDefinition(final String campaignName,
                                          final String segmentSelectionQuery,
                                          final Boolean hasPromo,
                                          final boolean enabled,
                                          final boolean delayNotifications) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(final Connection con) throws SQLException {
                PreparedStatement ps = con.prepareStatement(
                        INSERT_CAMPAIGN_DEFINITION,
                        Statement.RETURN_GENERATED_KEYS);

                int parameterIndex = 1;
                ps.setString(parameterIndex, campaignName);
                ps.setString(++parameterIndex, segmentSelectionQuery);
                ps.setBoolean(++parameterIndex, hasPromo);
                ps.setBoolean(++parameterIndex, enabled);
                ps.setBoolean(++parameterIndex, delayNotifications);
                return ps;
            }
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    private void updateCampaignDefinition(final Long id,
                                          final String campaignName,
                                          final String segmentSelectionQuery,
                                          final Boolean hasPromo,
                                          final boolean enabled,
                                          final boolean delayNotifications) {
        jdbc.update(UPDATE_REPLACE_CAMPAIGN_DEFINITION, campaignName, segmentSelectionQuery, hasPromo, enabled,
                delayNotifications, id);
    }

    private void insertCampaignChannelConfig(final long campaignId, final Map<NotificationChannelConfigType, String> channelConfig) {
        if (channelConfig != null) {
            for (Map.Entry<NotificationChannelConfigType, String> entry : channelConfig.entrySet()) {
                jdbc.update(INSERT_CAMPAIGN_CHANNEL_CONFIG, campaignId, entry.getKey().name(), entry.getValue());
            }
        }
    }


    private void insertCampaignContent(final CampaignDefinition campaignDefinition, Long campaignId) {
        for (Map.Entry<String, String> entry : campaignDefinition.getContent().entrySet()) {
            jdbc.update(INSERT_CAMPAIGN_CONTENT, campaignId, entry.getKey(), entry.getValue());
        }
    }

    private void updateCampaignContent(final CampaignDefinition campaignDefinition, Long campaignId) {
        insertCampaignContent(campaignDefinition, campaignId);
    }

    private void insertCampaignChannels(final Long campaignId, final List<ChannelType> channels) {
        if (channels != null && !(channels.isEmpty())) {
            for (ChannelType channel : channels) {
                jdbc.update(
                        "INSERT INTO CAMPAIGN_CHANNEL(CAMPAIGN_ID, CHANNEL_ID) VALUES (?,(SELECT ID FROM CHANNEL_TYPE WHERE CHANNEL_NAME=?))",
                        campaignId,
                        channel.toString());
            }
        }
    }

    private void updateCampaignChannels(final Long campaignId, final List<ChannelType> channels) {
        jdbc.update("DELETE FROM CAMPAIGN_CHANNEL WHERE CAMPAIGN_ID = ?", campaignId);
        insertCampaignChannels(campaignId, channels);
    }

    private void updateCampaignChannelConfig(final Long campaignId, final Map<NotificationChannelConfigType, String> channelConfig) {
        jdbc.update("DELETE FROM CAMPAIGN_CHANNEL_CONFIG WHERE CAMPAIGN_ID = ?", campaignId);
        insertCampaignChannelConfig(campaignId, channelConfig);
    }
}
