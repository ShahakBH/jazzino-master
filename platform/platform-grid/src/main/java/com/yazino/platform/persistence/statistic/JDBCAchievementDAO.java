package com.yazino.platform.persistence.statistic;

import com.yazino.platform.model.statistic.Achievement;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

@Repository("achievementDAO")
public class JDBCAchievementDAO implements AchievementDAO {

    private static final Logger LOG = LoggerFactory.getLogger(JDBCAchievementDAO.class);

    private static final String SELECT_ALL = "SELECT * FROM ACHIEVEMENT";

    private final AchievementRowMapper achievementRowMapper = new AchievementRowMapper();

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public JDBCAchievementDAO(@Qualifier("jdbcTemplate") final JdbcTemplate jdbcTemplate) {
        notNull(jdbcTemplate, "JDBC Template may not be null");

        this.jdbcTemplate = jdbcTemplate;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<Achievement> findAll() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Finding all achievements");
        }

        final Collection<Achievement> achievements = jdbcTemplate.query(SELECT_ALL, achievementRowMapper);
        if (achievements == null) {
            return Collections.emptySet();
        }
        return achievements;
    }

    private class AchievementRowMapper implements RowMapper {
        @Override
        public Object mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final String id = rs.getString("ACHIEVEMENT_ID");
            final String title = rs.getString("ACHIEVEMENT_TITLE");
            final String message = rs.getString("ACHIEVEMENT_MESSAGE");
            final String postedAchievementTitleText = rs.getString("POSTED_ACHIEVEMENT_TITLE_TEXT");
            final String postedAchievementTitleLink = rs.getString("POSTED_ACHIEVEMENT_TITLE_LINK");
            final String postedAchievementActionText = rs.getString("POSTED_ACHIEVEMENT_ACTION_NAME");
            final String postedAchievementActionLink = rs.getString("POSTED_ACHIEVEMENT_ACTION_LINK");
            final int level = rs.getInt("ACHIEVEMENT_LEVEL");
            final String shortDescription = rs.getString("ACHIEVEMENT_SHORT_DESCRIPTION");
            final String howToGet = rs.getString("ACHIEVEMENT_HOW_TO_GET");
            final String events = rs.getString("EVENT");
            final String accumulator = rs.getString("ACCUMULATOR");
            final String accumulatorParameters = rs.getString("ACCUMULATOR_PARAMS");
            final String gameType = rs.getString("GAME_TYPE");
            final boolean recurring = rs.getBoolean("RECURRING");

            final Set<String> eventList = new HashSet<String>();
            if (events != null) {
                for (final String event : events.split(",")) {
                    if (StringUtils.isNotBlank(event)) {
                        eventList.add(event);
                    }
                }
            }

            return new Achievement(id, level, title, message, shortDescription, howToGet, postedAchievementTitleText,
                    postedAchievementTitleLink, postedAchievementActionText, postedAchievementActionLink,
                    gameType, eventList, accumulator, accumulatorParameters, recurring);
        }
    }
}
