package com.yazino.platform.persistence.statistic;

import com.yazino.platform.model.statistic.PlayerAchievements;
import com.yazino.platform.model.statistic.PlayerLevel;
import com.yazino.platform.model.statistic.PlayerLevels;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

import static org.apache.commons.lang3.Validate.notNull;

@Repository("playerStatsDAO")
public class JDBCPlayerStatsDAO implements PlayerStatsDAO {
    private static final Logger LOG = LoggerFactory.getLogger(JDBCPlayerStatsDAO.class);

    private static final String FIELD_DELIMITER = "\t";
    private static final String RECORD_DELIMITER = "\n";
    public static final String SQL_QUERY_FOR_PLAYER_LEVEL = "SELECT PLAYER_ID, LEVEL FROM PLAYER WHERE PLAYER_ID=?";
    public static final String SQL_QUERY_UPDATE_PLAYER_LEVEL = "UPDATE PLAYER SET LEVEL = ? WHERE PLAYER_ID = ?";
    public static final String SQL_QUERY_PLAYER_ACHIEVEMENTS = "SELECT PLAYER_ID, ACHIEVEMENTS, ACHIEVEMENT_PROGRESS "
            + "FROM PLAYER WHERE PLAYER_ID=?";
    public static final String SQL_QUERY_UPDATE_PLAYER_ACHIEVEMENTS = "UPDATE PLAYER SET ACHIEVEMENTS = ?, "
            + "ACHIEVEMENT_PROGRESS = ? WHERE PLAYER_ID = ?";

