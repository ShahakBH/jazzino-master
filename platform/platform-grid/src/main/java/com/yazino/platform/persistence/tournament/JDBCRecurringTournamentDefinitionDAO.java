package com.yazino.platform.persistence.tournament;

import com.gigaspaces.datasource.DataIterator;
import com.yazino.platform.model.tournament.RecurringTournamentDefinition;
import com.yazino.platform.persistence.DataIterable;
import com.yazino.platform.persistence.ResultSetIterator;
import com.yazino.platform.tournament.DayPeriod;
import com.yazino.platform.tournament.TournamentVariationTemplate;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * DAO to load {@link RecurringTournamentDefinition}'s.
 */
@Repository("recurringTournamentDefinitionDao")
public class JDBCRecurringTournamentDefinitionDAO implements DataIterable<RecurringTournamentDefinition> {
    private static final Logger LOG = LoggerFactory.getLogger(JDBCRecurringTournamentDefinitionDAO.class);

    private static final String SELECT_ALL = "SELECT DEFINITION_ID, INITIAL_SIGNUP_TIME, SIGNUP_PERIOD,"
            + " FREQUENCY, EXCLUSION_PERIODS, TOURNAMENT_NAME, TOURNAMENT_DESCRIPTION, "
            + "PARTNER_ID, ENABLED, tvt.* FROM RECURRING_TOURNAMENT_DEFINITION rtd, TOURNAMENT_VARIATION_TEMPLATE tvt "
            + "WHERE rtd.TOURNAMENT_VARIATION_TEMPLATE_ID=tvt.TOURNAMENT_VARIATION_TEMPLATE_ID";
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<RecurringTournamentDefinition> rowMapper = defaultRowMapper();
    private final RowMapper<TournamentVariationTemplate> variationTemplateRowMapper;

    @Autowired(required = true)
    public JDBCRecurringTournamentDefinitionDAO(final JdbcTemplate jdbcTemplate) {
        notNull(jdbcTemplate, "jdbcTemplate must not be null");
        this.jdbcTemplate = jdbcTemplate;
        this.variationTemplateRowMapper = new TournamentVariationTemplateRowMapper(jdbcTemplate);
    }

    @Override
    public DataIterator<RecurringTournamentDefinition> iterateAll() {
        return new ResultSetIterator<RecurringTournamentDefinition>(jdbcTemplate, new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(final Connection con) throws SQLException {
                return con.prepareStatement(SELECT_ALL);
            }
        }, rowMapper);
    }

    private RowMapper<RecurringTournamentDefinition> defaultRowMapper() {
        return new RowMapper<RecurringTournamentDefinition>() {
            @Override
            public RecurringTournamentDefinition mapRow(final ResultSet rs,
                                                        final int rowNum) throws SQLException {
                final Integer id = rs.getInt("DEFINITION_ID");
                final Timestamp initialSignupTime = rs.getTimestamp("INITIAL_SIGNUP_TIME");
                final Long signupPeriod = rs.getLong("SIGNUP_PERIOD");
                final Long frequency = rs.getLong("FREQUENCY");
                final String exclusionPeriodValue = rs.getString("EXCLUSION_PERIODS");
                final String tournamentName = rs.getString("TOURNAMENT_NAME");
                final String tournamentDescription = rs.getString("TOURNAMENT_DESCRIPTION");
                final String partnerID = rs.getString("PARTNER_ID");
                final boolean enabled = rs.getBoolean("ENABLED");

                final TournamentVariationTemplate template =
                        variationTemplateRowMapper.mapRow(rs, rowNum);

                final RecurringTournamentDefinition tournamentDefinition = new RecurringTournamentDefinition();
                tournamentDefinition.setId(BigInteger.valueOf(id));
                tournamentDefinition.setInitialSignupTime(new DateTime(initialSignupTime.getTime()));
                tournamentDefinition.setSignupPeriod(signupPeriod);
                tournamentDefinition.setFrequency(frequency);
                tournamentDefinition.setTournamentVariationTemplate(template);
                tournamentDefinition.setTournamentName(tournamentName);
                tournamentDefinition.setTournamentDescription(tournamentDescription);
                tournamentDefinition.setPartnerId(partnerID);
                tournamentDefinition.setEnabled(enabled);

                if (exclusionPeriodValue != null && exclusionPeriodValue.trim().length() > 0) {
                    final String[] exclusionPeriodValues = exclusionPeriodValue.trim().split(",");
                    final Set<DayPeriod> exclusionPeriods = new HashSet<DayPeriod>(exclusionPeriodValues.length);
                    for (String exclusionPeriod : exclusionPeriodValues) {
                        try {
                            exclusionPeriods.add(new DayPeriod(exclusionPeriod));
                        } catch (Exception e) {
                            LOG.error(String.format(
                                    "Failed to add exclusion period because [%s] was not formatted properly (%s)",
                                    exclusionPeriod, e.getMessage()));
                        }
                    }
                    tournamentDefinition.setExclusionPeriods(exclusionPeriods.toArray(
                            new DayPeriod[exclusionPeriods.size()]));

                }
                return tournamentDefinition;
            }
        };
    }
}
