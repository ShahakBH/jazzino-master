package com.yazino.platform.persistence.tournament;

import com.yazino.platform.model.tournament.TournamentPlayer;
import com.yazino.platform.model.tournament.TournamentPlayerStatus;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Database persistence for {@link com.yazino.platform.model.tournament.TournamentPlayer}s.
 */
@Repository
public class JDBCTournamentPlayerDAO implements TournamentPlayerDao {

    private static final Logger LOG = LoggerFactory.getLogger(JDBCTournamentPlayerDAO.class);

    private static final String SELECT_FOR_TOURNAMENT = "SELECT * FROM TOURNAMENT_PLAYER WHERE TOURNAMENT_ID=?";

    private static final String INSERT_OR_UPDATE_TOURNAMENT_PLAYER = "INSERT INTO TOURNAMENT_PLAYER "
            + "(PLAYER_ID,TOURNAMENT_ACCOUNT_ID,TOURNAMENT_ID,PLAYER_STATUS,LEADERBOARD_POSITION, NAME, SETTLED_PRIZE, "
            + "ELIMINATION_TS, ELIMINATION_REASON, PLAYER_PROPERTIES) VALUES (?,?,?,?,?,?,?,?,?,?) "
            + "ON DUPLICATE KEY UPDATE PLAYER_STATUS=VALUES(PLAYER_STATUS),"
            + "LEADERBOARD_POSITION=VALUES(LEADERBOARD_POSITION),"
            + "SETTLED_PRIZE=VALUES(SETTLED_PRIZE),ELIMINATION_TS=VALUES(ELIMINATION_TS),"
            + "ELIMINATION_REASON=VALUES(ELIMINATION_REASON),PLAYER_PROPERTIES=VALUES(PLAYER_PROPERTIES)";

    private static final String DELETE_TOURNAMENT_PLAYER = "DELETE FROM TOURNAMENT_PLAYER WHERE "
            + "PLAYER_ID=? AND TOURNAMENT_ACCOUNT_ID=? AND TOURNAMENT_ID=?";

    private final TournamentPlayerRowMapper tournamentPlayerRowMapper = new TournamentPlayerRowMapper();

    private final JdbcTemplate template;
    private static final String FIELD_DELIMITER = "=";
    private static final String RECORD_DELIMITER = "\n";

    @Autowired
    public JDBCTournamentPlayerDAO(final JdbcTemplate template) {
        notNull(template, "JDBC Template may not be null");
        this.template = template;
    }

    public void save(final BigDecimal tournamentId, final TournamentPlayer tournamentPlayer) {
        notNull(tournamentPlayer, "Tournament Player may not be null");

        if (LOG.isDebugEnabled()) {
            LOG.debug("Saving tournament player " + tournamentPlayer);
        }

        template.update(new PreparedStatementCreator() {
            @SuppressWarnings("UnusedAssignment")
            public PreparedStatement createPreparedStatement(final Connection conn) throws SQLException {
                final PreparedStatement st = conn.prepareStatement(INSERT_OR_UPDATE_TOURNAMENT_PLAYER);

                int index = 1;
                st.setBigDecimal(index++, tournamentPlayer.getPlayerId());
                st.setBigDecimal(index++, tournamentPlayer.getAccountId());
                st.setBigDecimal(index++, tournamentId);
                st.setString(index++, tournamentPlayer.getStatus().getId());
                if (tournamentPlayer.getLeaderboardPosition() != null) {
                    st.setInt(index++, tournamentPlayer.getLeaderboardPosition());
                } else {
                    st.setInt(index++, 0);
                }
                st.setString(index++, tournamentPlayer.getName());
                st.setBigDecimal(index++, tournamentPlayer.getSettledPrize());
                if (tournamentPlayer.getEliminationTimestamp() != null) {
                    st.setTimestamp(index++, new Timestamp(tournamentPlayer.getEliminationTimestamp().getMillis()));
                } else {
                    st.setNull(index++, Types.TIMESTAMP);
                }
                if (tournamentPlayer.getEliminationReason() != null) {
                    st.setString(index++, String.valueOf(tournamentPlayer.getEliminationReason()));
                } else {
                    st.setNull(index++, Types.VARCHAR);
                }
                st.setString(index++, buildPropertiesString(tournamentPlayer.getProperties()));
                return st;
            }
        });

        if (LOG.isDebugEnabled()) {
            LOG.debug("Tournament player saved " + tournamentPlayer);
        }
    }

