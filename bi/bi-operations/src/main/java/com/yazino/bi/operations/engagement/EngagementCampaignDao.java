package com.yazino.bi.operations.engagement;

import com.yazino.engagement.ChannelType;
import com.yazino.engagement.EngagementCampaignStatus;
import com.yazino.platform.util.BigDecimals;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class EngagementCampaignDao {

    private final JdbcTemplate jdbcTemplate;

    // for cglib
    public EngagementCampaignDao() {
        jdbcTemplate = null;
    }

    @Autowired(required = true)
    public EngagementCampaignDao(@Qualifier("dwJdbcTemplate") final JdbcTemplate jdbcTemplate) {
        notNull(jdbcTemplate, "jdbcTemplate was null");
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String SELECT_ALL_APP_REQUEST_COLUMNS = "select ID, CHANNEL_TYPE, TITLE, DESCRIPTION, "
            + "MESSAGE, TRACKING, CREATED, SENT, TARGET_COUNT, STATUS, SCHEDULED_DT, EXPIRY_DT from APP_REQUEST";

    public static final String FIND_DUE_REQUESTS = SELECT_ALL_APP_REQUEST_COLUMNS + " where STATUS = "
            + EngagementCampaignStatus.CREATED.getValue() + " and SCHEDULED_DT <= ?";

    private static final String FIND_BY_ID_SQL = SELECT_ALL_APP_REQUEST_COLUMNS + " where ID = ?";

    private static final String FIND_ALL_SQL = SELECT_ALL_APP_REQUEST_COLUMNS + " ORDER BY CREATED DESC";

    private static final String FIND_ALL_BY_CHANNEL_TYPE_SQL = SELECT_ALL_APP_REQUEST_COLUMNS
            + " where CHANNEL_TYPE = ? ORDER BY CREATED DESC";

    private static final String FIND_ALL_BY_STATUS_SQL = SELECT_ALL_APP_REQUEST_COLUMNS
            + " where CHANNEL_TYPE = ? and STATUS = ?";

    private static final String UPDATE_SQL =
            "UPDATE APP_REQUEST SET TITLE = ?, DESCRIPTION = ?, MESSAGE = ?, TRACKING = ?, SENT = ?, "
                    + "STATUS = ?, SCHEDULED_DT = ?, EXPIRY_DT = ? where ID = ?";

    private static final String DELETE_SQL = "DELETE FROM APP_REQUEST where ID = ?";

    private static final String DELETE_TARGETS_SQL = "DELETE FROM APP_REQUEST_TARGET where APP_REQUEST_ID = ?";

    private static final String TARGETS_BY_ID_RANGE_SQL
            = "SELECT ID, APP_REQUEST_ID, GAME_TYPE, PLAYER_ID, EXTERNAL_ID from "
            + "APP_REQUEST_TARGET where APP_REQUEST_ID = ? ORDER BY GAME_TYPE, PLAYER_ID LIMIT ?,?";

    private static final String INSERT_TARGET_SQL =
            "INSERT IGNORE into APP_REQUEST_TARGET(APP_REQUEST_ID, GAME_TYPE, PLAYER_ID, EXTERNAL_ID) values(?,?,?,?)";

    private static final String COUNT_TARGETS_BY_ID_SQL =
            "SELECT COUNT(1) from APP_REQUEST_TARGET where APP_REQUEST_ID = ? ";

    private static final String UPDATE_MESSAGE_COUNT_SQL =
            "UPDATE APP_REQUEST SET TARGET_COUNT = ? where ID = ?";

    public EngagementCampaign findById(final Integer id) {
        try {
            return jdbcTemplate.queryForObject(FIND_BY_ID_SQL,
                    new Object[]{id},
                    new AppRequestRowMapper());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<EngagementCampaign> findAll() {
        return jdbcTemplate.query(FIND_ALL_SQL, new AppRequestRowMapper());
    }

    public List<EngagementCampaign> findAll(final ChannelType channelType) {
        return jdbcTemplate.query(FIND_ALL_BY_CHANNEL_TYPE_SQL, new AppRequestRowMapper(), channelType.name());
    }

    public List<EngagementCampaign> findAllByStatus(final ChannelType channelType,
                                                    final EngagementCampaignStatus status) {
        return jdbcTemplate.query(FIND_ALL_BY_STATUS_SQL,
                new AppRequestRowMapper(), channelType.name(), status.getValue()
        );
    }

    public int create(final EngagementCampaign engagementCampaign) {
        final DateTime created = new DateTime();
        SimpleJdbcInsert insertSpec =
                new SimpleJdbcInsert(jdbcTemplate)
                        .withTableName("APP_REQUEST")
                        .usingGeneratedKeyColumns("ID");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("CHANNEL_TYPE", engagementCampaign.getChannelType().name());
        parameters.put("TITLE", engagementCampaign.getTitle());
        parameters.put("DESCRIPTION", engagementCampaign.getDescription());
        parameters.put("MESSAGE", engagementCampaign.getMessage());
        parameters.put("TRACKING", engagementCampaign.getTrackingReference());
        parameters.put("CREATED", created.toDate());
        parameters.put("TARGET_COUNT", 0);
        parameters.put("STATUS", engagementCampaign.getStatus().getValue());
        parameters.put("SCHEDULED_DT", convertDateTimeToTimestamp(engagementCampaign.getScheduled()));
        parameters.put("EXPIRY_DT", convertDateTimeToTimestamp(engagementCampaign.getExpires()));

        Number id = insertSpec.executeAndReturnKey(parameters);
        return id.intValue();
    }

    public void update(final EngagementCampaign engagementCampaign) {
        jdbcTemplate.update(UPDATE_SQL,
                engagementCampaign.getTitle(),
                engagementCampaign.getDescription(),
                engagementCampaign.getMessage(),
                engagementCampaign.getTrackingReference(),
                convertDateTimeToTimestamp(engagementCampaign.getSent()),
                engagementCampaign.getStatus().getValue(),
                convertDateTimeToTimestamp(engagementCampaign.getScheduled()),
                convertDateTimeToTimestamp(engagementCampaign.getExpires()),
                engagementCampaign.getId()
        );
    }

    @Transactional("legacyDwTransactionManager")
    public void delete(final Integer id) {
        jdbcTemplate.update(DELETE_TARGETS_SQL, id);
        jdbcTemplate.update(DELETE_SQL, id);
    }

    /**
     * Fetch targets of a request. Note that query orders by game type and player id.
     *
     * @param appRequestId    app request id
     * @param rowOffset       offset of first row to return (zero based)
     * @param numberOfTargets max number of targets to return
     * @return targets of request. Empty list if request has no targets.
     */
    public List<AppRequestTarget> findAppRequestTargetsById(final Integer appRequestId,
                                                            final Integer rowOffset,
                                                            final Integer numberOfTargets) {
        return jdbcTemplate.query(TARGETS_BY_ID_RANGE_SQL,
                new AppRequestTargetRowMapper(),
                appRequestId, rowOffset, numberOfTargets);
    }

    @Transactional("legacyDwTransactionManager")
    public void addAppRequestTargets(final Integer appRequestId,
                                     final List<AppRequestTarget> targets) {
        jdbcTemplate.batchUpdate(
                INSERT_TARGET_SQL,
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(final PreparedStatement ps, final int i) throws SQLException {
                        ps.setInt(1, appRequestId);
                        final AppRequestTarget appRequestTarget = targets.get(i);
                        ps.setString(2, appRequestTarget.getGameType());
                        ps.setBigDecimal(3, appRequestTarget.getPlayerId());
                        ps.setString(4, appRequestTarget.getExternalId());
                    }

                    @Override
                    public int getBatchSize() {
                        return targets.size();
                    }
                });
        final int targetCount = getTargetCountById(appRequestId);
        updateMessageCount(targetCount, appRequestId);
    }

    public int getTargetCountById(final Integer appRequestId) {
        return jdbcTemplate.queryForObject(COUNT_TARGETS_BY_ID_SQL, Integer.class, appRequestId);
    }

    private void updateMessageCount(final Integer messageCount, final Integer appRequestId) {
        jdbcTemplate.update(UPDATE_MESSAGE_COUNT_SQL, messageCount, appRequestId);
    }

    private Timestamp convertDateTimeToTimestamp(final DateTime dateTime) {
        Timestamp timestamp = null;
        if (dateTime != null) {
            timestamp = new Timestamp(dateTime.getMillis());
        }
        return timestamp;
    }

    /**
     * Return List of scheduled app request that are dueDate due to be sent.
     *
     * @param dueDate date after which app requests are due to be sent
     * @return list of AppRequests to send
     */
    public List<EngagementCampaign> findDueEngagementCampaigns(final DateTime dueDate) {
        return jdbcTemplate.query(FIND_DUE_REQUESTS, new AppRequestRowMapper(), dueDate.toDate());
    }

    public void associateMarketingGroupWithCampaign(long marketingGroupId, int engagementCampaignId) {
        jdbcTemplate.update("call associate_marketing_group_with_app_request(?,?)", marketingGroupId, engagementCampaignId);
    }


    private static class AppRequestRowMapper implements RowMapper<EngagementCampaign> {
        @Override
        public EngagementCampaign mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final EngagementCampaignBuilder engagementCampaignBuilder = new EngagementCampaignBuilder()
                    .withId(rs.getInt("ID"))
                    .withChannelType(ChannelType.valueOf(rs.getString("CHANNEL_TYPE")))
                    .withTitle(rs.getString("TITLE"))
                    .withDescription(rs.getString("DESCRIPTION"))
                    .withMessage(rs.getString("MESSAGE"))
                    .withTrackingReference(rs.getString("TRACKING"))
                    .withCreateDate(new DateTime(rs.getTimestamp("CREATED")))
                    .withStatus(EngagementCampaignStatus.ofStatusCode(rs.getInt("STATUS")))
                    .withTargetCount(rs.getInt("TARGET_COUNT"))
                    .withSentDate(getNullableDateTime(rs, "SENT"))
                    .withScheduled(getNullableDateTime(rs, "SCHEDULED_DT"))
                    .withExpires(getNullableDateTime(rs, "EXPIRY_DT"));
            return engagementCampaignBuilder.build();
        }

        private DateTime getNullableDateTime(final ResultSet rs, final String column) throws SQLException {
            final Timestamp timestamp = rs.getTimestamp(column);
            if (timestamp != null) {
                return new DateTime(timestamp);
            }
            return null;
        }

    }

    private static class AppRequestTargetRowMapper implements RowMapper<AppRequestTarget> {
        @Override
        public AppRequestTarget mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            return new AppRequestTargetBuilder()
                    .withId(rs.getInt("ID"))
                    .withAppRequestId(rs.getInt("APP_REQUEST_ID"))
                    .withGameType(rs.getString("GAME_TYPE"))
                    .withPlayerId(BigDecimals.strip(rs.getBigDecimal("PLAYER_ID")))
                    .withExternalId(rs.getString("EXTERNAL_ID"))
                    .build();
        }
    }
}
