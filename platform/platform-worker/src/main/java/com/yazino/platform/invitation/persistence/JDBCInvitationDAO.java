package com.yazino.platform.invitation.persistence;

import com.yazino.platform.invitation.InvitationSource;
import com.yazino.platform.invitation.InvitationStatus;
import com.yazino.platform.util.BigDecimals;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Collection;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Warning. This implementation does not address the mismatch in the precision of timestamps at the application
 * level (milliseconds) and the database layer (seconds when using MySQL).  The milliseconds portion of
 * timestamps persisted in that scenario will be discarded when saving/reloading.
 */
@Repository("invitationDao")
public class JDBCInvitationDAO {
    private static final Logger LOG = LoggerFactory.getLogger(JDBCInvitationDAO.class);
    private static final Logger FALLBACK_LOG = LoggerFactory.getLogger("strata.datawarehouse.fallback");

    static final String SQL_INSERT_UPDATE =
            "INSERT INTO INVITATIONS "
                    + "(PLAYER_ID,RECIPIENT_IDENTIFIER,INVITED_FROM,"
                    + "STATUS,REWARD,CREATED_TS,UPDATED_TS,GAME_TYPE,SCREEN_SOURCE) "
                    + "VALUES (?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE "
                    + "STATUS=VALUES(STATUS), REWARD=VALUES(REWARD), UPDATED_TS=VALUES(UPDATED_TS)";

    public static final String SELECT_FROM_INVITATIONS = "SELECT * FROM INVITATIONS";

    private static final String SELECT_INVITATIONS_BY_RECIPIENT_AND_SOURCE =
            SELECT_FROM_INVITATIONS + " WHERE RECIPIENT_IDENTIFIER=? AND INVITED_FROM=?";

    private static final String SELECT_INVITATIONS_BY_ISSUING_PLAYER =
            SELECT_FROM_INVITATIONS + " WHERE PLAYER_ID = ?";

    private static final String SQL_NUMBER_OF_ACCEPTED_INVITES
            = "SELECT COUNT(*) FROM INVITATIONS WHERE STATUS='ACCEPTED' AND PLAYER_ID=? AND CREATED_TS >= ?";

    public static final String SELECT_INVITATIONS_BY_ISSUING_PLAYER_RECIPIENT_AND_SOURCE =
            SELECT_FROM_INVITATIONS + " WHERE PLAYER_ID = ? AND RECIPIENT_IDENTIFIER = ? AND INVITED_FROM = ?";


    private final JdbcTemplate jdbcTemplate;
    private final InvitationMapper invitationMapper = new InvitationMapper();

    @Autowired(required = true)
    public JDBCInvitationDAO(@Qualifier(value = "jdbcTemplate") final JdbcTemplate jdbcTemplate) {
        notNull(jdbcTemplate, "jdbcTemplate may not be null");

        this.jdbcTemplate = jdbcTemplate;
    }

    private Timestamp asTimestamp(final DateTime dateTime) {
        return new Timestamp(dateTime.getMillis());
    }

    public void save(final Invitation invitation) {
        notNull(invitation, "invitation is null");

        if (LOG.isDebugEnabled()) {
            LOG.debug("Saving invitation : " + invitation);
        }

        try {
            jdbcTemplate.update(SQL_INSERT_UPDATE, new PreparedStatementSetter() {
                @Override
                public void setValues(final PreparedStatement stmt) throws SQLException {
                    int idx = 0;
                    stmt.setBigDecimal(++idx, invitation.getIssuingPlayerId());
                    stmt.setString(++idx, invitation.getRecipientIdentifier());
                    stmt.setString(++idx, invitation.getSource().name());
                    stmt.setString(++idx, invitation.getStatus().name());
                    if (invitation.getRewardAmount() == null) {
                        stmt.setNull(++idx, Types.NUMERIC);
                    } else {
                        stmt.setLong(++idx, invitation.getRewardAmount());
                    }
                    stmt.setTimestamp(++idx, new Timestamp(invitation.getCreateTime().getMillis()));
                    stmt.setTimestamp(++idx, new Timestamp(invitation.getUpdateTime().getMillis()));
                    stmt.setString(++idx, invitation.getGameType());
                    stmt.setString(++idx, invitation.getScreenSource());
                }
            });

        } catch (final Exception e) {
            LOG.error(String.format("Failed to save invitation: %s", invitation), e);
            logCreateToFallbackLog(invitation);
        }
    }

