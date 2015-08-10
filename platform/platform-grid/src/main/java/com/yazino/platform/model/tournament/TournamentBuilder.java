package com.yazino.platform.model.tournament;

import com.google.common.collect.Lists;
import com.yazino.platform.tournament.TournamentStatus;
import com.yazino.platform.tournament.TournamentType;
import com.yazino.platform.tournament.TournamentVariationPayout;
import org.joda.time.DateTime;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Set;

/**
 * Builds a {@link Tournament} object by providing chaining capabilities.
 */
public class TournamentBuilder {

    private Tournament tournament = new Tournament();
    private TournamentVariationTemplateBuilder variationTemplateBuilder = defaultVariationTemplateBuilder();

    public TournamentBuilder withEntryFee(final BigDecimal entryFee) {
        variationTemplateBuilder.setEntryFee(entryFee);
        return this;
    }

    public TournamentBuilder withName(final String name) {
        tournament.setName(name);
        return this;
    }

    public TournamentBuilder withTournamentId(final BigDecimal id) {
        tournament.setTournamentId(id);
        return this;
    }

    public TournamentBuilder withPlayers(final TournamentPlayers players) {
        tournament.setPlayers(players);
        return this;
    }

    public TournamentBuilder withMinimumPrizePool(final BigDecimal minimumPrizePool) {
        variationTemplateBuilder.setPrizePool(minimumPrizePool);
        return this;
    }

    public TournamentBuilder withMinimumPayouts(final Set<TournamentVariationPayout> minimumPayouts) {
        variationTemplateBuilder.setTournamentPayouts(Lists.newArrayList(minimumPayouts));
        return this;
    }

    public TournamentBuilder withPot(final BigDecimal chipsInPot) {
        tournament.setPot(chipsInPot);
        return this;
    }

    public TournamentBuilder withTournamentStatus(final TournamentStatus status) {
        tournament.setTournamentStatus(status);
        return this;
    }

    public TournamentBuilder withDescription(final String description) {
        tournament.setDescription(description);
        return this;
    }

    public TournamentBuilder withStartTimeStamp(final DateTime startTime) {
        tournament.setStartTimeStamp(startTime);
        return this;
    }

    public TournamentBuilder withTables(final BigDecimal... tableIds) {
        tournament.setTables(Arrays.asList(tableIds));
        return this;
    }

    public TournamentBuilder withCurrentRoundIndex(final int index) {
        tournament.setCurrentRoundIndex(index);
        return this;
    }

    public TournamentBuilder withSettledPrizePot(final BigDecimal settledPrizePot) {
        tournament.setSettledPrizePot(settledPrizePot);
        return this;
    }

    public TournamentVariationTemplateBuilder variationTemplateBuilder() {
        return variationTemplateBuilder;
    }

    /**
     * Returns a new tournament.
     * Multiple calls to this method yield <i>copies</i> with the same values.
     *
     * @return Tournament
     */
    public Tournament build() {
        tournament.setTournamentVariationTemplate(variationTemplateBuilder.toTemplate());
        final Tournament copy = new Tournament();
        copy.setTournamentId(tournament.getTournamentId());
        copy.setStartTimeStamp(tournament.getStartTimeStamp());
        copy.setSignupStartTimeStamp(tournament.getSignupStartTimeStamp());
        copy.setSignupEndTimeStamp(tournament.getSignupEndTimeStamp());
        copy.setTournamentStatus(tournament.getTournamentStatus());
        copy.setTournamentVariationTemplate(tournament.getTournamentVariationTemplate());
        copy.setName(tournament.getName());
        copy.setDescription(tournament.getDescription());
        copy.setPot(tournament.getPot());
        copy.setNextEvent(tournament.getNextEvent());
        copy.setTables(tournament.getTables());
        copy.setPartnerId(tournament.getPartnerId());
        copy.setSettledPrizePot(tournament.getSettledPrizePot());
        copy.setPlayers(tournament.getPlayers());
        copy.setCurrentRoundIndex(tournament.getCurrentRoundIndex());
        return copy;
    }

    /**
     * Resets this builder, i.e. clears all existing tournament values.
     */
    public void reset() {
        tournament = new Tournament();
        variationTemplateBuilder = defaultVariationTemplateBuilder();
    }

    private static TournamentVariationTemplateBuilder defaultVariationTemplateBuilder() {
        final TournamentVariationTemplateBuilder builder = new TournamentVariationTemplateBuilder();
        builder.setTemplateName("Unknown Template");
        builder.setTournamentType(TournamentType.PRESET);
        builder.setTournamentVariationTemplateId(BigDecimal.ONE);
        return builder;
    }
}
