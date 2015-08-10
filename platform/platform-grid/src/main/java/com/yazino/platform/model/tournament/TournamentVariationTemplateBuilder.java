package com.yazino.platform.model.tournament;

import com.yazino.platform.tournament.TournamentType;
import com.yazino.platform.tournament.TournamentVariationPayout;
import com.yazino.platform.tournament.TournamentVariationRound;
import com.yazino.platform.tournament.TournamentVariationTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Class designed for TEST USE ONLY AT THIS POINT.
 * Move this class out of the production tree using the mvn test-jar plugin. When this was attempted surefire was
 * failing but wasn't creating any output (or an output directory).
 */
public class TournamentVariationTemplateBuilder {
    private static final String TEMPLATE_INSERT = "INSERT IGNORE INTO TOURNAMENT_VARIATION_TEMPLATE("
            + "TOURNAMENT_TYPE, "
            + "NAME, "
            + "GAME_TYPE,"
            + "ENTRY_FEE, "
            + "SERVICE_FEE,"
            + "STARTING_CHIPS,"
            + "MIN_PLAYERS,"
            + "MAX_PLAYERS,"
            + "ALLOCATOR) "
            + "VALUES ('%s', '%s', '%s', %s, %s, %s, %s, %s, '%s')";
    private static final String PAYOUT_INSERT = "INSERT IGNORE INTO TOURNAMENT_VARIATION_PAYOUT"
            + "(TOURNAMENT_VARIATION_TEMPLATE_ID, RANK, PAYOUT) VALUES (?, ?, ?)";
    private static final String ROUND_INSERT = "INSERT IGNORE INTO TOURNAMENT_VARIATION_ROUND("
            + "TOURNAMENT_VARIATION_TEMPLATE_ID, "
            + "ROUND_NUMBER, "
            + "ROUND_END_INTERVAL, "
            + "ROUND_LENGTH, "
            + "GAME_VARIATION_TEMPLATE_ID, "
            + "CLIENT_PROPERTIES_ID, "
            + "MINIMUM_BALANCE) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final int ONE_DAY = 86400000;

    private JdbcTemplate jdbcTemplate;
    private BigDecimal tournamentVariationTemplateId = null;
    private TournamentType tournamentType = null;
    private String templateName = null;
    private BigDecimal entryFee = null;
    private BigDecimal serviceFee = null;
    private BigDecimal prizePool = null;
    private BigDecimal startingChips = null;
    private Integer minPlayers = null;
    private Integer maxPlayers = null;
    private String gameType = null;
    private String allocator = "EVEN_BY_BALANCE";
    private List<TournamentVariationPayout> tournamentVariationPayouts = new ArrayList<TournamentVariationPayout>();
    private List<TournamentVariationRound> tournamentVariationRounds = new ArrayList<TournamentVariationRound>();
    private long expiryDelay = ONE_DAY;

    public TournamentVariationTemplateBuilder() {
    }

