package com.yazino.platform.service.community;

import com.yazino.platform.community.PlayerTrophyService;
import com.yazino.platform.community.Trophy;
import com.yazino.platform.community.TrophyCabinet;
import com.yazino.platform.community.TrophySummary;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.community.PlayerTrophy;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.community.PlayerTrophyRepository;
import com.yazino.platform.repository.community.TrophyRepository;
import com.yazino.platform.tournament.TrophyWinner;
import org.openspaces.remoting.RemotingService;
import org.openspaces.remoting.Routing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.yazino.game.api.ParameterisedMessage;

import java.math.BigDecimal;
import java.util.*;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Provides a Gigaspace implementation of {@link com.yazino.platform.community.PlayerTrophyService}
 */
@RemotingService
public class GigaspaceRemotingPlayerTrophyService implements PlayerTrophyService {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceRemotingPlayerTrophyService.class);

    private final PlayerTrophyRepository playerTrophyRepository;
    private final PlayerRepository playerRepository;
    private final TrophyRepository trophyRepository;

    @Autowired
    public GigaspaceRemotingPlayerTrophyService(final PlayerTrophyRepository playerTrophyRepository,
                                                final PlayerRepository playerRepository,
                                                final TrophyRepository trophyRepository) {
        notNull(playerTrophyRepository, "playerTrophyRepository must not be null");
        notNull(playerRepository, "playerRepository must not be null");
        notNull(trophyRepository, "trophyRepository must not be null");

        this.playerTrophyRepository = playerTrophyRepository;
        this.playerRepository = playerRepository;
        this.trophyRepository = trophyRepository;
    }

    @Override
    public TrophyCabinet findTrophyCabinetForPlayer(final String gameType,
                                                    @Routing final BigDecimal playerId,
                                                    final Collection<String> trophyNames) {
        notNull(gameType, "gameType must not null");
        notNull(playerId, "playerId must not null");
        notNull(trophyNames, "trophyNames must not null");
        final Player player = playerRepository.findById(playerId);
        notNull(player, "player with playerId " + playerId + " does not exist");

        final TrophyCabinet trophyCabinet = new TrophyCabinet();
        trophyCabinet.setGameType(gameType);
        trophyCabinet.setPlayerName(player.getName());

        final Map<String, Integer> trophyCounts = new HashMap<String, Integer>();
        final Map<String, Trophy> trophies = new HashMap<String, Trophy>();
        final Collection<PlayerTrophy> playerTrophies = playerTrophyRepository.findPlayersTrophies(playerId);

        for (PlayerTrophy playerTrophy : playerTrophies) {
            final BigDecimal trophyId = playerTrophy.getTrophyId();
            final Trophy trophy = trophyRepository.findById(trophyId);
            if (trophy == null) {
                LOG.warn("Invalid trophy reference founf for player {} to trophy {}", playerId, trophyId);
                continue;
            }

            trophies.put(trophy.getName(), trophy);
            final String trophyName = trophy.getName();
            if (trophyNames.contains(trophyName) && gameType.equals(trophy.getGameType())) {
                if (!trophyCounts.containsKey(trophyName)) {
                    trophyCounts.put(trophyName, 0);
                }
                trophyCounts.put(trophyName, trophyCounts.get(trophyName) + 1);
            }
        }

        for (String trophyName : trophyCounts.keySet()) {
            final Trophy trophy = trophies.get(trophyName);
            final ParameterisedMessage message;
            final Integer trophyCount = trophyCounts.get(trophyName);
            if (trophyCount == 1) {
                message = new ParameterisedMessage(trophy.getMessage(), player.getName());
            } else {
                message = new ParameterisedMessage(trophy.getMessageCabinet(), player.getName(), trophyCount);
            }
            trophyCabinet.addTrophySummary(new TrophySummary(trophyName, trophy.getImage(), message, trophyCount));
        }

        return trophyCabinet;
    }

    @Override
    public Map<String, List<TrophyWinner>> findWinnersByTrophyName(final String trophyName,
                                                                   final int maxResultsForTrophy,
                                                                   final String... gameTypes) {
        final Map<String, List<TrophyWinner>> winners = new HashMap<String, List<TrophyWinner>>();

        for (String gameType : gameTypes) {
            final Trophy trophy = trophyRepository.findByNameAndGameType(trophyName, gameType);
            notNull(trophy, "Trophy " + trophyName + " not found");

            final List<PlayerTrophy> playerTrophies = playerTrophyRepository.findWinnersByTrophyId(
                    trophy.getId(), maxResultsForTrophy);

            final List<TrophyWinner> tournamentPlayers = new LinkedList<TrophyWinner>();
            for (PlayerTrophy playerTrophy : playerTrophies) {
                final Player player = playerRepository.findById(playerTrophy.getPlayerId());
                if (player != null) {
                    tournamentPlayers.add(new TrophyWinner(player.getPlayerId(),
                            player.getName(), player.getPictureUrl(), playerTrophy.getAwardTime()));
                } else {
                    LOG.error("Failed to find player {} for PlayerTrophy {}", playerTrophy.getPlayerId(), playerTrophy);
                }
            }
            winners.put(gameType, tournamentPlayers);
        }

        return winners;
    }

}