    private void logCreateToFallbackLog(final Invitation invitation) {
        String sql = null;
        try {
            final PreparedStatementFormatter formatter = new PreparedStatementFormatter();
            sql =
                    formatter.format(SQL_INSERT_UPDATE + ";", invitation.getIssuingPlayerId(), invitation
                            .getRecipientIdentifier(), invitation.getSource().name(), invitation.getStatus().name(),
                            invitation.getRewardAmount(), invitation.getCreateTime(), invitation.getUpdateTime(),
                            invitation.getGameType(), invitation.getScreenSource());
            FALLBACK_LOG.error(sql);
        } catch (final Exception e) {
            LOG.error("Failed to write to fallback log: " + sql, e);
        }
    }

    public int getNumberOfAcceptedInvites(final BigDecimal invitingPlayerId,
                                          final DateTime sinceCreateTime) {
        return jdbcTemplate.queryForInt(SQL_NUMBER_OF_ACCEPTED_INVITES, invitingPlayerId, asTimestamp(sinceCreateTime));
    }

    public Collection<Invitation> getInvitations(final String recipientIdentifier, final InvitationSource source) {
        notNull(recipientIdentifier, "recipientIdentifier is null");
        notNull(source, "source is null");
        return jdbcTemplate.query(SELECT_INVITATIONS_BY_RECIPIENT_AND_SOURCE,
                new String[]{recipientIdentifier, source.name()},
                invitationMapper);
    }

    public Collection<Invitation> findInvitationsByIssuingPlayerId(final BigDecimal issuingPlayerId) {
        notNull(issuingPlayerId, "issuingPlayerId is null");
        return jdbcTemplate.query(SELECT_INVITATIONS_BY_ISSUING_PLAYER, new Object[]{issuingPlayerId},
                invitationMapper);
    }

    public Invitation findInvitationsByIssuingPlayerRecipientAndSource(final BigDecimal issuingPlayerId,
                                                                       final String recipientIdentifier,
                                                                       final InvitationSource source) {
        notNull(issuingPlayerId, "issuingPlayerId is null");
        notNull(recipientIdentifier, "recipientIdentifier is null");
        notNull(source, "source is null");
        final List<Invitation> matches = jdbcTemplate.query(SELECT_INVITATIONS_BY_ISSUING_PLAYER_RECIPIENT_AND_SOURCE,
                new Object[]{issuingPlayerId, recipientIdentifier, source.name()}, invitationMapper);
        if (matches.size() > 1) {
            // TODO log or throw?
            throw new RuntimeException(String.format("Found more than 1 matching invitation (issuingPlayerId=%s, "
                    + "recipientIdentifier=%s, source=%s)", issuingPlayerId, recipientIdentifier, source));
        }
        if (matches.isEmpty()) {
            return null;
        }
        return matches.get(0);
    }

    private static class InvitationMapper implements RowMapper<Invitation> {
        @Override
        public Invitation mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            Long rewardAmount = rs.getLong("REWARD");
            if (rs.wasNull()) {
                rewardAmount = null;
            }

            return new Invitation(BigDecimals.strip(rs.getBigDecimal("PLAYER_ID")), rs.getString("RECIPIENT_IDENTIFIER"),
                    InvitationSource.valueOf(rs.getString("INVITED_FROM")), InvitationStatus.valueOf(rs
                    .getString("STATUS")), rewardAmount, new DateTime(rs.getTimestamp("CREATED_TS")),
                    new DateTime(rs.getTimestamp("UPDATED_TS")), rs.getString("GAME_TYPE"),
                    rs.getString("SCREEN_SOURCE"));
        }
    }
}
