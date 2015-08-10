package com.yazino.platform.tournament;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

public class TournamentVariationTemplate implements Serializable {
    private static final long serialVersionUID = -5015202203695493875L;

    private final BigDecimal tournamentVariationTemplateId;
    private final TournamentType tournamentType;
    private final String templateName;
    private final BigDecimal entryFee;
    private final BigDecimal serviceFee;
    private final BigDecimal prizePool;
    private final BigDecimal startingChips;
    private final Integer minPlayers;
    private final Integer maxPlayers;
    private final String allocator;
    private final List<TournamentVariationPayout> tournamentVariationPayouts;
    private final List<TournamentVariationRound> tournamentVariationRounds;
    private final String gameType;
    private final long expiryDelay;

    public TournamentVariationTemplate(final BigDecimal tournamentVariationTemplateId,
                                       final TournamentType tournamentType,
                                       final String templateName,
                                       final BigDecimal entryFee,
                                       final BigDecimal serviceFee,
                                       final BigDecimal prizePool,
                                       final BigDecimal startingChips,
                                       final Integer minPlayers,
                                       final Integer maxPlayers,
                                       final String gameType,
                                       final long expiryDelay,
                                       final String allocator,
                                       final List<TournamentVariationPayout> payouts,
                                       final List<TournamentVariationRound> rounds) {
        notNull(tournamentVariationTemplateId,
                "Tournament Variation Template ID may not be null");
        notNull(tournamentType, "Tournament Type may not be null");
        notNull(templateName, "Template Name may not be null");

        this.tournamentVariationTemplateId = tournamentVariationTemplateId;
        this.tournamentType = tournamentType;
        this.templateName = templateName;
        this.entryFee = entryFee;
        this.serviceFee = serviceFee;
        this.prizePool = prizePool;
        this.startingChips = startingChips;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.tournamentVariationRounds = rounds;
        this.tournamentVariationPayouts = payouts;
        this.allocator = allocator;
        this.gameType = gameType;
        this.expiryDelay = expiryDelay;
    }

    public BigDecimal getEntryFee() {
        return entryFee;
    }

    public String getGameType() {
        return gameType;
    }

    public BigDecimal getServiceFee() {
        return serviceFee;
    }

    public BigDecimal getPrizePool() {
        return prizePool;
    }

    public BigDecimal getStartingChips() {
        return startingChips;
    }

    public Integer getMinPlayers() {
        return minPlayers;
    }

    public Integer getMaxPlayers() {
        return maxPlayers;
    }

    public BigDecimal getTournamentVariationTemplateId() {
        return tournamentVariationTemplateId;
    }

    public TournamentType getTournamentType() {
        return tournamentType;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getAllocator() {
        return allocator;
    }

    public List<TournamentVariationPayout> getTournamentPayouts() {
        return Collections.unmodifiableList(tournamentVariationPayouts);
    }

    public List<TournamentVariationRound> getTournamentRounds() {
        return Collections.unmodifiableList(tournamentVariationRounds);
    }

    public long getExpiryDelay() {
        return expiryDelay;
    }


    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj.getClass() != getClass()) {
            return false;
        }

        final TournamentVariationTemplate rhs = (TournamentVariationTemplate) obj;
        return new EqualsBuilder()
                .append(tournamentVariationTemplateId, rhs.tournamentVariationTemplateId)
                .append(tournamentType, rhs.tournamentType)
                .append(templateName, rhs.templateName)
                .append(entryFee, rhs.entryFee)
                .append(serviceFee, rhs.serviceFee)
                .append(prizePool, rhs.prizePool)
                .append(startingChips, rhs.startingChips)
                .append(minPlayers, rhs.minPlayers)
                .append(maxPlayers, rhs.maxPlayers)
                .append(allocator, rhs.allocator)
                .append(tournamentVariationPayouts, rhs.tournamentVariationPayouts)
                .append(tournamentVariationRounds, rhs.tournamentVariationRounds)
                .append(expiryDelay, rhs.expiryDelay)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 47)
                .append(tournamentVariationTemplateId)
                .append(tournamentType)
                .append(templateName)
                .append(entryFee)
                .append(serviceFee)
                .append(prizePool)
                .append(startingChips)
                .append(minPlayers)
                .append(maxPlayers)
                .append(allocator)
                .append(tournamentVariationPayouts)
                .append(tournamentVariationRounds)
                .append(expiryDelay)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
