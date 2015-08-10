package com.yazino.web.domain;

import com.yazino.platform.tournament.*;
import com.yazino.web.data.TournamentDetailHelper;
import com.yazino.web.util.Cached;
import com.yazino.web.util.PlayerFriendsCache;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

@Service("tournamentLobbyCache")
public class TournamentLobbyCache {
    private static final Logger LOG = LoggerFactory.getLogger(TournamentLobbyCache.class);

    private final TournamentService tournamentService;
    private final TrophyLeaderboardService trophyLeaderboardService;
    private final SiteConfiguration siteConfiguration;
    private final PlayerFriendsCache playerFriendsCache;

    private final Cached<Schedule> cachedSchedules
            = new Cached<>(new Cached.Retriever<Schedule>() {
        @Override
        public Schedule retrieve(final String descriptor) {
            try {
                return tournamentService.getTournamentSchedule(descriptor);
            } catch (Exception e) {
                LOG.error(String.format("Couldn't find schedule for partner %s and descriptor %s",
                        siteConfiguration.getPartnerId(), descriptor), e);
                return null;
            }
        }
    });

    private final Cached<TrophyLeaderboardView> cachedTrophyLeaderBoard
            = new Cached<>(new Cached.Retriever<TrophyLeaderboardView>() {
        @Override
        public TrophyLeaderboardView retrieve(final String descriptor) {
            try {
                return trophyLeaderboardService.findByGameType(descriptor);
            } catch (Exception e) {
                LOG.error("Couldn't find trophy leaderboard for game type {}", descriptor);
                return null;
            }
        }
    });

    private final Cached<Summary> cachedMostRecentTournament
            = new Cached<>(new Cached.Retriever<Summary>() {
        @Override
        public Summary retrieve(final String descriptor) {
            try {
                return tournamentService.findLastTournamentSummary(descriptor);

            } catch (Exception e) {
                LOG.error("Couldn't find last summary for " + descriptor, e);
                return null;
            }
        }
    });

    @Autowired
    public TournamentLobbyCache(final SiteConfiguration siteConfiguration,
                                final PlayerFriendsCache playerFriendsCache,
                                final TournamentService tournamentService,
                                final TrophyLeaderboardService trophyLeaderboardService) {
        notNull(siteConfiguration, "siteConfiguration is null");
        notNull(tournamentService, "tournamentService is null");
        notNull(trophyLeaderboardService, "trophyLeaderboardService is null");

        this.siteConfiguration = siteConfiguration;
        this.tournamentService = tournamentService;
        this.playerFriendsCache = playerFriendsCache;
        this.trophyLeaderboardService = trophyLeaderboardService;
    }

    public List<TournamentDetail> getTournamentSchedule(final String gameType,
                                                        final BigDecimal playerId) {
        final List<TournamentDetail> playerSchedule = new ArrayList<>();
        final Schedule schedule = cachedSchedules.getItem(gameType);
        if (schedule != null) {
            final Set<BigDecimal> friends = friendsOf(playerId);
            LOG.debug("player: {} friends: {}", playerId, friends);

            final Set<TournamentRegistrationInfo> tournamentDetails = tournamentsFor(playerId, schedule);
            for (TournamentRegistrationInfo tournamentRegistrationInfo : tournamentDetails) {
                final TournamentDetail tournamentDetail = TournamentDetailHelper.buildTournamentDetail(
                        gameType, playerId, friends, tournamentRegistrationInfo);
                playerSchedule.add(tournamentDetail);
            }
        }
        return playerSchedule;
    }

    private Set<TournamentRegistrationInfo> tournamentsFor(final BigDecimal playerId,
                                                           final Schedule schedule) {
        if (playerId == null) {
            return schedule.getChronologicalTournaments();
        } else {
            return schedule.getChronologicalTournamentsForPlayer(playerId);
        }
    }

    private Set<BigDecimal> friendsOf(final BigDecimal playerId) {
        if (playerId == null) {
            return new HashSet<>();
        } else {
            return playerFriendsCache.getFriendIds(playerId);
        }
    }

    public TournamentDetail getNextTournament(final BigDecimal playerId,
                                              final String gameType) {
        notNull(gameType, "gameType is null");

        LOG.debug("Finding tournament with the nearest start time for game type = {}, partner Id = {} and player id = {}",
                gameType, siteConfiguration.getPartnerId(), playerId);

        final List<TournamentDetail> details = getTournamentSchedule(gameType, playerId);
        if (details.isEmpty()) {
            return null;
        }
        return details.iterator().next();
    }

