package com.yazino.platform.persistence.statistic;

import com.yazino.platform.model.statistic.ExperienceFactor;
import com.yazino.platform.model.statistic.LevelDefinition;
import com.yazino.platform.model.statistic.LevelingSystem;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.apache.commons.lang3.Validate.notNull;

@Repository("levelingSystemDAO")
public class JDBCLevelingSystemDAO implements LevelingSystemDAO {
    private static final Logger LOG = LoggerFactory.getLogger(JDBCLevelingSystemDAO.class);
    private static final String RECORD_DELIMITER = "\n";
    private static final String FIELD_DELIMITER = "\t";

    private static final String SELECT_ALL = "SELECT * FROM LEVEL_SYSTEM";

    private final LevelingSystemRowMapper levelingSystemRowMapper = new LevelingSystemRowMapper();

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public JDBCLevelingSystemDAO(@Qualifier("jdbcTemplate") final JdbcTemplate jdbcTemplate) {
        notNull(jdbcTemplate, "JDBC Template may not be null");
        this.jdbcTemplate = jdbcTemplate;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<LevelingSystem> findAll() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Finding all level systems");
        }

        final Collection<LevelingSystem> levels = jdbcTemplate.query(SELECT_ALL, levelingSystemRowMapper);
        if (levels == null) {
            return Collections.emptySet();
        }
        return levels;
    }

    private class LevelingSystemRowMapper implements RowMapper {
        @Override
        public Object mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final String gameType = rs.getString("GAME_TYPE");
            final Collection<ExperienceFactor> experienceFactors
                    = buildExperienceFactors(rs.getString("EXPERIENCE_FACTORS"));
            final List<LevelDefinition> levelDefinitions = buildLevelDefinitions(rs.getString("LEVEL_DEFINITIONS"));
            return new LevelingSystem(gameType, experienceFactors, levelDefinitions);
        }
    }

    private Collection<ExperienceFactor> buildExperienceFactors(final String experienceFactors) {
        final Set<ExperienceFactor> result = new HashSet<ExperienceFactor>();
        final StringTokenizer records = new StringTokenizer(experienceFactors, RECORD_DELIMITER);
        while (records.hasMoreTokens()) {
            final String record = records.nextToken();
            final String[] fields = record.split(FIELD_DELIMITER);
            if (fields.length != 2) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format("Line '%s' has wrong number of fields (expected 2, got %s)"
                            , record, fields.length));
                }
                continue;
            }
            final String event = fields[0];
            final BigDecimal points = new BigDecimal(fields[1]);
            result.add(new ExperienceFactor(event, points));
        }
        return result;
    }

    private List<LevelDefinition> buildLevelDefinitions(final String levelDefinitions) {
        final List<LevelDefinition> result = new ArrayList<LevelDefinition>();
        final StringTokenizer records = new StringTokenizer(levelDefinitions, RECORD_DELIMITER);
        int level = 1;
        BigDecimal minPoints = BigDecimal.ZERO;
        while (records.hasMoreTokens()) {
            final String record = records.nextToken();
            final String[] tokens = record.split(FIELD_DELIMITER);
            if (StringUtils.isBlank(record)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format("Level definition at line %s is blank. Ignoring", level));
                }
                continue;
            }
            final BigDecimal maxPoints = new BigDecimal(tokens[0]);
            final BigDecimal chips = new BigDecimal(tokens[1]);
            result.add(new LevelDefinition(level, minPoints, maxPoints, chips));
            level++;
            minPoints = maxPoints;
        }
        return result;
    }
}
