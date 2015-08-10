package com.yazino.platform.persistence.tournament;

import com.gigaspaces.datasource.DataIterator;
import com.yazino.platform.model.tournament.TrophyLeaderboard;
import com.yazino.platform.persistence.DataIterable;
import com.yazino.platform.persistence.ResultSetIterator;
import com.yazino.platform.tournament.TrophyLeaderboardPlayer;
import com.yazino.platform.tournament.TrophyLeaderboardPlayers;
import com.yazino.platform.tournament.TrophyLeaderboardPosition;
import com.yazino.platform.util.BigDecimals;
import com.yazino.platform.util.community.AvatarTokeniser;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Database persistence for {@link com.yazino.platform.model.tournament.TrophyLeaderboard}s.
 */
@Repository("trophyLeaderboardDao")
public class JDBCTrophyLeaderboardDAO implements TrophyLeaderboardDao, DataIterable<TrophyLeaderboard> {

    private static final Logger LOG = LoggerFactory.getLogger(JDBCTrophyLeaderboardDAO.class);

    private static final String INSERT_OR_UPDATE_LEADERBOARD = "INSERT INTO LEADERBOARD "
            + "(LEADERBOARD_ID,START_TS,END_TS,CYCLE_LENGTH,CYCLE_END_TS,ACTIVE, "
            + "GAME_TYPE,POINT_BONUS_PER_PLAYER,LEADERBOARD_NAME) "
            + "VALUES (?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE START_TS=VALUES(START_TS),END_TS=VALUES(END_TS),"
            + "CYCLE_END_TS=VALUES(CYCLE_END_TS),ACTIVE=VALUES(ACTIVE),"
            + "POINT_BONUS_PER_PLAYER=VALUES(POINT_BONUS_PER_PLAYER),"
            + "LEADERBOARD_NAME=VALUES(LEADERBOARD_NAME)";
    private static final String DELETE_LEADERBOARD_PLAYER = "DELETE FROM LEADERBOARD_PLAYER WHERE LEADERBOARD_ID=?";
    private static final String INSERT_LEADERBOARD_PLAYER_VALUES = "(?,?,?,?,?)";
    private static final String INSERT_LEADERBOARD_PLAYER = "INSERT INTO LEADERBOARD_PLAYER "
            + "(LEADERBOARD_ID,PLAYER_ID,LEADERBOARD_POSITION, PLAYER_NAME,PLAYER_POINTS) VALUES ";
    private static final String DELETE_LEADERBOARD_POSITION = "DELETE FROM LEADERBOARD_POSITION WHERE LEADERBOARD_ID=?";
    private static final String INSERT_LEADERBOARD_POSITION_VALUES = "(?,?,?,?,?)";
    private static final String INSERT_LEADERBOARD_POSITION = "INSERT INTO LEADERBOARD_POSITION "
            + "(LEADERBOARD_ID,LEADERBOARD_POSITION,AWARD_POINTS,AWARD_PAYOUT,TROPHY_ID) VALUES ";

    private static final String SELECT_ACTIVE_LEADERBOARD = "SELECT * FROM LEADERBOARD WHERE ACTIVE=TRUE";
    private static final String SELECT_LEADERBOARD_PLAYERS
            = "SELECT LP.*,P.PICTURE_LOCATION FROM LEADERBOARD_PLAYER LP,PLAYER P "
            + "WHERE LP.LEADERBOARD_ID=? AND LP.PLAYER_ID=P.PLAYER_ID ORDER BY LP.PLAYER_POINTS DESC";
    private static final String SELECT_LEADERBOARD_POSITIONS
            = "SELECT * FROM LEADERBOARD_POSITION WHERE LEADERBOARD_ID=? ORDER BY LEADERBOARD_POSITION ASC";

    private final TrophyLeaderboardPlayerRowMapper trophyLeaderboardPlayerRowMapper
            = new TrophyLeaderboardPlayerRowMapper();
    private final TrophyLeaderboardPositionRowMapper trophyLeaderboardPositionRowMapper
            = new TrophyLeaderboardPositionRowMapper();
    private final TrophyLeaderboardRowMapper trophyLeaderboardRowMapper = new TrophyLeaderboardRowMapper();

    private final JdbcTemplate jdbcTemplate;
    private final AvatarTokeniser avatarTokeniser;

    @Autowired
    public JDBCTrophyLeaderboardDAO(final JdbcTemplate jdbcTemplate,
                                    final AvatarTokeniser avatarTokeniser) {
        notNull(jdbcTemplate, "JDBC Template may not be null");
        notNull(avatarTokeniser, "avatarTokeniser may not be null");

        this.jdbcTemplate = jdbcTemplate;
        this.avatarTokeniser = avatarTokeniser;
    }

