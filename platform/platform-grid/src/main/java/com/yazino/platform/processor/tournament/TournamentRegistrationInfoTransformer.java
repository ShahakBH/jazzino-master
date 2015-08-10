package com.yazino.platform.processor.tournament;

import com.google.common.base.Function;
import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.model.tournament.TournamentPlayer;
import com.yazino.platform.model.tournament.TournamentPlayerStatus;
import com.yazino.platform.model.tournament.TournamentPlayers;
import com.yazino.platform.tournament.TournamentRegistrationInfo;
import com.yazino.platform.tournament.TournamentRegistrationInfoBuilder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TournamentRegistrationInfoTransformer implements Function<Tournament, TournamentRegistrationInfo> {

    @Override
    public TournamentRegistrationInfo apply(final Tournament tournament) {
        return new TournamentRegistrationInfoBuilder()
                .withTournamentId(tournament.getTournamentId())
                .withStartTimeStamp(tournament.getStartTimeStamp())
                .withEntryFee(entryFeeFor(tournament))
                .withCurrentPrizePool(tournament.calculateUnpaidPrizePool())
                .withName(tournament.getName())
                .withDescription(tournament.getDescription())
                .withVariationTemplateName(tournament.getTournamentVariationTemplate().getTemplateName())
                .withPlayers(activePlayers(tournament.getPlayers()))
                .build();
    }

    private BigDecimal entryFeeFor(final Tournament tournament) {
        BigDecimal entryFee = BigDecimal.ZERO;
        if (tournament.getTournamentVariationTemplate() != null) {
            if (tournament.getTournamentVariationTemplate().getEntryFee() != null) {
                entryFee = entryFee.add(tournament.getTournamentVariationTemplate().getEntryFee());
            }
            if (tournament.getTournamentVariationTemplate().getServiceFee() != null) {
                entryFee = entryFee.add(tournament.getTournamentVariationTemplate().getServiceFee());
            }
        }
        return entryFee;
    }

    private static Set<BigDecimal> activePlayers(final TournamentPlayers players) {
        if (players == null) {
            return Collections.emptySet();
        }
        final Set<TournamentPlayer> activePlayers = players.getByStatus(TournamentPlayerStatus.ACTIVE);
        final Set<BigDecimal> activePlayerIds = new HashSet<>(activePlayers.size());
        for (TournamentPlayer player : activePlayers) {
            activePlayerIds.add(player.getPlayerId());
        }
        return activePlayerIds;
    }
}