    public void remove(final BigDecimal tournamentId, final TournamentPlayer tournamentPlayer) {
        notNull(tournamentPlayer, "Tournament Player may not be null");

        if (LOG.isDebugEnabled()) {
            LOG.debug("Removing tournament player " + tournamentPlayer);
        }

        final int rowsDeleted = template.update(new PreparedStatementCreator() {
            public PreparedStatement createPreparedStatement(final Connection conn) throws SQLException {
                int fieldIndex = 1;
                final PreparedStatement st = conn.prepareStatement(DELETE_TOURNAMENT_PLAYER);

                st.setBigDecimal(fieldIndex++, tournamentPlayer.getPlayerId());
                st.setBigDecimal(fieldIndex++, tournamentPlayer.getAccountId());
                st.setBigDecimal(fieldIndex, tournamentId);

                return st;
            }
        });

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Tournament player deleted %s, rows affected = %s", tournamentPlayer, rowsDeleted));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<TournamentPlayer> findByTournamentId(final BigDecimal tournamentId) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Selecting players for tournament " + tournamentId);
        }

        final Collection<TournamentPlayer> tournamentPlayers = (Collection<TournamentPlayer>)
                template.query(new PreparedStatementCreator() {
                    public PreparedStatement createPreparedStatement(final Connection conn) throws SQLException {
                        final PreparedStatement st = conn.prepareStatement(SELECT_FOR_TOURNAMENT);

                        st.setBigDecimal(1, tournamentId);

                        return st;
                    }
                }, tournamentPlayerRowMapper);

        return new HashSet<TournamentPlayer>(tournamentPlayers);
    }

    private class TournamentPlayerRowMapper implements RowMapper {
        public Object mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Mapping row " + rowNum);
            }

            final TournamentPlayer tournamentPlayer = new TournamentPlayer();

            tournamentPlayer.setPlayerId(BigDecimals.strip(rs.getBigDecimal("PLAYER_ID")));
            tournamentPlayer.setAccountId(BigDecimals.strip(rs.getBigDecimal("TOURNAMENT_ACCOUNT_ID")));
            tournamentPlayer.setStatus(TournamentPlayerStatus.getById(rs.getString("PLAYER_STATUS")));
            tournamentPlayer.setLeaderboardPosition(rs.getInt("LEADERBOARD_POSITION"));
            tournamentPlayer.setName(rs.getString("NAME"));
            tournamentPlayer.setSettledPrize(rs.getBigDecimal("SETTLED_PRIZE"));
            tournamentPlayer.setEliminationTimestamp(readNullableDateTime(rs.getTimestamp("ELIMINATION_TS")));
            final String eliminationReason = rs.getString("ELIMINATION_REASON");
            if (!StringUtils.isBlank(eliminationReason)) {
                tournamentPlayer.setEliminationReason(TournamentPlayer.EliminationReason.valueOf(eliminationReason));
            }
            tournamentPlayer.setProperties(parsePropertiesString(rs.getString("PLAYER_PROPERTIES")));
            return tournamentPlayer;
        }
    }

    private DateTime readNullableDateTime(final Timestamp value) {
        if (value == null) {
            return null;
        }
        return new DateTime(value.getTime());
    }

    private String buildPropertiesString(final Map<String, String> properties) {
        if (properties == null || properties.size() == 0) {
            return null;
        }

        final StringBuilder builder = new StringBuilder();
        for (final String propertyName : properties.keySet()) {
            builder.append(propertyName);
            final String propertyValue = properties.get(propertyName);
            if (propertyValue != null) {
                builder.append(FIELD_DELIMITER);
                builder.append(propertyValue);
            }
            builder.append(RECORD_DELIMITER);
        }

        return builder.toString();
    }

    private Map<String, String> parsePropertiesString(final String propertiesField) {
        if (StringUtils.isBlank(propertiesField)) {
            return Collections.emptyMap();
        }

        final Map<String, String> properties = new HashMap<String, String>();

        final StringTokenizer records = new StringTokenizer(propertiesField, RECORD_DELIMITER);
        while (records.hasMoreTokens()) {
            final String record = records.nextToken();

            if (record != null && record.trim().length() > 0) {
                final String propertyName;
                final String propertyValue;

                final int fieldSeparatorIndex = record.indexOf(FIELD_DELIMITER);
                if (fieldSeparatorIndex == -1) {
                    propertyName = record.trim();
                    propertyValue = null;
                } else {
                    propertyName = record.substring(0, fieldSeparatorIndex).trim();
                    propertyValue = record.substring(fieldSeparatorIndex + 1);
                }

                properties.put(propertyName, propertyValue);
            }
        }

        return properties;
    }
}
