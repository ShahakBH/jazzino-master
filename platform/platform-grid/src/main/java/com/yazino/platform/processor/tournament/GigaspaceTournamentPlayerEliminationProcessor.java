package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.tournament.TournamentPlayerEliminationRequest;
import com.yazino.platform.model.tournament.TrophyLeaderboard;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.tournament.TrophyLeaderboardRepository;
import org.joda.time.DateTime;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

@EventDriven
@Polling(gigaSpace = "gigaSpace", concurrentConsumers = 1, maxConcurrentConsumers = 5)
public class GigaspaceTournamentPlayerEliminationProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceTournamentPlayerEliminationProcessor.class);

    private static final TournamentPlayerEliminationRequest TEMPLATE = new TournamentPlayerEliminationRequest();

    private final PlayerRepository playerRepository;
    private final TrophyLeaderboardRepository trophyLeaderboardRepository;

    public GigaspaceTournamentPlayerEliminationProcessor() {
        // CGLib constructor
        playerRepository = null;
        trophyLeaderboardRepository = null;
    }

    @Autowired
    public GigaspaceTournamentPlayerEliminationProcessor(final PlayerRepository playerRepository,
                                                         final TrophyLeaderboardRepository trophyLeaderboardRepository) {
        notNull(playerRepository, "playerRepository may not be null");
        notNull(trophyLeaderboardRepository, "trophyLeaderboardRepository may not be null");

        this.playerRepository = playerRepository;
        this.trophyLeaderboardRepository = trophyLeaderboardRepository;
    }

    @EventTemplate
    public TournamentPlayerEliminationRequest eventTemplate() {
        return TEMPLATE;
    }

    @SpaceDataEvent
    public void process(final TournamentPlayerEliminationRequest request) {
        LOG.debug("Processing {}", request);

        if (request == null) {
            return;
        }

        try {
            checkInitialisation();

            final Player player = playerRepository.findById(request.getPlayerId());
            if (player == null) {
                LOG.error("Player {} does not exist; cannot process request for tournament {}", request.getPlayerId(), request.getTournamentId());
                return;
            }

            final DateTime now = new DateTime();
            final Set<TrophyLeaderboard> leaderboards = trophyLeaderboardRepository.findCurrentAndActiveWithGameType(now, request.getGameType());
            LOG.debug("Found {} leaderboards for {} with type {} for request {}", leaderboards.size(), now, request.getGameType(), request);

            for (TrophyLeaderboard leaderboard : leaderboards) {
                LOG.debug("Request update for player {} on leaderboard {} from tournament {}",
                        request.getPlayerId(), leaderboard.getId(), request.getTournamentId());
                trophyLeaderboardRepository.requestUpdate(leaderboard.getId(), request.getTournamentId(), request.getPlayerId(),
                        player.getName(), player.getPictureUrl(), request.getLeaderBoardPosition(), request.getNumberOfPlayers());
            }

        } catch (Exception e) {
            LOG.error("Failed to process request {}", request, e);
        }
    }

    private void checkInitialisation() {
        if (playerRepository == null
                || trophyLeaderboardRepository == null) {
            throw new IllegalStateException("This is an uninitialised processor and cannot be called directly");
        }
    }

}