    @Override
    public void save(final TrophyLeaderboard trophyLeaderboard) {
        notNull(trophyLeaderboard, "Trophy Leaderboard may not be null");

        if (LOG.isDebugEnabled()) {
            LOG.debug("Leaderboard " + trophyLeaderboard + ": saving");
        }

        jdbcTemplate.update(new PreparedStatementCreator() {
            @SuppressWarnings("UnusedAssignment")
            public PreparedStatement createPreparedStatement(final Connection conn) throws SQLException {
                final PreparedStatement st = conn.prepareStatement(INSERT_OR_UPDATE_LEADERBOARD);

                int index = 1;

                st.setBigDecimal(index++, trophyLeaderboard.getId());
                st.setTimestamp(index++, new Timestamp(trophyLeaderboard.getStartTime().getMillis()));
                st.setTimestamp(index++, new Timestamp(trophyLeaderboard.getEndTime().getMillis()));
                st.setLong(index++, trophyLeaderboard.getCycle().getMillis());
                if (trophyLeaderboard.getCurrentCycleEnd() != null) {
                    st.setTimestamp(index++, new Timestamp(trophyLeaderboard.getCurrentCycleEnd().getMillis()));
                } else {
                    st.setNull(index++, Types.TIMESTAMP);
                }
                st.setBoolean(index++, trophyLeaderboard.getActive());
                st.setString(index++, trophyLeaderboard.getGameType());
                st.setLong(index++, trophyLeaderboard.getPointBonusPerPlayer());
                st.setString(index++, trophyLeaderboard.getName());

                return st;
            }
        });

        saveLeaderboardPositions(trophyLeaderboard);
        saveLeaderboardPlayers(trophyLeaderboard);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Trophy leaderboard saved: " + trophyLeaderboard.getId());
        }
    }

    private void saveLeaderboardPlayers(final TrophyLeaderboard trophyLeaderboard) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Leaderboard " + trophyLeaderboard + ": saving players");
        }

        jdbcTemplate.update(DELETE_LEADERBOARD_PLAYER, trophyLeaderboard.getId());

        if (trophyLeaderboard.getOrderedByPosition() == null || trophyLeaderboard.getOrderedByPosition().size() == 0) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Leaderboard " + trophyLeaderboard + ": has no players");
            }

            return;
        }

        final StringBuilder queryBuilder = new StringBuilder(INSERT_LEADERBOARD_PLAYER);
        for (int i = 0; i < trophyLeaderboard.getOrderedByPosition().size(); ++i) {
            if (i > 0) {
                queryBuilder.append(",");
            }
            queryBuilder.append(INSERT_LEADERBOARD_PLAYER_VALUES);
        }

        jdbcTemplate.update(new PreparedStatementCreator() {
            public PreparedStatement createPreparedStatement(final Connection conn) throws SQLException {
                final PreparedStatement st = conn.prepareStatement(queryBuilder.toString());

                int index = 1;
                for (final TrophyLeaderboardPlayer player : trophyLeaderboard.getOrderedByPosition()) {
                    st.setBigDecimal(index++, trophyLeaderboard.getId());
                    st.setBigDecimal(index++, player.getPlayerId());
                    st.setInt(index++, player.getLeaderboardPosition());
                    st.setString(index++, player.getPlayerName());
                    st.setLong(index++, player.getPoints());
                }

                return st;
            }
        });
    }

    private void saveLeaderboardPositions(final TrophyLeaderboard trophyLeaderboard) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Leaderboard " + trophyLeaderboard + ": saving positions");
        }

        jdbcTemplate.update(DELETE_LEADERBOARD_POSITION, trophyLeaderboard.getId());

        if (trophyLeaderboard.getPositionData() == null || trophyLeaderboard.getPositionData().size() == 0) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Leaderboard " + trophyLeaderboard + ": has no positions");
            }

            return;
        }

        final StringBuilder queryBuilder = new StringBuilder(INSERT_LEADERBOARD_POSITION);
        for (int i = 0; i < trophyLeaderboard.getPositionData().size(); ++i) {
            if (i > 0) {
                queryBuilder.append(",");
            }
            queryBuilder.append(INSERT_LEADERBOARD_POSITION_VALUES);
        }

        jdbcTemplate.update(new PreparedStatementCreator() {
            public PreparedStatement createPreparedStatement(final Connection conn) throws SQLException {
                final PreparedStatement st = conn.prepareStatement(queryBuilder.toString());

                int index = 1;
                for (final TrophyLeaderboardPosition position : trophyLeaderboard.getPositionData().values()) {
                    st.setBigDecimal(index++, trophyLeaderboard.getId());
                    st.setInt(index++, position.getPosition());
                    st.setLong(index++, position.getAwardPoints());
                    st.setLong(index++, position.getAwardPayout());
                    st.setBigDecimal(index++, position.getTrophyId());
                }

                return st;
            }
        });
    }

    @Override
    public DataIterator<TrophyLeaderboard> iterateAll() {
        return new ResultSetIterator<TrophyLeaderboard>(jdbcTemplate, new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(final Connection con) throws SQLException {
                return con.prepareStatement(SELECT_ACTIVE_LEADERBOARD);
            }
        }, trophyLeaderboardRowMapper);
    }

    private class TrophyLeaderboardRowMapper implements RowMapper<TrophyLeaderboard> {
        @Override
        public TrophyLeaderboard mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final BigDecimal leaderboardId = rs.getBigDecimal("LEADERBOARD_ID");
            final DateTime startTime = new DateTime(rs.getTimestamp("START_TS").getTime());
            final DateTime endTime = new DateTime(rs.getTimestamp("END_TS").getTime());
            final Timestamp cycleEndTimestamp = rs.getTimestamp("CYCLE_END_TS");
            DateTime cycleEndTime = null;
            if (cycleEndTimestamp != null) {
                cycleEndTime = new DateTime(cycleEndTimestamp.getTime());
            }

            final Duration duration = new Duration(rs.getLong("CYCLE_LENGTH"));
            final boolean active = rs.getBoolean("ACTIVE");
            final String gameType = rs.getString("GAME_TYPE");
            final long pointBonusPerPlayer = rs.getLong("POINT_BONUS_PER_PLAYER");
            final String leaderboardName = rs.getString("LEADERBOARD_NAME");


            final TrophyLeaderboard trophyLeaderboard = new TrophyLeaderboard(
                    leaderboardId, leaderboardName, gameType, new Interval(startTime, endTime), duration);
            trophyLeaderboard.setActive(active);
            trophyLeaderboard.setPointBonusPerPlayer(pointBonusPerPlayer);
            trophyLeaderboard.setCurrentCycleEnd(cycleEndTime);

            final TrophyLeaderboardPlayers players = new TrophyLeaderboardPlayers();
            final List<TrophyLeaderboardPlayer> trophyLeaderboardPlayers = jdbcTemplate.query(
                    SELECT_LEADERBOARD_PLAYERS, trophyLeaderboardPlayerRowMapper, leaderboardId);

            for (TrophyLeaderboardPlayer trophyLeaderboardPlayer : trophyLeaderboardPlayers) {
                players.addPlayer(trophyLeaderboardPlayer);
            }

            trophyLeaderboard.setPlayers(players);

            final List<TrophyLeaderboardPosition> positions = jdbcTemplate.query(
                    SELECT_LEADERBOARD_POSITIONS, new Object[]{leaderboardId}, trophyLeaderboardPositionRowMapper);
            for (TrophyLeaderboardPosition position : positions) {
                trophyLeaderboard.addPosition(position);
            }

            return trophyLeaderboard;
        }
    }

    private class TrophyLeaderboardPlayerRowMapper implements RowMapper<TrophyLeaderboardPlayer> {
        @Override
        public TrophyLeaderboardPlayer mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final int position = rs.getInt("LEADERBOARD_POSITION");
            final BigDecimal playerId = BigDecimals.strip(rs.getBigDecimal("PLAYER_ID"));
            final String playerName = rs.getString("PLAYER_NAME");
            final long points = rs.getLong("PLAYER_POINTS");
            final String pictureUrl = avatarTokeniser.detokenise(rs.getString("PICTURE_LOCATION"));

            return new TrophyLeaderboardPlayer(position, playerId, playerName, points, pictureUrl);
        }
    }

    private class TrophyLeaderboardPositionRowMapper implements RowMapper<TrophyLeaderboardPosition> {
        @Override
        public TrophyLeaderboardPosition mapRow(final ResultSet rs,
                                                final int rowNum) throws SQLException {
            final int leaderboardPosition = rs.getInt("LEADERBOARD_POSITION");
            final long awardPoints = rs.getLong("AWARD_POINTS");
            final long awardPayout = rs.getLong("AWARD_PAYOUT");
            final BigDecimal trophyId = rs.getBigDecimal("TROPHY_ID");

            return new TrophyLeaderboardPosition(leaderboardPosition, awardPoints, awardPayout, trophyId);
        }
    }
}
