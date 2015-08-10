package com.yazino.platform.processor.tournament;

import com.google.common.base.Function;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.model.tournament.TournamentPlayer;
import com.yazino.platform.model.tournament.TournamentSummary;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.tournament.TournamentPlayerSummary;
import com.yazino.platform.tournament.TournamentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Creates a {@link TournamentSummary} from a {@link Tournament}.
 */
@Service
public class TournamentToSummaryTransformer implements Function<Tournament, TournamentSummary> {
    private static final Logger LOG = LoggerFactory.getLogger(TournamentToSummaryTransformer.class);

    private final PlayerRepository playerRepository;

    @Autowired
    public TournamentToSummaryTransformer(final PlayerRepository playerRepository) {
        notNull(playerRepository, "playerRepository may not be null");

        this.playerRepository = playerRepository;
    }

    @Override
    public TournamentSummary apply(final Tournament tournament) {
        if (tournament == null) {
            return null;
        }

        final TournamentStatus tournamentStatus = tournament.getTournamentStatus();

        if (tournamentStatus != TournamentStatus.SETTLED) {
            throw new IllegalStateException("Summary cannot be generated until tournament is settled");
        }

        final TournamentSummary summary = new TournamentSummary();
        summary.setTournamentId(tournament.getTournamentId());
        summary.setVariationId(tournament.getTournamentVariationTemplate().getTournamentVariationTemplateId());
        summary.setTournamentName(tournament.getName());
        summary.setGameType(tournament.getTournamentVariationTemplate().getGameType());
        summary.setFinishDateTime(new Date());
        summary.setStartDateTime(tournament.getStartTimeStamp().toDate());

        for (final TournamentPlayer player : tournament.getPlayers()) {
            if (player.getLeaderboardPosition() == null) {
                continue;
            }

            BigDecimal settledPrize = player.getSettledPrize();
            if (settledPrize == null) {
                settledPrize = BigDecimal.ZERO;
            }

            final String pictureUrl = pictureFor(player.getPlayerId());
            final TournamentPlayerSummary playerSummary = new TournamentPlayerSummary(
                    player.getPlayerId(), player.getLeaderboardPosition(), player.getName(), settledPrize, pictureUrl);
            summary.addPlayer(playerSummary);
        }

        LOG.debug("TournamentSummary [{}] generated from Tournament [{}]", summary, tournament);

        return summary;
    }

    private String pictureFor(final BigDecimal playerId) {
        final Player player = playerRepository.findById(playerId);
        if (player != null) {
            return player.getPictureUrl();
        }
        return null;
    }
}