    public List<TrophyLeaderboardPlayer> getTrophyLeaderBoard(final String gameType,
                                                              final int maxToDisplay) {
        final TrophyLeaderboardView leaderBoard = cachedTrophyLeaderBoard.getItem(gameType);
        final List<TrophyLeaderboardPlayer> positions;
        if (leaderBoard == null) {
            positions = Collections.emptyList();
        } else {
            positions = leaderBoard.getOrderedByPosition();
        }
        return positions.subList(0, Math.min(maxToDisplay, positions.size()));
    }

    public List<TrophyLeaderboardPlayer> getTrophyLeaderBoardFriends(final String gameType,
                                                                     final Set<BigDecimal> included,
                                                                     final int maxToDisplay) {
        final TrophyLeaderboardView leaderBoard = cachedTrophyLeaderBoard.getItem(gameType);
        List<TrophyLeaderboardPlayer> positions;
        if (leaderBoard == null) {
            positions = Collections.emptyList();
        } else {
            positions = leaderBoard.getFilteredOrderedByPosition(included);
        }
        return positions.subList(0, Math.min(maxToDisplay, positions.size()));
    }

    public TrophyLeaderboardPlayer getTrophyLeaderBoardPlayer(final String gameType,
                                                              final BigDecimal playerId) {
        final TrophyLeaderboardView leaderBoard = cachedTrophyLeaderBoard.getItem(gameType);
        if (leaderBoard != null && leaderBoard.getPlayers() != null) {
            return leaderBoard.getPlayers().findPlayer(playerId);
        }
        return null;
    }

    public Summary getLastTournamentSummary(final String gameType) {
        return cachedMostRecentTournament.getItem(gameType);
    }

    public Map<String, TournamentDetail> getNextTournaments(final BigDecimal playerId,
                                                            final String... gameTypes) {
        final Map<String, TournamentDetail> details = new HashMap<>();

        for (String gameType : gameTypes) {
            details.put(gameType, getNextTournament(playerId, gameType));
        }
        return details;
    }

    public TrophyLeaderBoardSummary getTrophyLeaderBoardSummary(final BigDecimal playerId,
                                                                final String gameType,
                                                                final int maxToDisplay) {
        Validate.notBlank(gameType, "gameType is empty");
        Validate.isTrue(maxToDisplay >= 0, "maxToDisplay must be >= 0");

        final TrophyLeaderboardView leaderBoard = cachedTrophyLeaderBoard.getItem(gameType);
        return new TrophyLeaderBoardSummaryTransformer(playerId, maxToDisplay).apply(leaderBoard);
    }

    public TrophyLeaderBoardSummary getTrophyLeaderBoardSummaryForFriends(final BigDecimal playerId,
                                                                          final String gameType,
                                                                          final Set<BigDecimal> friendIds,
                                                                          final int maxToDisplay) {
        Validate.notBlank(gameType, "gameType is empty");
        Validate.isTrue(maxToDisplay >= 0, "maxToDisplay must be >= 0");
        Validate.notNull(friendIds, "friendIds is null");

        final TrophyLeaderboardView leaderBoard = cachedTrophyLeaderBoard.getItem(gameType);
        friendIds.add(playerId);
        return new TrophyLeaderBoardSummaryTransformer(playerId, maxToDisplay, friendIds).apply(leaderBoard);
    }

    public Map<String, UpcomingTournamentSummary> getNextTournamentsForEachVariation(final BigDecimal playerId,
                                                                                     final String gameType) {
        notNull(playerId, "playerId may not be null");
        notBlank(gameType, "gameType may not be null");

        final Map<String, UpcomingTournamentSummary> nextTournamentsByVariation = new HashMap<>();
        for (UpcomingTournamentSummary tournament : getTournamentScheduleSummary(gameType, playerId)) {
            final String variation = tournament.getVariationName();
            if (!nextTournamentsByVariation.containsKey(variation)
                    || tournament.getMillisToStart() < nextTournamentsByVariation.get(variation).getMillisToStart()) {
                nextTournamentsByVariation.put(variation, tournament);
            }
        }

        return nextTournamentsByVariation;
    }

    private List<UpcomingTournamentSummary> getTournamentScheduleSummary(final String gameType,
                                                                         final BigDecimal playerId) {
        final List<UpcomingTournamentSummary> playerSchedule = new ArrayList<>();

        final Schedule schedule = cachedSchedules.getItem(gameType);
        if (schedule != null) {
            final Set<BigDecimal> friends = friendsOf(playerId);
            LOG.debug("Player {} has friends {}", playerId, friends);

            final Set<TournamentRegistrationInfo> tournamentDetails = tournamentsFor(playerId, schedule);
            for (TournamentRegistrationInfo tournament : tournamentDetails) {
                playerSchedule.add(new UpcomingTournamentSummary(playerId, friends, tournament));
            }
        }

        return playerSchedule;
    }
}