    private final PlayerLevelsRowMapper playerLevelsRowMapper = new PlayerLevelsRowMapper();
    private final PlayerAchievementsRowMapper playerAchievementsRowMapper = new PlayerAchievementsRowMapper();

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public JDBCPlayerStatsDAO(@Qualifier("jdbcTemplate") final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @SuppressWarnings("unchecked")
    @Override
    public PlayerLevels getLevels(final BigDecimal playerId) {
        notNull(playerId, "playerId may not be null");
        try {
            return (PlayerLevels) jdbcTemplate.queryForObject(SQL_QUERY_FOR_PLAYER_LEVEL,
                    new Object[]{playerId},
                    playerLevelsRowMapper);

        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public void saveLevels(final PlayerLevels playerLevels) {
        jdbcTemplate.update(new PreparedStatementCreator() {
            @SuppressWarnings("UnusedAssignment")
            public PreparedStatement createPreparedStatement(final Connection conn) throws SQLException {
                final PreparedStatement st = conn.prepareStatement(SQL_QUERY_UPDATE_PLAYER_LEVEL,
                        Statement.NO_GENERATED_KEYS);
                int argIndex = 1;
                st.setString(argIndex++, buildLevelString(playerLevels.getLevels()));
                st.setBigDecimal(argIndex++, playerLevels.getPlayerId());
                return st;
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public PlayerAchievements getAchievements(final BigDecimal playerId) {
        notNull(playerId, "playerId may not be null");
        try {
            return (PlayerAchievements) jdbcTemplate.queryForObject(SQL_QUERY_PLAYER_ACHIEVEMENTS,
                    new Object[]{playerId}, playerAchievementsRowMapper);

        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public void saveAchievements(final PlayerAchievements playerAchievements) {
        notNull(playerAchievements, "playerAchievements is null");
        if (LOG.isDebugEnabled()) {
            LOG.debug("Saving achievements for " + playerAchievements.getPlayerId());
        }
        jdbcTemplate.update(new PreparedStatementCreator() {
            @SuppressWarnings("UnusedAssignment")
            public PreparedStatement createPreparedStatement(final Connection conn) throws SQLException {
                final PreparedStatement st = conn.prepareStatement(SQL_QUERY_UPDATE_PLAYER_ACHIEVEMENTS,
                        Statement.NO_GENERATED_KEYS);
                int argIndex = 1;
                st.setString(argIndex++, buildAchievementsString(playerAchievements.getAchievements()));
                st.setString(argIndex++, buildAchievementProgressString(playerAchievements.getAchievementProgress()));
                st.setBigDecimal(argIndex++, playerAchievements.getPlayerId());
                return st;
            }
        });
    }

    private String buildLevelString(final Map<String, PlayerLevel> levels) {
        final StringBuilder builder = new StringBuilder();
        for (String gameType : levels.keySet()) {
            builder.append(gameType);
            builder.append(FIELD_DELIMITER);
            final PlayerLevel playerLevel = levels.get(gameType);
            builder.append(String.valueOf(playerLevel.getLevel()));
            builder.append(FIELD_DELIMITER);
            builder.append(String.valueOf(playerLevel.getExperience()));
            builder.append(RECORD_DELIMITER);
        }
        return builder.toString();
    }


    private class PlayerLevelsRowMapper implements RowMapper {

        public static final int FIELDS = 3;

        public Object mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final BigDecimal playerId = BigDecimals.strip(rs.getBigDecimal("PLAYER_ID"));
            final Map<String, PlayerLevel> levels = parseLevels(rs.getString("LEVEL"));
            return new PlayerLevels(playerId, levels);
        }

        private Map<String, PlayerLevel> parseLevels(final String levels) {
            if (StringUtils.isBlank(levels)) {
                return new HashMap<String, PlayerLevel>();
            }
            final Map<String, PlayerLevel> result = new HashMap<String, PlayerLevel>();
            final StringTokenizer records = new StringTokenizer(levels, RECORD_DELIMITER);
            while (records.hasMoreTokens()) {
                final String record = records.nextToken();
                final String[] fields = record.split(FIELD_DELIMITER);
                if (fields.length != FIELDS) {
                    continue;
                }
                final String gameType = fields[0];
                final int level = Integer.parseInt(fields[1]);
                final BigDecimal xp = new BigDecimal(fields[2]);
                result.put(gameType, new PlayerLevel(level, xp));
            }
            return result;
        }
    }

    private String buildAchievementsString(final Set<String> achievements) {
        final StringBuilder builder = new StringBuilder();
        for (final String achievementId : achievements) {
            builder.append(achievementId);
            builder.append(RECORD_DELIMITER);
        }

        return builder.toString();
    }


    private String buildAchievementProgressString(final Map<String, String> accumulatorState) {
        final StringBuilder builder = new StringBuilder();
        for (final String accumulatorName : accumulatorState.keySet()) {
            builder.append(accumulatorName);
            builder.append(FIELD_DELIMITER);
            builder.append(accumulatorState.get(accumulatorName));
            builder.append(RECORD_DELIMITER);
        }
        return builder.toString();
    }

    private class PlayerAchievementsRowMapper implements RowMapper {
        @Override
        public Object mapRow(final ResultSet rs, final int i) throws SQLException {
            final BigDecimal playerId = BigDecimals.strip(rs.getBigDecimal("PLAYER_ID"));
            final Set<String> achievements = parseAchievementsString(rs.getString("ACHIEVEMENTS"));
            final Map<String, String> achievementProgress
                    = parseAchievementProgressString(rs.getString("ACHIEVEMENT_PROGRESS"));
            return new PlayerAchievements(playerId, achievements, achievementProgress);
        }

        private Map<String, String> parseAchievementProgressString(final String accumulatorString) {
            if (StringUtils.isBlank(accumulatorString)) {
                return new HashMap<String, String>();
            }
            final Map<String, String> result = new HashMap<String, String>();
            final StringTokenizer records = new StringTokenizer(accumulatorString, RECORD_DELIMITER);
            while (records.hasMoreTokens()) {
                final String record = records.nextToken();
                final int nameIndex = record.indexOf(FIELD_DELIMITER);
                if (nameIndex == -1) {
                    continue;
                }
                final String accumulatorName = record.substring(0, nameIndex);
                final String accumulatorValue = record.substring(nameIndex + 1);

                result.put(accumulatorName, accumulatorValue);
            }
            return result;
        }

        private Set<String> parseAchievementsString(final String achievementField) {
            if (StringUtils.isBlank(achievementField)) {
                return new HashSet<String>();
            }
            final Set<String> achievements = new HashSet<String>();
            final StringTokenizer records = new StringTokenizer(achievementField, RECORD_DELIMITER);
            while (records.hasMoreTokens()) {
                final String record = records.nextToken();

                if (record != null && record.trim().length() > 0) {
                    achievements.add(record.trim());
                }
            }
            return achievements;
        }
    }
}
