package com.yazino.platform.model.conversion;

import com.google.common.base.Function;
import com.google.common.collect.Sets;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.tournament.TournamentDetail;
import com.yazino.platform.tournament.TournamentStatus;
import com.yazino.platform.tournament.TournamentVariationPayout;
import org.joda.time.DateTimeZone;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.apache.commons.lang3.Validate.notNull;

public class TournamentDetailTransformer implements Function<Tournament, TournamentDetail> {

    private static final DateTimeZone GMT_ZONE = DateTimeZone.forID("GMT");

    private final PlayerRepository playerRepository;
    private final BigDecimal playerId;

    public TournamentDetailTransformer(final PlayerRepository playerRepository,
                                       final BigDecimal playerId) {
        notNull(playerRepository, "playerRepository may not be null");

        this.playerRepository = playerRepository;
        this.playerId = playerId;
    }

    @Override
    public TournamentDetail apply(final Tournament tournament) {
        if (tournament == null) {
            return null;
        }

        final boolean inProgress = tournament.getTournamentStatus() == TournamentStatus.RUNNING
                || tournament.getTournamentStatus() == TournamentStatus.ON_BREAK
                || tournament.getTournamentStatus() == TournamentStatus.WAITING_FOR_CLIENTS;

        int friendsRegistered = 0;
        if (playerId != null) {
            friendsRegistered = Sets.intersection(tournament.players(), friendIds()).size();
        }

        final String gameType = tournament.getTournamentVariationTemplate().getGameType();
        return new TournamentDetail(
                tournament.getTournamentId(),
                tournament.getName(),
                gameType,
                tournament.getTournamentVariationTemplate().getTemplateName(),
                tournament.getDescription(),
                tournament.playerCount(),
                friendsRegistered,
                inProgress,
                playerId != null && tournament.players().contains(playerId),
                firstPrizeFor(tournament),
                tournament.calculateUnpaidPrizePool(),
                entryFeeFor(tournament),
                tournament.getStartTimeStamp().getMillis() - System.currentTimeMillis(),
                tournament.getStartTimeStamp().withZone(GMT_ZONE).toDate());
    }

    private Set<BigDecimal> friendIds() {
        final Player player = playerRepository.findById(playerId);
        if (player != null) {
            return newHashSet(player.retrieveFriends().keySet());
        }
        return Collections.emptySet();
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

    private BigDecimal firstPrizeFor(final Tournament tournament) {
        BigDecimal firstPrize = BigDecimal.ZERO;
        if (tournament.getTournamentVariationTemplate() != null) {
            final List<TournamentVariationPayout> payouts
                    = tournament.getTournamentVariationTemplate().getTournamentPayouts();
            if (payouts != null && !payouts.isEmpty()) {
                firstPrize = payouts.get(0).getPayout();
            }
        }
        return firstPrize;
    }
}
