package com.yazino.platform.persistence.tournament;

import com.yazino.platform.tournament.TournamentType;
import com.yazino.platform.tournament.TournamentVariationPayout;
import com.yazino.platform.tournament.TournamentVariationRound;
import com.yazino.platform.tournament.TournamentVariationTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Provides a {@link RowMapper} for {@link TournamentVariationTemplate}'s
 */
public class TournamentVariationTemplateRowMapper implements RowMapper<TournamentVariationTemplate> {
    private static final String SELECT_TOURNAMENT_VARIATION_PAYOUT
            = "SELECT * FROM TOURNAMENT_VARIATION_PAYOUT WHERE TOURNAMENT_VARIATION_TEMPLATE_ID = ? ORDER BY RANK";
    private static final String SELECT_TOURNAMENT_VARIATION_ROUND
            = "SELECT * FROM TOURNAMENT_VARIATION_ROUND"
            + " WHERE TOURNAMENT_VARIATION_TEMPLATE_ID = ? ORDER BY ROUND_NUMBER";
    private static final int ONE_DAY = 86400000;

    private final RowMapper<TournamentVariationPayout> tournamentVariationPayoutRowMapper
            = new TournamentVariationPayoutRowMapper();
    private final RowMapper<TournamentVariationRound> tournamentVariationRoundRowMapper
            = new TournamentVariationRoundRowMapper();

    private final JdbcTemplate template;

    public TournamentVariationTemplateRowMapper(final JdbcTemplate template) {
        notNull(template, "template must not be null");
        this.template = template;
    }

    public TournamentVariationTemplate mapRow(final ResultSet rs, final int rowNum) throws SQLException {
        final BigDecimal templateId = rs.getBigDecimal("TOURNAMENT_VARIATION_TEMPLATE_ID");
        @SuppressWarnings("unchecked")
        final List<TournamentVariationPayout> tournamentVariationPayouts = template.query(
                SELECT_TOURNAMENT_VARIATION_PAYOUT,
                new Object[]{templateId},
                tournamentVariationPayoutRowMapper);
        @SuppressWarnings("unchecked")
        final List<TournamentVariationRound> tournamentVariationRounds = template.query(
                SELECT_TOURNAMENT_VARIATION_ROUND,
                new Object[]{templateId},
                tournamentVariationRoundRowMapper);
        return new TournamentVariationTemplate(
                templateId,
                TournamentType.valueOf(rs.getString("TOURNAMENT_TYPE")),
                rs.getString("NAME"),
                rs.getBigDecimal("ENTRY_FEE"),
                rs.getBigDecimal("SERVICE_FEE"),
                rs.getBigDecimal("PRIZE_POOL"),
                rs.getBigDecimal("STARTING_CHIPS"),
                rs.getInt("MIN_PLAYERS"),
                rs.getInt("MAX_PLAYERS"),
                rs.getString("GAME_TYPE"),
                ONE_DAY,
                rs.getString("ALLOCATOR"),
                tournamentVariationPayouts,
                tournamentVariationRounds
        );
    }

    private class TournamentVariationPayoutRowMapper implements RowMapper<TournamentVariationPayout> {
        @Override
        public TournamentVariationPayout mapRow(final ResultSet rs,
                                                final int rowNum) throws SQLException {
            return new TournamentVariationPayout(
                    rs.getInt("RANK"),
                    rs.getBigDecimal("PAYOUT")
            );
        }
    }

    private class TournamentVariationRoundRowMapper implements RowMapper<TournamentVariationRound> {
        @Override
        public TournamentVariationRound mapRow(final ResultSet rs,
                                               final int rowNum) throws SQLException {
            return new TournamentVariationRound(
                    rs.getInt("ROUND_NUMBER"),
                    rs.getLong("ROUND_END_INTERVAL"),
                    rs.getLong("ROUND_LENGTH"),
                    rs.getBigDecimal("GAME_VARIATION_TEMPLATE_ID"),
                    rs.getString("CLIENT_PROPERTIES_ID"),
                    rs.getBigDecimal("MINIMUM_BALANCE"),
                    rs.getString("DESCRIPTION"));
        }
    }

}