    public TournamentVariationTemplateBuilder(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public TournamentVariationTemplateBuilder(final JdbcTemplate jdbcTemplate,
                                              final TournamentVariationTemplate tournamentVariationTemplate) {
        this.jdbcTemplate = jdbcTemplate;

        tournamentVariationTemplateId = tournamentVariationTemplate.getTournamentVariationTemplateId();
        tournamentType = tournamentVariationTemplate.getTournamentType();
        templateName = tournamentVariationTemplate.getTemplateName();
        entryFee = tournamentVariationTemplate.getEntryFee();
        serviceFee = tournamentVariationTemplate.getServiceFee();
        startingChips = tournamentVariationTemplate.getStartingChips();
        minPlayers = tournamentVariationTemplate.getMinPlayers();
        maxPlayers = tournamentVariationTemplate.getMaxPlayers();
        gameType = tournamentVariationTemplate.getGameType();

        if (tournamentVariationTemplate.getTournamentPayouts() != null) {
            tournamentVariationPayouts = new ArrayList<TournamentVariationPayout>(
                    tournamentVariationTemplate.getTournamentPayouts());
        }
        if (tournamentVariationTemplate.getTournamentRounds() != null) {
            tournamentVariationRounds = new ArrayList<TournamentVariationRound>(
                    tournamentVariationTemplate.getTournamentRounds());
        }
    }

    public TournamentVariationTemplateBuilder setTournamentVariationTemplateId(
            final BigDecimal newTournamentVariationTemplateId) {
        this.tournamentVariationTemplateId = newTournamentVariationTemplateId;
        return this;
    }

    public TournamentVariationTemplateBuilder setTournamentType(final TournamentType newTournamentType) {
        this.tournamentType = newTournamentType;
        return this;
    }

    public TournamentVariationTemplateBuilder setGameType(final String newGameType) {
        this.gameType = newGameType;
        return this;
    }

    public TournamentVariationTemplateBuilder setTemplateName(final String newTemplateName) {
        this.templateName = newTemplateName;
        return this;
    }

    public TournamentVariationTemplateBuilder setEntryFee(final BigDecimal newEntryFee) {
        this.entryFee = newEntryFee;
        return this;
    }

    public TournamentVariationTemplateBuilder setServiceFee(final BigDecimal newServiceFee) {
        this.serviceFee = newServiceFee;
        return this;
    }

    public TournamentVariationTemplateBuilder setPrizePool(final BigDecimal newPrizePool) {
        this.prizePool = newPrizePool;
        return this;
    }

    public TournamentVariationTemplateBuilder setStartingChips(final BigDecimal newStartingChips) {
        this.startingChips = newStartingChips;
        return this;
    }

    public TournamentVariationTemplateBuilder setMinPlayers(final Integer newMinPlayers) {
        this.minPlayers = newMinPlayers;
        return this;
    }

    public TournamentVariationTemplateBuilder setMaxPlayers(final Integer newMaxPlayers) {
        this.maxPlayers = newMaxPlayers;
        return this;
    }

    public TournamentVariationTemplateBuilder setTournamentPayouts(
            final List<TournamentVariationPayout> newTournamentVariationPayouts) {
        this.tournamentVariationPayouts = newTournamentVariationPayouts;
        return this;
    }

    public TournamentVariationTemplateBuilder addTournamentPayout(
            final TournamentVariationPayout tournamentVariationPayout) {
        tournamentVariationPayouts.add(tournamentVariationPayout);
        return this;
    }

    public TournamentVariationTemplateBuilder setTournamentRounds(
            final List<TournamentVariationRound> newTournamentVariationRounds) {
        this.tournamentVariationRounds = newTournamentVariationRounds;
        return this;
    }

    public TournamentVariationTemplateBuilder addTournamentRound(
            final TournamentVariationRound tournamentVariationRound) {
        tournamentVariationRounds.add(tournamentVariationRound);
        return this;
    }

    public TournamentVariationTemplateBuilder setExpiryDelay(final long newExpiryDelay) {
        this.expiryDelay = newExpiryDelay;
        return this;
    }

    public TournamentVariationTemplateBuilder setAllocator(final String newAllocator) {
        this.allocator = newAllocator;
        return this;
    }

    public TournamentVariationTemplate toTemplate() {
        return new TournamentVariationTemplate(
                tournamentVariationTemplateId,
                tournamentType,
                templateName,
                entryFee,
                serviceFee,
                prizePool,
                startingChips,
                minPlayers,
                maxPlayers,
                gameType,
                expiryDelay,
                allocator,
                tournamentVariationPayouts,
                tournamentVariationRounds
        );
    }

    public TournamentVariationTemplate saveToDatabase() {
        notNull(jdbcTemplate, "JDBC Template required to save to DB");
        tournamentVariationTemplateId = BigDecimal.valueOf(jdbcTemplate.execute(
                String.format(
                        TEMPLATE_INSERT,
                        tournamentType,
                        templateName,
                        gameType,
                        entryFee,
                        serviceFee,
                        startingChips,
                        minPlayers,
                        maxPlayers,
                        allocator
                ),
                new CallableStatementCallback<Long>() {
                    @Override
                    public Long doInCallableStatement(final CallableStatement cs)
                            throws SQLException, DataAccessException {
                        long key;
                        cs.execute();
                        final ResultSet rs = cs.getGeneratedKeys();
                        if (rs.next()) {
                            key = rs.getLong(1);
                            rs.close();
                            return key;
                        }
                        return null;
                    }
                }));
        for (TournamentVariationRound tournamentVariationRound : tournamentVariationRounds) {
            jdbcTemplate.update(ROUND_INSERT,
                    tournamentVariationTemplateId,
                    tournamentVariationRound.getRoundNumber(),
                    tournamentVariationRound.getRoundEndInterval(),
                    tournamentVariationRound.getRoundLength(),
                    tournamentVariationRound.getGameVariationTemplateId(),
                    tournamentVariationRound.getClientPropertiesId(),
                    tournamentVariationRound.getMinimumBalance());
        }
        for (TournamentVariationPayout tournamentVariationPayout : tournamentVariationPayouts) {
            jdbcTemplate.update((PAYOUT_INSERT),
                    tournamentVariationTemplateId,
                    tournamentVariationPayout.getRank(),
                    tournamentVariationPayout.getPayout());
        }
        return toTemplate();
    }
}

