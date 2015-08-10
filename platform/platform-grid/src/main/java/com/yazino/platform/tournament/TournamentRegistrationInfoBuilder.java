package com.yazino.platform.tournament;

import org.joda.time.DateTime;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

public class TournamentRegistrationInfoBuilder {
    private Set<BigDecimal> players;
    private BigDecimal tournamentId;
    private DateTime startTimeStamp;
    private BigDecimal entryFee;
    private BigDecimal currentPrizePool;
    private String name;
    private String description;
    private String variationTemplateName;

    public TournamentRegistrationInfoBuilder withPlayers(final Set<BigDecimal> newPlayers) {
        this.players = newPlayers;
        return this;
    }

    public TournamentRegistrationInfoBuilder withTournamentId(final BigDecimal newTournamentId) {
        this.tournamentId = newTournamentId;
        return this;
    }

    public TournamentRegistrationInfoBuilder withStartTimeStamp(final DateTime newStartTimeStamp) {
        this.startTimeStamp = newStartTimeStamp;
        return this;
    }

    public TournamentRegistrationInfoBuilder withEntryFee(final BigDecimal newEntryFee) {
        this.entryFee = newEntryFee;
        return this;
    }

    public TournamentRegistrationInfoBuilder withCurrentPrizePool(final BigDecimal newCurrentPrizePool) {
        this.currentPrizePool = newCurrentPrizePool;
        return this;
    }

    public TournamentRegistrationInfoBuilder withName(final String newName) {
        this.name = newName;
        return this;
    }

    public TournamentRegistrationInfoBuilder withDescription(final String newDescription) {
        this.description = newDescription;
        return this;
    }

    public TournamentRegistrationInfoBuilder withVariationTemplateName(final String newVariationTemplateName) {
        this.variationTemplateName = newVariationTemplateName;
        return this;
    }

    public TournamentRegistrationInfo build() {
        return new TournamentRegistrationInfo(tournamentId,
                startTimeStamp,
                defaultIfNull(entryFee, BigDecimal.ZERO),
                defaultIfNull(currentPrizePool, BigDecimal.ZERO),
                name,
                description,
                variationTemplateName,
                defaultIfNull(players, Collections.<BigDecimal>emptySet()));
    }

}
