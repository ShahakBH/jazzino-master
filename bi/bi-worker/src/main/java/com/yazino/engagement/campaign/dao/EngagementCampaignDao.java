package com.yazino.engagement.campaign.dao;


import com.yazino.engagement.ChannelType;
import com.yazino.engagement.EngagementCampaignStatus;
import com.yazino.engagement.campaign.AppRequestExternalReference;
import com.yazino.engagement.facebook.FacebookAppRequestEnvelope;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class EngagementCampaignDao {
    private static final Logger LOG = LoggerFactory.getLogger(EngagementCampaignDao.class);

    public static final String APP_REQUEST_MESSAGE_SQL = "SELECT title, description, message, game_type, tracking, external_id, expiry_dt"
            + " from APP_REQUEST ar JOIN APP_REQUEST_TARGET art ON ar.id = art.app_request_id"
            + " where art.id = ? and ar.id = ?";
    public static final String SAVE_EXTERNAL_REF = "UPDATE APP_REQUEST_TARGET SET EXTERNAL_REF=? WHERE ID=?";
    public static final String EXTERNAL_REF_SQL = "SELECT EXTERNAL_ID, GAME_TYPE, EXTERNAL_REF "
            + "FROM APP_REQUEST_TARGET WHERE APP_REQUEST_ID=? AND EXTERNAL_REF IS NOT NULL";
    public static final String SELECT_EXPIRED_APP_REQUEST;

    static {
        final StringBuilder sql = new StringBuilder();
        sql.append("SELECT ID FROM APP_REQUEST WHERE EXPIRY_DT <= Now() and status = 2 and ");

        sql.append("CHANNEL_TYPE in (");
        int deleteTypeCount = 0;
        for (ChannelType type : ChannelType.values()) {
            if (type.canDeleteRequests()) {
                if (deleteTypeCount > 0) {
                    sql.append(',');
                }
                sql.append('\'').append(type.name()).append('\'');
                deleteTypeCount++;
            }
        }
        sql.append(")");
        SELECT_EXPIRED_APP_REQUEST = sql.toString();
    }

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public EngagementCampaignDao(@Qualifier("dwJdbcTemplate") final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Fetch the app request message for given target and request. Only one should be returned since we're querying by
     * primary keys. All non connection/transient db exceptions are caught and logged.
     *
     * @param campaignId the request id
     * @param targetId   target id
     * @return the app request or null if none found.
     */
    public FacebookAppRequestEnvelope fetchAppRequestEnvelopeByCampaignAndTargetId(final int campaignId,
                                                                                   final Integer targetId) {
        try {
            return jdbcTemplate.queryForObject(APP_REQUEST_MESSAGE_SQL, new RowMapper<FacebookAppRequestEnvelope>() {
                @Override
                public FacebookAppRequestEnvelope mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                    DateTime expiry = asDateTime(rs.getDate("expiry_dt"));
                    return new FacebookAppRequestEnvelope(
                            rs.getString("title"), rs.getString("description"),
                            rs.getString("external_id"),
                            rs.getString("game_type"),
                            rs.getString("message"),
                            rs.getString("tracking"),
                            expiry);
                }
            }, targetId, campaignId);
        } catch (final DataAccessResourceFailureException e) {
            LOG.warn("Cannot connect to database, returning to queue", e);
            throw e;
        } catch (final TransientDataAccessException e) {
            LOG.warn("Transient failure while reading from database, returning to queue", e);
            throw e;
        } catch (EmptyResultDataAccessException e) {
            LOG.debug("Target not found for campaignId={}, targetId={}", new Object[]{campaignId, targetId, e});
        } catch (final Exception e) {
            LOG.error("Read failed for campaignId={}, targetId={}", new Object[]{campaignId, targetId, e});
        }
        return null;
    }

    private DateTime asDateTime(Date date) {
        if (date != null) {
            return new DateTime(date);
        } else {
            return null;
        }
    }

    /**
     * Updates the external reference to this target (e.g. Facebook's request ID)
     *
     * @param targetId          target id
     * @param externalReference the reference to be saved
     */
    public void saveExternalReference(final Integer targetId, final String externalReference) {
        jdbcTemplate.update(SAVE_EXTERNAL_REF, externalReference, targetId);
    }

    public List<AppRequestExternalReference> fetchAppRequestExternalReferences(final int campaignId) {
        return jdbcTemplate.query(EXTERNAL_REF_SQL, new RowMapper<AppRequestExternalReference>() {
            @Override
            public AppRequestExternalReference mapRow(final ResultSet resultSet, final int i) throws SQLException {
                return new AppRequestExternalReference(resultSet.getString("EXTERNAL_ID"),
                        resultSet.getString("GAME_TYPE"),
                        resultSet.getString("EXTERNAL_REF"));
            }
        }, campaignId);
    }

    public List<Integer> fetchCampaignsToExpire() {
        return jdbcTemplate.query(SELECT_EXPIRED_APP_REQUEST, new RowMapper<Integer>() {
            @Override
            public Integer mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                return rs.getInt("ID");
            }
        });
    }


    public void updateCampaignStatus(final Integer id, final EngagementCampaignStatus status) {
        jdbcTemplate.update("UPDATE APP_REQUEST SET STATUS=? WHERE ID=?", status.getValue(), id);
    }

    /**
     * Flags a campaign as expiring.
     *
     * @param campaignId campaign to change,
     * @return true if status was updated, false if campaign status couldn't be updated.
     *         This will be because, either another process has started expiring the campaign or the current status
     *         is not SENT
     */
    public boolean updateCampaignStatusToExpiring(final Integer campaignId) {
        final int updated = jdbcTemplate.update(
                "update APP_REQUEST set status=? where id=? and expiry_dt < now() and status = ?",
                EngagementCampaignStatus.EXPIRING.getValue(),
                campaignId,
                EngagementCampaignStatus.SENT.getValue());
        return updated > 0;
    }
}
